package com.yyn.labor.blocks;

import java.util.List;
import java.util.Optional;

import com.simibubi.create.AllRecipeTypes;
import com.simibubi.create.content.kinetics.press.PressingRecipe;
import com.simibubi.create.content.processing.sequenced.SequencedAssemblyRecipe;
import com.simibubi.create.foundation.recipe.RecipeApplier;

import com.yyn.labor.CreateVillagerLabor;

import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

import net.minecraftforge.items.ItemStackHandler;
import net.minecraftforge.items.wrapper.RecipeWrapper;

public class PressSeatBlockEntity extends WorkerSeatBlockEntity {

    public PressSeatBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state, SeatMaterial material) {
        super(type, pos, state, material);
        maxCooldown = material.scaleCooldown(10);
    }

    @Override
    protected List<ItemStack> processItem(ItemStack input) {
        // Priority 1: Sequenced assembly recipes (e.g. sturdy plate)
        Optional<PressingRecipe> seqRecipe = SequencedAssemblyRecipe.getRecipe(
            level, input, AllRecipeTypes.PRESSING.getType(), PressingRecipe.class);
        if (seqRecipe.isPresent()) {
            return RecipeApplier.applyRecipeOn(level, input.copy(), seqRecipe.get(), true);
        }

        // Priority 2: Standalone pressing recipes
        ItemStackHandler inv = new ItemStackHandler(1);
        inv.setStackInSlot(0, input);
        Optional<PressingRecipe> recipe = AllRecipeTypes.PRESSING.find(new RecipeWrapper(inv), level);
        if (recipe.isEmpty())
            return List.of();

        return RecipeApplier.applyRecipeOn(level, input.copy(), recipe.get(), true);
    }

    @Override
    protected void onProcessingComplete() {
        if (level != null && !level.isClientSide) {
            level.playSound(null, worldPosition, CreateVillagerLabor.PRESS_WORK_SOUND.get(),
                net.minecraft.sounds.SoundSource.BLOCKS, 1.0f, 1.0f);
        }
    }
}
