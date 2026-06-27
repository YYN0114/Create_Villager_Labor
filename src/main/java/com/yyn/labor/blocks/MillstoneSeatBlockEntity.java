package com.yyn.labor.blocks;

import java.util.List;

import com.simibubi.create.AllRecipeTypes;
import com.simibubi.create.foundation.recipe.RecipeApplier;

import com.yyn.labor.CreateVillagerLabor;

import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

public class MillstoneSeatBlockEntity extends WorkerSeatBlockEntity {

    public MillstoneSeatBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
        maxCooldown = 10;
    }

    public static MillstoneSeatBlockEntity create(BlockPos pos, BlockState state) {
        return new MillstoneSeatBlockEntity(MillstoneSeatBlock.BLOCK_ENTITY_TYPE, pos, state);
    }

    @Override
    protected List<ItemStack> processItem(ItemStack input) {
        var recipe = AllRecipeTypes.MILLING.find(new SingleRecipeInput(input), level);

        if (recipe.isEmpty())
            return List.of();
        
        return RecipeApplier.applyRecipeOn(level, input.copy(), recipe.get().value(), true);
    }

    @Override
    protected void onProcessingComplete() {
        if (level != null && !level.isClientSide) {
            level.playSound(null, worldPosition, CreateVillagerLabor.MILLSTONE_WORK_SOUND.get(),
                net.minecraft.sounds.SoundSource.BLOCKS, 1.0f, 1.0f);
        }
    }
}
