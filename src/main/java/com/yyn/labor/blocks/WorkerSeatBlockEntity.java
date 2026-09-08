package com.yyn.labor.blocks;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import org.slf4j.Logger;

import com.mojang.logging.LogUtils;
import com.simibubi.create.content.contraptions.actors.seat.SeatEntity;
import com.simibubi.create.content.kinetics.belt.BeltBlockEntity;
import com.simibubi.create.content.kinetics.belt.BeltHelper;
import com.simibubi.create.content.kinetics.belt.transport.BeltInventory;
import com.simibubi.create.content.kinetics.belt.transport.TransportedItemStack;
import com.simibubi.create.content.logistics.depot.DepotBlock;
import com.simibubi.create.content.logistics.depot.DepotBlockEntity;
import com.simibubi.create.content.processing.basin.BasinBlockEntity;
import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import com.simibubi.create.foundation.blockEntity.behaviour.filtering.FilteringBehaviour;
import com.simibubi.create.foundation.item.SmartInventory;
import com.yyn.labor.Config;
import com.yyn.labor.entity.LaborEntity;
import com.yyn.labor.util.MaidChatBubbleUtil;
import com.yyn.labor.util.WorkerUtil;

import net.createmod.catnip.data.Iterate;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemHandlerHelper;

public abstract class WorkerSeatBlockEntity extends SmartBlockEntity {

    private static final Logger LOGGER = LogUtils.getLogger();

    private static final AtomicInteger OUTPUT_COUNTER = new AtomicInteger(0);

    protected int maxCooldown;
    protected boolean hasWorker;
    protected int workerCheckCooldown;

    protected int processingTimer;
    protected ItemStack processingStack;
    protected int outputCooldown;
    protected int outputCooldownDuration;

    protected FilteringBehaviour filtering;

    private boolean wasWorking;
    private ItemStack lastHandItem;
    protected Direction beltDirection;
    private int rotationTick;

    // 保存工人原始主手物品，避免工位加工期间覆盖工人（酒狐、女仆等）本来自带的持物
    private LivingEntity cachedWorker;
    private ItemStack savedOriginalHandItem = ItemStack.EMPTY;
    private boolean hasSavedOriginalHand = false;

    // TLM 聊天气泡计时器
    private int chatBubbleTimer = 0;

    // 手套升级：允许处理加热配方
    private boolean hasGlovesUpgrade = false;

    protected final SeatMaterial material;

    public WorkerSeatBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state, SeatMaterial material) {
        super(type, pos, state);
        this.material = material;
        maxCooldown = 5;
        workerCheckCooldown = 0;
        hasWorker = false;
        processingTimer = 0;
        processingStack = ItemStack.EMPTY;
        outputCooldown = 0;
        outputCooldownDuration = material.scaleCooldown(20);
        lastHandItem = ItemStack.EMPTY;
        beltDirection = null;
        rotationTick = 0;
    }

    public SeatMaterial getMaterial() {
        return material;
    }

    // ==================== 手套升级 ====================

    public boolean hasGlovesUpgrade() {
        return hasGlovesUpgrade;
    }

    public void setGlovesUpgrade(boolean value) {
        this.hasGlovesUpgrade = value;
        setChanged();
        notifyUpdate();
    }

    @Override
    public void addBehaviours(List<BlockEntityBehaviour> behaviours) {
        filtering = new FilteringBehaviour(this, new SeatFilterSlot()).forRecipes();
        behaviours.add(filtering);
    }

    @Override
    public void tick() {
        super.tick();
        if (level == null || level.isClientSide)
            return;

        checkWorker();

        if (hasWorker) {
            processWork();
            // 女仆在工位上（无论是否工作）即周期性显示 TLM 聊天气泡
            tryShowChatBubble();
            // 每 tick 更新工人朝向（朝向传送带），不受 cooldown/processing 影响
            updateWorkerRotation();
        }
    }

    protected void processWork() {
        if (outputCooldown > 0) {
            outputCooldown--;
            return;
        }

        if (!processingStack.isEmpty() && processingTimer > 0) {
            processingTimer--;
            if (processingTimer == 0)
                finishProcessing();
            return;
        }

        // After cooldown ends, clear the old processingStack (kept for rendering)
        // and try to acquire the next item in the same tick
        if (!processingStack.isEmpty() && processingTimer == 0) {
            processingStack = ItemStack.EMPTY;
            updateHand();
            notifyUpdate();
        }

        if (processingStack.isEmpty())
            acquireNextItem();
        else
            updateHand();

        updateWorkingState();
    }

    /**
     * 每 tick 更新工人朝向，指向传送带方向。
     * 独立于 processWork()，不受 cooldown 和 processing 状态影响。
     */
    private void updateWorkerRotation() {
        if (beltDirection == null) {
            detectBeltDirection();
        }
        if (beltDirection == null)
            return;

        rotationTick++;
        if (rotationTick >= 5) {
            rotationTick = 0;
            rotateWorkerTowardBelt();
        }
    }

    private void detectBeltDirection() {
        for (Direction dir : Iterate.horizontalDirections) {
            BlockPos adjacentPos = worldPosition.relative(dir);
            BlockState adjacentState = level.getBlockState(adjacentPos);
            if (adjacentState.getBlock() instanceof com.simibubi.create.content.kinetics.belt.BeltBlock) {
                beltDirection = dir;
                return;
            }
        }
    }

    /**
     * 周期性为工作中的女仆显示 TLM 聊天气泡（测试文本 1-5 随机轮换）。
     * 仅在服务端调用，气泡通过 TLM 内部网络同步到客户端。
     */
    private void tryShowChatBubble() {
        if (!Config.ENABLE_TLM_CHAT_BUBBLE.get()) return;
        chatBubbleTimer++;
        if (chatBubbleTimer < Config.TLM_CHAT_BUBBLE_INTERVAL.get()) return;
        chatBubbleTimer = 0;

        LivingEntity worker = findWorker();
        if (worker == null) return;
        MaidChatBubbleUtil.showWorkingBubble(worker);
    }

    private void updateWorkingState() {
        boolean isWorking = !processingStack.isEmpty() && processingTimer > 0;
        if (isWorking != wasWorking) {
            wasWorking = isWorking;
            BlockState state = level.getBlockState(worldPosition);
            if (state.hasProperty(WorkerSeatBlock.WORKING)) {
                level.setBlock(worldPosition, state.setValue(WorkerSeatBlock.WORKING, isWorking), 2);
            }
        }
    }

    private static boolean isMaidEntity(Entity entity) {
        return WorkerUtil.isMaidEntity(entity);
    }

    private static boolean isMillenaireVillager(Entity entity) {
        return WorkerUtil.isMillenaireVillager(entity);
    }

    private boolean isTaggedWorker(Entity entity) {
        return WorkerUtil.isTaggedWorker(entity);
    }

    protected void checkWorker() {
        if (workerCheckCooldown > 0) {
            workerCheckCooldown--;
            return;
        }
        workerCheckCooldown = 5;

        // 判定"工位是否真有人工作" → 仍沿用原来的严格口径 isWorkerPresent()，
        // 与修复前逻辑 100% 一致，确保"原本能工作 → 现在也工作"。
        boolean newHasWorker = isWorkerPresent();

        // 找一个用于写入主手的工人引用。findWorker() 已与 isWorkerPresent 使用同口径，
        // 保证只返回真正能工作的 LivingEntity（不会把僵尸/羊/错误实体写入主手）。
        LivingEntity currentWorker = newHasWorker ? findWorker() : null;

        // 工人变化时：上一位工人还原主手物品，新工人（若有）保存原始主手物品
        if (cachedWorker != currentWorker) {
            restoreOriginalHandIfNeeded();
            cachedWorker = currentWorker;
            saveOriginalHandIfNeeded();
            lastHandItem = ItemStack.EMPTY;
        }

        if (newHasWorker != hasWorker) {
            hasWorker = newHasWorker;
            LOGGER.info("Worker presence changed: {} -> {} at {}", !hasWorker, hasWorker, worldPosition);
            notifyUpdate();
        }
    }

    protected boolean isWorkerPresent() {
        AABB searchBox = new AABB(worldPosition).inflate(0.5);

        for (SeatEntity seatEntity : level.getEntitiesOfClass(SeatEntity.class, searchBox)) {
            if (seatEntity.isVehicle()) {
                for (Entity passenger : seatEntity.getPassengers()) {
                    if (passenger.isAlive() && (passenger instanceof Villager || passenger instanceof Player
                        || isMaidEntity(passenger) || isMillenaireVillager(passenger)
                        || isTaggedWorker(passenger)))
                        return true;
                }
            }
        }

        for (Entity entity : level.getEntitiesOfClass(Player.class, searchBox)) {
            if (entity.isAlive() && entity.isPassenger()) {
                Entity vehicle = entity.getVehicle();
                if (vehicle instanceof SeatEntity && vehicle.blockPosition().equals(worldPosition))
                    return true;
            }
        }

        for (LaborEntity labor : level.getEntitiesOfClass(LaborEntity.class, searchBox)) {
            if (labor.isAlive())
                return true;
        }

        return false;
    }

    // =============== 工人主手物品保护：保存/还原原始持物 ===============

    private void saveOriginalHandIfNeeded() {
        hasSavedOriginalHand = false;
        savedOriginalHandItem = ItemStack.EMPTY;
        if (cachedWorker == null) return;
        if (cachedWorker instanceof Player) return;
        if (cachedWorker instanceof LaborEntity) return;
        ItemStack current = cachedWorker.getItemInHand(InteractionHand.MAIN_HAND);
        if (!current.isEmpty()) {
            savedOriginalHandItem = current.copy();
            hasSavedOriginalHand = true;
        }
    }

    private void restoreOriginalHandIfNeeded() {
        if (cachedWorker == null) return;
        try {
            if (hasSavedOriginalHand && !savedOriginalHandItem.isEmpty()) {
                cachedWorker.setItemInHand(InteractionHand.MAIN_HAND, savedOriginalHandItem.copy());
            }
        } catch (Exception ignored) {
        } finally {
            hasSavedOriginalHand = false;
            savedOriginalHandItem = ItemStack.EMPTY;
        }
    }

    public void restoreWorkerOnRemoval() {
        restoreOriginalHandIfNeeded();
        cachedWorker = null;
    }

    // ================================================================

    protected void acquireNextItem() {
        for (Direction dir : Iterate.horizontalDirections) {
            BlockPos adjacentPos = worldPosition.relative(dir);
            BlockState adjacentState = level.getBlockState(adjacentPos);
            Block block = adjacentState.getBlock();

            if (block instanceof com.simibubi.create.content.kinetics.belt.BeltBlock) {
                if (tryTakeFromBelt(adjacentPos)) {
                    beltDirection = dir;
                    return;
                }
            } else if (block instanceof DepotBlock) {
                if (tryTakeFromDepot(adjacentPos))
                    return;
            } else if (isBasinBlock(adjacentState)) {
                if (tryTakeFromBasin(adjacentPos))
                    return;
            }
        }
    }

    protected boolean itemCanBeProcessed(ItemStack stack) {
        List<ItemStack> outputs = processItem(stack);
        if (outputs.isEmpty())
            return false;
        if (filtering.isActive())
            return outputs.stream().anyMatch(filtering::test);
        return true;
    }

    /**
     * 查找当前工位上的工人实体。
     * 搜索范围与 isWorkerPresent() 保持一致（inflate(0.5)），且判定口径完全相同，
     * 只会返回"真正会被认为在工位上工作的 LivingEntity"，避免把僵尸/羊等
     * 错误乘客当成工人去写入主手物品。
     */
    public LivingEntity findWorker() {
        AABB searchBox = new AABB(worldPosition).inflate(0.5);

        // 1) SeatEntity 上的合法乘客（Villager/Player/Maid/Millenaire/Tagged）
        for (SeatEntity seatEntity : level.getEntitiesOfClass(SeatEntity.class, searchBox)) {
            if (!seatEntity.isVehicle())
                continue;
            for (Entity passenger : seatEntity.getPassengers()) {
                if (!passenger.isAlive())
                    continue;
                if (passenger instanceof LivingEntity living &&
                    (passenger instanceof Villager || passenger instanceof Player
                        || isMaidEntity(passenger) || isMillenaireVillager(passenger)
                        || isTaggedWorker(passenger))) {
                    return living;
                }
            }
        }

        // 2) Player（兜底：部分玩家骑乘路径）
        for (Entity entity : level.getEntitiesOfClass(Player.class, searchBox)) {
            if (entity.isAlive() && entity.isPassenger()) {
                Entity vehicle = entity.getVehicle();
                if (vehicle instanceof SeatEntity && vehicle.blockPosition().equals(worldPosition))
                    return (Player) entity;
            }
        }

        // 3) LaborEntity（性能升级生成的无AI村民工人）
        for (LaborEntity labor : level.getEntitiesOfClass(LaborEntity.class, searchBox)) {
            if (labor.isAlive())
                return labor;
        }
        return null;
    }

    private void rotateWorkerTowardBelt() {
        if (beltDirection == null)
            return;
        LivingEntity worker = findWorker();
        if (worker == null)
            return;

        float targetYaw = switch (beltDirection) {
            case NORTH -> 180;
            case SOUTH -> 0;
            case WEST -> 90;
            case EAST -> -90;
            default -> 0;
        };
        worker.setYRot(targetYaw);
        worker.yBodyRot = targetYaw;
        worker.setYHeadRot(targetYaw);
    }

    protected boolean tryTakeFromBelt(BlockPos beltPos) {
        BeltBlockEntity segmentBE = BeltHelper.getSegmentBE(level, beltPos);
        if (segmentBE == null)
            return false;
        int targetSegment = segmentBE.index;

        BeltBlockEntity controller = BeltHelper.getControllerBE(level, beltPos);
        if (controller == null)
            return false;

        BeltInventory inventory = controller.getInventory();
        if (inventory == null)
            return false;

        TransportedItemStack found = null;
        for (TransportedItemStack item : inventory.getTransportedItems()) {
            float center = targetSegment + 0.5f;
            if (Math.abs(item.beltPosition - center) > 0.45f)
                continue;
            if (!itemCanBeProcessed(item.stack))
                continue;
            found = item;
            break;
        }

        if (found == null)
            return false;

        if (!inventory.getTransportedItems().remove(found)) {
            LOGGER.warn("Failed to remove item from belt at {}, concurrent modification?", worldPosition);
            return false;
        }

        int batchSize = material.getBatchSize();
        int taken = Math.min(batchSize, found.stack.getCount());
        int remaining = found.stack.getCount() - taken;
        if (remaining > 0) {
            ItemStack restStack = found.stack.copy();
            restStack.setCount(remaining);
            TransportedItemStack rest = new TransportedItemStack(restStack);
            rest.beltPosition = found.beltPosition;
            rest.insertedFrom = found.insertedFrom;
            rest.insertedAt = found.insertedAt;
            inventory.addItem(rest);
        }
        controller.notifyUpdate();

        ItemStack processingCopy = found.stack.copy();
        processingCopy.setCount(taken);
        processingStack = processingCopy;
        processingTimer = maxCooldown;
        updateHand();
        notifyUpdate();
        return true;
    }

    protected boolean tryTakeFromDepot(BlockPos depotPos) {
        if (!(level.getBlockEntity(depotPos) instanceof DepotBlockEntity depotBE))
            return false;

        ItemStack heldItem = depotBE.getHeldItem();
        if (heldItem.isEmpty())
            return false;

        if (!itemCanBeProcessed(heldItem))
            return false;

        int batchSize = material.getBatchSize();
        int taken = Math.min(batchSize, heldItem.getCount());
        ItemStack processingCopy = heldItem.copy();
        processingCopy.setCount(taken);
        processingStack = processingCopy;
        int remaining = heldItem.getCount() - taken;
        if (remaining > 0) {
            ItemStack restStack = heldItem.copy();
            restStack.setCount(remaining);
            depotBE.setHeldItem(restStack);
        } else {
            depotBE.setHeldItem(ItemStack.EMPTY);
        }
        processingTimer = maxCooldown;
        updateHand();
        notifyUpdate();
        return true;
    }

    protected boolean isBasinBlock(BlockState state) {
        ResourceLocation key = state.getBlock().builtInRegistryHolder().key().location();
        return "create".equals(key.getNamespace()) && "basin".equals(key.getPath());
    }

    protected boolean tryTakeFromBasin(BlockPos basinPos) {
        BlockEntity be = level.getBlockEntity(basinPos);
        if (be == null)
            return false;

        LazyOptional<IItemHandler> cap = be.getCapability(ForgeCapabilities.ITEM_HANDLER, Direction.UP);
        IItemHandler handler = cap.orElse(null);
        if (handler == null)
            return false;

        int batchSize = material.getBatchSize();

        for (int slot = 0; slot < handler.getSlots(); slot++) {
            ItemStack stack = handler.getStackInSlot(slot);
            if (stack.isEmpty())
                continue;
            if (!itemCanBeProcessed(stack))
                continue;

            int toExtract = Math.min(batchSize, stack.getCount());
            ItemStack extracted = handler.extractItem(slot, toExtract, false);
            if (extracted.isEmpty())
                continue;

            processingStack = extracted;
            processingTimer = maxCooldown;
            updateHand();
            notifyUpdate();
            return true;
        }
        return false;
    }

    protected void finishProcessing() {
        if (processingStack.isEmpty())
            return;

        List<ItemStack> outputs = processItem(processingStack);

        if (outputs.isEmpty()) {
            LOGGER.warn("Processing failed for {} at {}, returning item to adjacent depot (or dropping) to prevent loss",
                processingStack.getItem(), worldPosition);
            // 仅置物台(无传送带/盆)场景：原物放回相邻 Depot，不直接丢地上
            ItemStack toReturn = processingStack.copy();
            if (!outputToAdjacentDepotOnly(toReturn)) {
                Block.popResource(level, worldPosition.above(), toReturn);
            }
            processingStack = ItemStack.EMPTY;
            updateHand();
            outputCooldown = outputCooldownDuration;
            notifyUpdate();
            return;
        }

        updateHand();

        for (ItemStack output : outputs) {
            // 先 Belt→Basin→Depot 依次尝试；仅置物台时 Depot 接住物品，不会掉出世界
            if (!outputToAdjacent(output)) {
                if (!outputToAdjacentDepotOnly(output)) {
                    Block.popResource(level, worldPosition.above(), output);
                }
            }
        }

        outputCooldown = outputCooldownDuration;
        notifyUpdate();
        onProcessingComplete();
    }

    protected void onProcessingComplete() {
    }

    /**
     * 更新工人主手显示。
     * 1) 玩家：绝对不修改主手；
     * 2) 非玩家工人：有加工物品时才设为加工物品；加工为空则还原其入职时保存的原始主手；
     * 3) 不再使用 {@code worker != cachedWorker} 作为 early return：若检测到工人切换，
     *    直接在此处顺带进行旧工人物品还原和新工人物品保存，避免因 checkWorker 的
     *    冷却周期导致"工位无法继续工作"或主手写入错误实体。
     */
    protected void updateHand() {
        LivingEntity worker = findWorker();

        if (worker != cachedWorker) {
            restoreOriginalHandIfNeeded();
            cachedWorker = worker;
            saveOriginalHandIfNeeded();
            lastHandItem = ItemStack.EMPTY;
        }

        if (worker instanceof Player) return;

        ItemStack toHold = getHandItem();

        if (!processingStack.isEmpty()) {
            if (ItemStack.matches(toHold, lastHandItem)) return;
            lastHandItem = toHold.copy();
            if (worker != null)
                worker.setItemInHand(InteractionHand.MAIN_HAND, toHold.copy());
        } else {
            lastHandItem = ItemStack.EMPTY;
            if (worker == null) return;
            if (worker instanceof LaborEntity) {
                worker.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
            } else if (hasSavedOriginalHand) {
                worker.setItemInHand(InteractionHand.MAIN_HAND, savedOriginalHandItem.copy());
            }
            // 未保存原始持物则不覆盖，以免丢失工人后来自己获得的物品
        }
    }

    protected ItemStack getHandItem() {
        return processingStack.isEmpty() ? ItemStack.EMPTY : processingStack;
    }

    protected boolean outputToAdjacent(ItemStack stack) {
        for (Direction dir : Iterate.horizontalDirections) {
            BlockPos adjacentPos = worldPosition.relative(dir);
            BlockState adjacentState = level.getBlockState(adjacentPos);
            Block block = adjacentState.getBlock();

            if (block instanceof com.simibubi.create.content.kinetics.belt.BeltBlock) {
                if (outputToBeltAtStart(stack, adjacentPos))
                    return true;
            } else if (isBasinBlock(adjacentState)) {
                if (outputToBasin(stack, adjacentPos))
                    return true;
            }
        }
        return false;
    }

    /**
     * 当且仅当相邻只有置物台（没有传送带/盆）时使用：原物或产出物放回置物台。
     * 与 outputToAdjacent 的差别是：不扫 Belt/Basin，只扫 Depot，避免误塞到其他位置。
     */
    protected boolean outputToAdjacentDepotOnly(ItemStack stack) {
        for (Direction dir : Iterate.horizontalDirections) {
            BlockPos adjacentPos = worldPosition.relative(dir);
            BlockState adjacentState = level.getBlockState(adjacentPos);
            if (adjacentState.getBlock() instanceof DepotBlock) {
                if (outputToDepot(stack, adjacentPos))
                    return true;
            }
        }
        return false;
    }

    /**
     * 把物品推到 Depot 置物台上：
     * - Depot 当前为空 → 直接 setHeldItem
     * - Depot 已有同类物品且两者能合并（未到单堆上限）→ 合并
     * 合并不完的剩余部分会直接掉出（置物台一次只能放一堆）
     */
    protected boolean outputToDepot(ItemStack stack, BlockPos depotPos) {
        BlockEntity be = level.getBlockEntity(depotPos);
        if (!(be instanceof DepotBlockEntity depotBE))
            return false;
        ItemStack current = depotBE.getHeldItem();
        if (current.isEmpty()) {
            depotBE.setHeldItem(stack.copy());
            return true;
        }
        if (ItemStack.isSameItemSameTags(current, stack)) {
            int space = current.getMaxStackSize() - current.getCount();
            if (space <= 0) return false;
            int toMove = Math.min(space, stack.getCount());
            ItemStack merged = current.copy();
            merged.grow(toMove);
            depotBE.setHeldItem(merged);
            if (toMove < stack.getCount()) {
                ItemStack leftover = stack.copy();
                leftover.shrink(toMove);
                Block.popResource(level, depotPos.above(), leftover);
            }
            return true;
        }
        return false;
    }

    private boolean outputToBeltAtStart(ItemStack stack, BlockPos beltPos) {
        BeltBlockEntity controller = BeltHelper.getControllerBE(level, beltPos);
        if (controller == null) return false;

        BeltInventory inventory = controller.getInventory();
        if (inventory == null) return false;

        BeltBlockEntity segment = BeltHelper.getSegmentBE(level, beltPos);
        if (segment == null) return false;

        TransportedItemStack newStack = new TransportedItemStack(stack.copy());
        newStack.beltPosition = segment.index + 0.01f + (OUTPUT_COUNTER.incrementAndGet() % 100) * 0.0001f;

        Direction insertDir = Direction.getNearest(
            worldPosition.getX() - beltPos.getX(), 0,
            worldPosition.getZ() - beltPos.getZ()
        ).getOpposite();
        newStack.insertedFrom = insertDir;
        newStack.insertedAt = segment.index;

        inventory.addItem(newStack);
        controller.notifyUpdate();
        return true;
    }

    protected boolean outputToBasin(ItemStack stack, BlockPos basinPos) {
        BlockEntity be = level.getBlockEntity(basinPos);
        if (!(be instanceof BasinBlockEntity basinBE))
            return false;
        SmartInventory outputInv = basinBE.getOutputInventory();
        outputInv.allowInsertion();
        ItemStack remainder = ItemHandlerHelper.insertItemStacked(outputInv, stack.copy(), false);
        outputInv.forbidInsertion();
        basinBE.notifyChangeOfContents();
        return remainder.isEmpty();
    }

    protected abstract List<ItemStack> processItem(ItemStack input);

    @Override
    public void write(CompoundTag compound, boolean clientPacket) {
        super.write(compound, clientPacket);
        compound.putBoolean("HasWorker", hasWorker);
        compound.putInt("ProcessingTimer", processingTimer);
        compound.put("ProcessingStack", processingStack.save(new CompoundTag()));
        compound.putBoolean("HasGlovesUpgrade", hasGlovesUpgrade);
    }

    @Override
    protected void read(CompoundTag compound, boolean clientPacket) {
        super.read(compound, clientPacket);
        hasWorker = compound.getBoolean("HasWorker");
        processingTimer = compound.getInt("ProcessingTimer");
        processingStack = ItemStack.of(compound.getCompound("ProcessingStack"));
        if (compound.contains("HasGlovesUpgrade")) {
            hasGlovesUpgrade = compound.getBoolean("HasGlovesUpgrade");
        } else {
            hasGlovesUpgrade = false;
        }
        cachedWorker = null;
        hasSavedOriginalHand = false;
        savedOriginalHandItem = ItemStack.EMPTY;
        lastHandItem = ItemStack.EMPTY;
    }

    public boolean hasWorker() {
        return hasWorker;
    }

    public boolean isWorkerPlayer() {
        LivingEntity worker = findWorker();
        return worker instanceof Player;
    }
}