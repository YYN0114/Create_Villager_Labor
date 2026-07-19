package com.yyn.labor.blocks;

import java.util.List;

import com.simibubi.create.AllRecipeTypes;
import com.simibubi.create.foundation.recipe.RecipeApplier;

import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

import net.minecraftforge.items.ItemStackHandler;
import net.minecraftforge.items.wrapper.RecipeWrapper;

public class MixerSeatBlockEntity extends WorkerSeatBlockEntity {

    public MixerSeatBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
        maxCooldown = 10;
    }

    public static MixerSeatBlockEntity create(BlockPos pos, BlockState state) {
        return new MixerSeatBlockEntity(MixerSeatBlock.BLOCK_ENTITY_TYPE, pos, state);
    }

    @Override
    protected List<ItemStack> processItem(ItemStack input) {
        ItemStackHandler inv = new ItemStackHandler(1);
        inv.setStackInSlot(0, input);
        var recipe = AllRecipeTypes.MIXING.find(new RecipeWrapper(inv), level);

        if (recipe.isEmpty())
            return List.of();

        return RecipeApplier.applyRecipeOn(level, input.copy(), recipe.get(), true);
    }
}
