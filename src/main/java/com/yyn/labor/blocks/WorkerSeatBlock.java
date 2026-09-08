package com.yyn.labor.blocks;

import java.util.List;

import com.simibubi.create.content.contraptions.actors.seat.SeatBlock;
import com.simibubi.create.content.contraptions.actors.seat.SeatEntity;
import com.simibubi.create.content.equipment.wrench.IWrenchable;
import com.yyn.labor.entity.LaborEntity;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;

import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.event.level.BlockEvent;

public abstract class WorkerSeatBlock extends SeatBlock implements IWrenchable {

    public static final EnumProperty<DyeColor> COLOR = EnumProperty.create("color", DyeColor.class);
    public static final BooleanProperty WORKING = BooleanProperty.create("working");

    public WorkerSeatBlock(Properties properties, DyeColor defaultColor) {
        super(properties, defaultColor);
        registerDefaultState(defaultBlockState()
            .setValue(COLOR, defaultColor)
            .setValue(WORKING, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(COLOR, WORKING);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext ctx) {
        return withWater(super.getStateForPlacement(ctx), ctx);
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        if (!state.is(newState.getBlock())) {
            // 方块被替换（破坏/替换等）时：先让工位方块实体还原工人原始主手物品
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof WorkerSeatBlockEntity wsbe) {
                wsbe.restoreWorkerOnRemoval();
            }

            // 清理性能升级生成的 LaborEntity
            AABB searchBox = new AABB(pos).inflate(0.5);
            List<LaborEntity> labors = level.getEntitiesOfClass(LaborEntity.class, searchBox);
            for (LaborEntity labor : labors) {
                labor.discard();
            }
        }
        super.onRemove(state, level, pos, newState, movedByPiston);
    }

    // ============================================================
    // 拆除返还：由于没有配置 loot_table JSON，统一在 Block 层通过
    // getCloneItemStack + playerDestroy / onSneakWrenched 手动掉落方块物品
    // （Create 的 SeatBlock 继承自本类，父类 getDrops 不会为子类生成自定义方块物品）
    // ============================================================

    /** 把工位方块实体内部物品（filter、手套升级等）+方块本体掉落出来 */
    private void dropSeatContents(Level level, BlockPos pos, BlockState state, BlockEntity blockEntity,
                                  Player player, ItemStack tool, boolean dropBlockItem) {
        if (!(level instanceof ServerLevel serverLevel)) return;

        // 1) 先让 Create/SmartBlockEntity 吐出自己的方块实体内容物（Filter 等）
        if (blockEntity != null) {
            try {
                BlockEntity be = level.getBlockEntity(pos);
                // 触发一次 SmartBlockEntity 的逻辑：父类 Block.dropResources 仅在有 loot table 时掉方块物品
                // 这里直接以 destroyBlock(dropResources=true) 的思路，单独处理内容物
                if (be instanceof com.simibubi.create.foundation.blockEntity.SmartBlockEntity sbe) {
                    // 用 Create 的 dropResources 走 BlockEntityBehaviour 的内容物掉落（如过滤器）
                    Block.dropResources(state, serverLevel, pos, be, player, tool);
                }
            } catch (Exception ignored) {}
        }

        // 2) 手动掉落方块物品本体（本模组尚未创建 loot_table JSON，因此 dropResources 不会替我们掉）
        if (dropBlockItem && asItem() != null && asItem() != Blocks.AIR.asItem()) {
            ItemStack blockStack = new ItemStack(asItem());
            // 保留颜色（COLOR 属性）—— BlockItem 在放置时会匹配，这里一并加到掉落物
            if (state.hasProperty(COLOR)) {
                // 染色状态通过 blockstate 存储，BlockItem 放置时会使用默认色
                // 若安装羊毛毯染色（原版 SeatBlock 的机制）等后续再扩展
            }
            Block.popResource(serverLevel, pos, blockStack);
        }
    }

    /**
     * 玩家徒手/工具挖掘：触发掉落方块物品 + 方块实体内容物。
     * 原版默认调用 getDrops -> LootTable，没有 loot_table 就不掉方块。
     * 这里重写 playerDestroy，直接手动掉出。
     */
    @Override
    public void playerDestroy(Level level, Player player, BlockPos pos, BlockState state, BlockEntity blockEntity, ItemStack tool) {
        // 注意：super.playerDestroy 不会替我们掉方块物品（没有loot table），但会触发经验/破坏统计等
        boolean isCreative = player.getAbilities().instabuild;
        boolean isCreativeSeat = this instanceof IHasSeatMaterial hs && hs.getMaterial() == SeatMaterial.CREATIVE;

        // 先掉内容物 + 方块物品（创造版工位方块不掉）
        if (!level.isClientSide) {
            dropSeatContents(level, pos, state, blockEntity, player, tool,
                !isCreative && !isCreativeSeat /* 生存+非创造版才掉方块本体 */);
        }
        super.playerDestroy(level, player, pos, state, blockEntity, tool);
    }
    // IHasSeatMaterial 已提取为顶级接口 IHasSeatMaterial.java

    // 扳手潜行右键 = 拆除（Create IWrenchable 默认约定）。
    // 修正：原代码 Block.dropResources 依赖 loot_table（本模组没有），所以不掉方块。
    // 改为手动调用 dropSeatContents() 掉出方块本体 + 方块实体内容物。
    @Override
    public InteractionResult onSneakWrenched(BlockState state, UseOnContext context) {
        Level world = context.getLevel();
        BlockPos pos = context.getClickedPos();
        Player player = context.getPlayer();

        if (!(world instanceof ServerLevel serverLevel))
            return InteractionResult.SUCCESS;

        BlockEvent.BreakEvent event = new BlockEvent.BreakEvent(world, pos, world.getBlockState(pos), player);
        NeoForge.EVENT_BUS.post(event);
        if (event.isCanceled())
            return InteractionResult.SUCCESS;

        BlockEntity be = world.getBlockEntity(pos);
        boolean isCreative = player != null && player.getAbilities().instabuild;
        boolean isCreativeSeat = this instanceof IHasSeatMaterial hs && hs.getMaterial() == SeatMaterial.CREATIVE;

        // （与 playerDestroy 保持一致）掉方块实体内容物 + 方块本体
        dropSeatContents(serverLevel, pos, state, be, player, context.getItemInHand(),
            !isCreative && !isCreativeSeat);

        state.spawnAfterBreak(serverLevel, pos, ItemStack.EMPTY, true);
        world.destroyBlock(pos, false);
        IWrenchable.playRemoveSound(world, pos);
        return InteractionResult.SUCCESS;
    }

    // 扳手普通右键 = 不旋转（工位方块没有可旋转的方向属性）
    @Override
    public InteractionResult onWrenched(BlockState state, UseOnContext context) {
        return InteractionResult.SUCCESS;
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hitResult) {
        if (player.isShiftKeyDown() || player instanceof FakePlayer)
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;

        DyeColor dyeColor = DyeColor.getColor(stack);
        if (dyeColor != null && dyeColor != state.getValue(COLOR)) {
            if (level.isClientSide)
                return ItemInteractionResult.SUCCESS;
            level.setBlockAndUpdate(pos, state.setValue(COLOR, dyeColor));
            if (!player.getAbilities().instabuild)
                stack.shrink(1);
            return ItemInteractionResult.SUCCESS;
        }

        List<SeatEntity> seats = level.getEntitiesOfClass(SeatEntity.class, new AABB(pos));
        if (!seats.isEmpty()) {
            SeatEntity seatEntity = seats.get(0);
            List<Entity> passengers = seatEntity.getPassengers();
            if (!passengers.isEmpty() && passengers.get(0) instanceof Player)
                return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
            if (!level.isClientSide) {
                seatEntity.ejectPassengers();
                player.startRiding(seatEntity);
            }
            return ItemInteractionResult.SUCCESS;
        }

        if (level.isClientSide)
            return ItemInteractionResult.SUCCESS;
        SeatBlock.sitDown(level, pos, SeatBlock.getLeashed(level, player).or(player));
        return ItemInteractionResult.SUCCESS;
    }
}