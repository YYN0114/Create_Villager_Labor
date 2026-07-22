package com.yyn.labor.blocks;

import java.util.List;

import com.simibubi.create.content.contraptions.actors.seat.SeatBlock;
import com.simibubi.create.content.contraptions.actors.seat.SeatEntity;

import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;

import net.minecraftforge.common.util.FakePlayer;

public abstract class WorkerSeatBlock extends SeatBlock {

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
