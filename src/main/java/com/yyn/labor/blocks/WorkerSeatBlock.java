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
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;

import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.event.level.BlockEvent;

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

            AABB searchBox = new AABB(pos).inflate(0.5);
            List<LaborEntity> labors = level.getEntitiesOfClass(LaborEntity.class, searchBox);
            for (LaborEntity labor : labors) {
                labor.discard();
            }
        }
        super.onRemove(state, level, pos, newState, movedByPiston);
    }
    // IHasSeatMaterial 已提取为顶级接口 IHasSeatMaterial.java

    private void dropSeatContents(Level level, BlockPos pos, BlockState state, BlockEntity blockEntity,
                                  Player player, ItemStack tool, boolean dropBlockItem) {
        if (!(level instanceof ServerLevel serverLevel)) return;

        // 1) 先调用 dropResources 让 SmartBlockEntity/行为系统把过滤器等内容物吐出来
        if (blockEntity != null) {
            try {
                Block.dropResources(state, serverLevel, pos, blockEntity, player, tool);
            } catch (Exception ignored) {}
        }

        // 2) 手动掉落方块本体（缺少 loot_table 时 dropResources 不会掉）
        if (dropBlockItem && asItem() != null && asItem() != Blocks.AIR.asItem()) {
            Block.popResource(serverLevel, pos, new ItemStack(asItem()));
        }
    }

    /**
     * 玩家徒手/工具挖掘：由于缺少 loot_table JSON，原版 getDrops 不掉方块本体。
     * 这里重写 playerDestroy，手动掉出方块物品 + 方块实体内容物。
     */
    @Override
    public void playerDestroy(Level level, Player player, BlockPos pos, BlockState state, BlockEntity blockEntity, ItemStack tool) {
        boolean isCreative = player.getAbilities().instabuild;
        boolean isCreativeSeat = this instanceof IHasSeatMaterial hs && hs.getMaterial() == SeatMaterial.CREATIVE;

        if (!level.isClientSide) {
            dropSeatContents(level, pos, state, blockEntity, player, tool,
                !isCreative && !isCreativeSeat);
        }
        super.playerDestroy(level, player, pos, state, blockEntity, tool);
    }

    // 扳手潜行右键 = 拆除（Create IWrenchable 默认约定）
    // 原代码 Block.dropResources 依赖 loot_table，没有则不掉方块。改为手动 dropSeatContents。
    @Override
    public InteractionResult onSneakWrenched(BlockState state, UseOnContext context) {
        Level world = context.getLevel();
        BlockPos pos = context.getClickedPos();
        Player player = context.getPlayer();

        if (!(world instanceof ServerLevel serverLevel))
            return InteractionResult.SUCCESS;

        BlockEvent.BreakEvent event = new BlockEvent.BreakEvent(world, pos, world.getBlockState(pos), player);
        MinecraftForge.EVENT_BUS.post(event);
        if (event.isCanceled())
            return InteractionResult.SUCCESS;

        BlockEntity be = world.getBlockEntity(pos);
        boolean isCreative = player != null && player.getAbilities().instabuild;
        boolean isCreativeSeat = this instanceof IHasSeatMaterial hs && hs.getMaterial() == SeatMaterial.CREATIVE;

        dropSeatContents(serverLevel, pos, state, be, player, context.getItemInHand(),
            !isCreative && !isCreativeSeat);

        state.spawnAfterBreak(serverLevel, pos, ItemStack.EMPTY, true);
        world.destroyBlock(pos, false);
        IWrenchable.playRemoveSound(world, pos);
        return InteractionResult.SUCCESS;
    }

    // 扳手普通右键 = 不旋转
    @Override
    public InteractionResult onWrenched(BlockState state, UseOnContext context) {
        return InteractionResult.SUCCESS;
    }

    // 1.20.1 Forge 仍使用老签名 use(...)（而非 useItemOn）
    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hitResult) {
        if (player.isShiftKeyDown() || player instanceof FakePlayer)
            return InteractionResult.PASS;

        ItemStack stack = player.getItemInHand(hand);
        DyeColor dyeColor = DyeColor.getColor(stack);
        if (dyeColor != null && dyeColor != state.getValue(COLOR)) {
            if (level.isClientSide)
                return InteractionResult.SUCCESS;
            level.setBlockAndUpdate(pos, state.setValue(COLOR, dyeColor));
            if (!player.getAbilities().instabuild)
                stack.shrink(1);
            return InteractionResult.SUCCESS;
        }

        List<SeatEntity> seats = level.getEntitiesOfClass(SeatEntity.class, new AABB(pos));
        if (!seats.isEmpty()) {
            SeatEntity seatEntity = seats.get(0);
            List<Entity> passengers = seatEntity.getPassengers();
            if (!passengers.isEmpty() && passengers.get(0) instanceof Player)
                return InteractionResult.PASS;
            if (!level.isClientSide) {
                seatEntity.ejectPassengers();
                player.startRiding(seatEntity);
            }
            return InteractionResult.SUCCESS;
        }

        if (level.isClientSide)
            return InteractionResult.SUCCESS;
        SeatBlock.sitDown(level, pos, SeatBlock.getLeashed(level, player).or(player));
        return InteractionResult.SUCCESS;
    }
}