package com.yyn.labor.blocks;

import java.util.List;

import com.simibubi.create.AllRecipeTypes;
import com.simibubi.create.foundation.recipe.RecipeApplier;

import com.yyn.labor.CreateVillagerLabor;

import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

import net.neoforged.neoforge.items.ItemStackHandler;
import net.neoforged.neoforge.items.wrapper.RecipeWrapper;

public class SawSeatBlockEntity extends WorkerSeatBlockEntity {

    public SawSeatBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
        maxCooldown = 10;
    }

    public static SawSeatBlockEntity create(BlockPos pos, BlockState state) {
        return new SawSeatBlockEntity(SawSeatBlock.BLOCK_ENTITY_TYPE, pos, state);
    }

    @Override
    protected List<ItemStack> processItem(ItemStack input) {
        ItemStackHandler inv = new ItemStackHandler(1);
        inv.setStackInSlot(0, input);
        var recipe = AllRecipeTypes.CUTTING.find(new RecipeWrapper(inv), level);

        if (recipe.isEmpty())
            return List.of();

        return RecipeApplier.applyRecipeOn(level, input.copy(), recipe.get().value(), true);
    }

    @Override
    protected void onProcessingComplete() {
        if (level != null && !level.isClientSide) {
            level.playSound(null, worldPosition, CreateVillagerLabor.SAW_WORK_SOUND.get(),
                net.minecraft.sounds.SoundSource.BLOCKS, 1.0f, 1.0f);
        }
    }
}
