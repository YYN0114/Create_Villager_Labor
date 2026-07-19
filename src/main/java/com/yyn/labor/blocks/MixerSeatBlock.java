package com.yyn.labor.blocks;

import net.minecraft.core.BlockPos;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.MapColor;

public class MixerSeatBlock extends WorkerSeatBlock implements EntityBlock {

    public static BlockEntityType<MixerSeatBlockEntity> BLOCK_ENTITY_TYPE;

    public MixerSeatBlock() {
        super(BlockBehaviour.Properties.of()
            .mapColor(MapColor.COLOR_GRAY)
            .strength(0.5f), DyeColor.LIGHT_BLUE);
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return BLOCK_ENTITY_TYPE.create(pos, state);
    }

    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return type == BLOCK_ENTITY_TYPE ? ($0, $1, $2, be) -> ((WorkerSeatBlockEntity)be).tick() : null;
    }
}
