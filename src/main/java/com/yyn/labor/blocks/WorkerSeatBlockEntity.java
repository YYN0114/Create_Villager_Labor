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
import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import com.simibubi.create.foundation.blockEntity.behaviour.filtering.FilteringBehaviour;

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

    private static Class<?> maidClass;
    private static boolean maidChecked = false;

    public WorkerSeatBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
        maxCooldown = 5;
        workerCheckCooldown = 0;
        hasWorker = false;
        processingTimer = 0;
        processingStack = ItemStack.EMPTY;
        outputCooldown = 0;
        outputCooldownDuration = 20;
        lastHandItem = ItemStack.EMPTY;
        beltDirection = null;
        rotationTick = 0;
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

        if (hasWorker)
            processWork();
    }

    protected void processWork() {
        checkHandItem();

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

        if (beltDirection != null) {
            rotationTick++;
            if (rotationTick >= 5) {
                rotationTick = 0;
                rotateWorkerTowardBelt();
            }
        }

        updateWorkingState();
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
        if (!maidChecked) {
            try {
                maidClass = Class.forName("com.github.tartaricacid.touhoulittlemaid.entity.passive.EntityMaid");
            } catch (ClassNotFoundException e1) {
                try {
                    maidClass = Class.forName("touhou_little_maid.entity.passive.MaidEntity");
                } catch (ClassNotFoundException e2) {
                    maidClass = null;
                }
            }
            maidChecked = true;
        }
        return maidClass != null && maidClass.isInstance(entity);
    }

    protected void checkWorker() {
        if (workerCheckCooldown > 0) {
            workerCheckCooldown--;
            return;
        }
        workerCheckCooldown = 5;

        boolean newHasWorker = isWorkerPresent();
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
                    if (passenger instanceof Villager || passenger instanceof Player || isMaidEntity(passenger))
                        return true;
                }
            }
        }

        for (Entity entity : level.getEntitiesOfClass(Player.class, searchBox)) {
            if (entity.isPassenger()) {
                Entity vehicle = entity.getVehicle();
                if (vehicle instanceof SeatEntity && vehicle.blockPosition().equals(worldPosition))
                    return true;
            }
        }

        return false;
    }

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

    private LivingEntity findWorker() {
        for (SeatEntity seatEntity : level.getEntitiesOfClass(SeatEntity.class, new AABB(worldPosition))) {
            if (!seatEntity.isVehicle())
                continue;
            for (Entity passenger : seatEntity.getPassengers()) {
                if (passenger instanceof LivingEntity living)
                    return living;
            }
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

        int remaining = found.stack.getCount() - 1;
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
        processingCopy.setCount(1);
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

        ItemStack processingCopy = heldItem.copy();
        processingCopy.setCount(1);
        processingStack = processingCopy;
        int remaining = heldItem.getCount() - 1;
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

    protected boolean tryTakeFromBasin(BlockPos basinPos) {
        BlockEntity be = level.getBlockEntity(basinPos);
        if (be == null)
            return false;

        LazyOptional<IItemHandler> cap = be.getCapability(ForgeCapabilities.ITEM_HANDLER, Direction.UP);
        IItemHandler handler = cap.orElse(null);
        if (handler == null)
            return false;

        for (int slot = 0; slot < handler.getSlots(); slot++) {
            ItemStack stack = handler.getStackInSlot(slot);
            if (stack.isEmpty())
                continue;
            if (!itemCanBeProcessed(stack))
                continue;

            ItemStack extracted = handler.extractItem(slot, 1, false);
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

    private void finishProcessing() {
        if (processingStack.isEmpty())
            return;

        List<ItemStack> outputs = processItem(processingStack);

        if (outputs.isEmpty()) {
            LOGGER.warn("Processing failed for {} at {}, dropping item to prevent loss",
                processingStack.getItem(), worldPosition);
            Block.popResource(level, worldPosition.above(), processingStack.copy());
            processingStack = ItemStack.EMPTY;
            updateHand();
            outputCooldown = outputCooldownDuration;
            notifyUpdate();
            return;
        }

        // Keep processingStack for rendering during cooldown;
        // it will be cleared when outputCooldown reaches 0 in processWork()
        updateHand();

        for (ItemStack output : outputs) {
            if (!outputToAdjacent(output))
                Block.popResource(level, worldPosition.above(), output);
        }

        // Immediately clear the worker's hand to prevent item duplication
        // (renderer uses processingStack, which is kept for rendering during cooldown)
        LivingEntity worker = findWorker();
        if (worker != null) {
            worker.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
        }
        lastHandItem = ItemStack.EMPTY;

        outputCooldown = outputCooldownDuration;
        notifyUpdate();

        onProcessingComplete();
    }

    /**
     * Checks if the worker's hand item was externally modified (e.g. by a belt
     * pushing items into the player). If so, ejects the foreign item and restores
     * the expected hand item to prevent item loss/duplication.
     */
    private void checkHandItem() {
        LivingEntity worker = findWorker();
        if (worker == null)
            return;

        ItemStack currentHand = worker.getItemInHand(InteractionHand.MAIN_HAND);
        if (ItemStack.matches(currentHand, lastHandItem))
            return;

        // Hand was externally modified
        if (!currentHand.isEmpty()) {
            // Eject the foreign item to prevent loss
            Block.popResource(level, worldPosition.above(), currentHand.copy());
        }
        // Restore the expected hand item
        worker.setItemInHand(InteractionHand.MAIN_HAND, lastHandItem.isEmpty() ? ItemStack.EMPTY : lastHandItem.copy());
    }

    protected void onProcessingComplete() {
    }

    protected void updateHand() {
        ItemStack toHold = getHandItem();
        if (ItemStack.matches(toHold, lastHandItem))
            return;
        lastHandItem = toHold.copy();

        LivingEntity worker = findWorker();
        if (worker != null)
            worker.setItemInHand(InteractionHand.MAIN_HAND, toHold);
    }

    protected ItemStack getHandItem() {
        return processingStack.isEmpty() ? ItemStack.EMPTY : processingStack;
    }

    private boolean outputToAdjacent(ItemStack stack) {
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

    private boolean isBasinBlock(BlockState state) {
        ResourceLocation key = state.getBlock().builtInRegistryHolder().key().location();
        return "create".equals(key.getNamespace()) && "basin".equals(key.getPath());
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

    private boolean outputToBasin(ItemStack stack, BlockPos basinPos) {
        BlockEntity be = level.getBlockEntity(basinPos);
        if (be == null)
            return false;

        LazyOptional<IItemHandler> cap = be.getCapability(ForgeCapabilities.ITEM_HANDLER, Direction.UP);
        IItemHandler handler = cap.orElse(null);
        if (handler == null)
            return false;

        ItemStack remainder = stack.copy();
        for (int slot = 0; slot < handler.getSlots() && !remainder.isEmpty(); slot++) {
            remainder = handler.insertItem(slot, remainder, false);
        }
        be.setChanged();
        return remainder.isEmpty();
    }

    protected abstract List<ItemStack> processItem(ItemStack input);

    @Override
    public void write(CompoundTag compound, boolean clientPacket) {
        super.write(compound, clientPacket);
        compound.putBoolean("HasWorker", hasWorker);
        compound.putInt("ProcessingTimer", processingTimer);
        compound.put("ProcessingStack", processingStack.save(new CompoundTag()));
    }

    @Override
    protected void read(CompoundTag compound, boolean clientPacket) {
        super.read(compound, clientPacket);
        hasWorker = compound.getBoolean("HasWorker");
        processingTimer = compound.getInt("ProcessingTimer");
        processingStack = ItemStack.of(compound.getCompound("ProcessingStack"));
    }

    public boolean hasWorker() {
        return hasWorker;
    }
}
