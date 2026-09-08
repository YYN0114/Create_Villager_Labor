package com.yyn.labor.blocks;

import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;

import com.simibubi.create.AllRecipeTypes;
import com.simibubi.create.content.kinetics.saw.CuttingRecipe;
import com.simibubi.create.content.processing.sequenced.SequencedAssemblyRecipe;
import com.simibubi.create.foundation.recipe.RecipeApplier;
import com.simibubi.create.foundation.recipe.RecipeConditions;
import com.simibubi.create.foundation.recipe.RecipeFinder;

import com.yyn.labor.CreateVillagerLabor;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

public class SawSeatBlockEntity extends WorkerSeatBlockEntity {

    private static final Object cuttingRecipesKey = new Object();
    private static final RecipeType<?> woodcuttingRecipeType;

    static {
        RecipeType<?> type = null;
        try {
            ResourceLocation id = ResourceLocation.fromNamespaceAndPath("druidcraft", "woodcutting");
            type = BuiltInRegistries.RECIPE_TYPE.get(id);
        } catch (Exception e) {
            type = null;
        }
        woodcuttingRecipeType = type;
    }

    public SawSeatBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state, SeatMaterial material) {
        super(type, pos, state, material);
        maxCooldown = material.scaleCooldown(10);
    }

    @Override
    protected List<ItemStack> processItem(ItemStack input) {
        // Priority 1: Sequenced assembly recipes (with shouldIgnore + filter check)
        Optional<RecipeHolder<CuttingRecipe>> seqRecipe = SequencedAssemblyRecipe.getRecipe(
            level, input, AllRecipeTypes.CUTTING.getType(), CuttingRecipe.class);
        if (seqRecipe.isPresent()) {
            RecipeHolder<CuttingRecipe> holder = seqRecipe.get();
            if (!AllRecipeTypes.shouldIgnoreInAutomation(holder)) {
                ItemStack resultItem = holder.value().getResultItem(level.registryAccess());
                if (!filtering.isActive() || filtering.test(resultItem)) {
                    return RecipeApplier.applyRecipeOn(level, input.copy(), holder.value(), true);
                }
            }
        }

        // Priority 2: Cutting + Stonecutter + optional Woodcutting (same recipe set as Create saw)
        Predicate<RecipeHolder<? extends Recipe<?>>> typePredicate = holder -> {
            RecipeType<?> rt = holder.value().getType();
            if (rt == AllRecipeTypes.CUTTING.getType()) return true;
            if (rt == RecipeType.STONECUTTING) return true;
            if (woodcuttingRecipeType != null && rt == woodcuttingRecipeType) return true;
            return false;
        };

        List<RecipeHolder<? extends Recipe<?>>> candidates = RecipeFinder.get(
            cuttingRecipesKey, level, typePredicate);

        for (RecipeHolder<? extends Recipe<?>> holder : candidates) {
            Recipe<?> recipe = holder.value();

            if (AllRecipeTypes.shouldIgnoreInAutomation(holder)) continue;
            if (!RecipeConditions.firstIngredientMatches(input).test(holder)) continue;

            // Filter check (representative output via getResultItem; exact rolling handled by RecipeApplier)
            if (filtering.isActive()) {
                ItemStack rep = recipe.getResultItem(level.registryAccess());
                if (rep.isEmpty() || !filtering.test(rep)) continue;
            }

            List<ItemStack> outputs = RecipeApplier.applyRecipeOn(level, input.copy(), recipe, true);
            if (!outputs.isEmpty()) return outputs;
        }

        // Priority 3: 兜底全量遍历所有配方（兼容第三方模组把切割/石头切半等配方注册为非 Create RecipeType）
        // 纯函数过滤：只保留"单 ingredient 且 ingredient 与 input 匹配"的配方
        HolderLookup.Provider reg = level.registryAccess();
        for (RecipeHolder<?> holder : level.getRecipeManager().getRecipes()) {
            Recipe<?> recipe = holder.value();
            RecipeType<?> rt = recipe.getType();
            if (rt == AllRecipeTypes.CUTTING.getType()) continue;
            if (rt == RecipeType.STONECUTTING) continue;
            if (woodcuttingRecipeType != null && rt == woodcuttingRecipeType) continue;
            java.util.List<net.minecraft.world.item.crafting.Ingredient> ings = recipe.getIngredients();
            if (ings.size() != 1) continue;
            if (!ings.get(0).test(input)) continue;
            ItemStack rep = recipe.getResultItem(reg);
            if (rep.isEmpty()) continue;
            if (filtering.isActive() && !filtering.test(rep)) continue;
            if (!AllRecipeTypes.shouldIgnoreInAutomation(holder)) {
                java.util.List<ItemStack> outputs = RecipeApplier.applyRecipeOn(level, input.copy(), recipe, true);
                if (!outputs.isEmpty()) return outputs;
            }
        }

        return List.of();
    }

    @Override
    protected void onProcessingComplete() {
        if (level != null && !level.isClientSide) {
            level.playSound(null, worldPosition, CreateVillagerLabor.SAW_WORK_SOUND.get(),
                net.minecraft.sounds.SoundSource.BLOCKS, 1.0f, 1.0f);
        }
    }
}
