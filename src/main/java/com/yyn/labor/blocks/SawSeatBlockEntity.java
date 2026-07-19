package com.yyn.labor.blocks;

import java.util.List;
import java.util.Optional;

import com.simibubi.create.AllRecipeTypes;
import com.simibubi.create.content.kinetics.saw.CuttingRecipe;
import com.simibubi.create.content.processing.sequenced.SequencedAssemblyRecipe;
import com.simibubi.create.foundation.recipe.RecipeApplier;

import com.yyn.labor.CreateVillagerLabor;

import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

import net.minecraftforge.items.ItemStackHandler;
import net.minecraftforge.items.wrapper.RecipeWrapper;

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
        // Priority 1: Sequenced assembly recipes
        Optional<CuttingRecipe> seqRecipe = SequencedAssemblyRecipe.getRecipe(
            level, input, AllRecipeTypes.CUTTING.getType(), CuttingRecipe.class);
        if (seqRecipe.isPresent()) {
            return RecipeApplier.applyRecipeOn(level, input.copy(), seqRecipe.get(), true);
        }

        // Priority 2: Standalone cutting recipes
        ItemStackHandler inv = new ItemStackHandler(1);
        inv.setStackInSlot(0, input);
        var recipe = AllRecipeTypes.CUTTING.find(new RecipeWrapper(inv), level);
        if (recipe.isEmpty())
            return List.of();

        return RecipeApplier.applyRecipeOn(level, input.copy(), recipe.get(), true);
    }

    @Override
    protected void onProcessingComplete() {
        if (level != null && !level.isClientSide) {
            level.playSound(null, worldPosition, CreateVillagerLabor.SAW_WORK_SOUND.get(),
                net.minecraft.sounds.SoundSource.BLOCKS, 1.0f, 1.0f);
        }
    }
}
