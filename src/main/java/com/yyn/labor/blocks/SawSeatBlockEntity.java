package com.yyn.labor.blocks;

import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;

import com.simibubi.create.AllRecipeTypes;
import com.simibubi.create.content.kinetics.saw.CuttingRecipe;
import com.simibubi.create.content.processing.sequenced.SequencedAssemblyRecipe;
import com.simibubi.create.foundation.recipe.RecipeApplier;
import com.simibubi.create.foundation.recipe.RecipeFinder;

import com.yyn.labor.CreateVillagerLabor;

import net.minecraft.core.BlockPos;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

public class SawSeatBlockEntity extends WorkerSeatBlockEntity {

    private static final Object cuttingRecipesKey = new Object();
    private static final RecipeType<?> woodcuttingRecipeType;

    static {
        RecipeType<?> type = null;
        try {
            ResourceLocation id = new ResourceLocation("druidcraft", "woodcutting");
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
        RegistryAccess reg = level.registryAccess();

        // Priority 1: Sequenced assembly recipes (with shouldIgnore + filter check)
        Optional<CuttingRecipe> seqRecipe = SequencedAssemblyRecipe.getRecipe(
            level, input, AllRecipeTypes.CUTTING.getType(), CuttingRecipe.class);
        if (seqRecipe.isPresent()) {
            CuttingRecipe recipe = seqRecipe.get();
            // Note: SequencedAssemblyRecipe holder cannot be checked via shouldIgnoreInAutomation
            //       in 1.20 (no RecipeHolder). Treat as valid directly.
            ItemStack resultItem = recipe.getResultItem(reg);
            if (!filtering.isActive() || filtering.test(resultItem)) {
                return RecipeApplier.applyRecipeOn(level, input.copy(), recipe, true);
            }
        }

        // Priority 2: Cutting + Stonecutter + optional Woodcutting (same recipe set as Create saw)
        Predicate<Recipe<?>> typePredicate = recipe -> {
            RecipeType<?> rt = recipe.getType();
            if (rt == AllRecipeTypes.CUTTING.getType()) return true;
            if (rt == RecipeType.STONECUTTING) return true;
            if (woodcuttingRecipeType != null && rt == woodcuttingRecipeType) return true;
            return false;
        };

        List<Recipe<?>> candidates = RecipeFinder.get(cuttingRecipesKey, level, typePredicate);

        for (Recipe<?> recipe : candidates) {
            if (AllRecipeTypes.shouldIgnoreInAutomation(recipe)) continue;
            if (!firstIngredientMatches(input, recipe)) continue;

            // Filter check (representative output via getResultItem)
            if (filtering.isActive()) {
                ItemStack rep = recipe.getResultItem(reg);
                if (rep.isEmpty() || !filtering.test(rep)) continue;
            }

            List<ItemStack> outputs = RecipeApplier.applyRecipeOn(level, input.copy(), recipe, true);
            if (!outputs.isEmpty()) return outputs;
        }


        // Priority 3: 兜底全量遍历所有配方（兼容第三方模组把切割/石头切半等配方注册为非 Create RecipeType）
        // 纯函数过滤：只保留"单 ingredient 且 ingredient 与 input 匹配"的配方
        for (Recipe<?> recipe : level.getRecipeManager().getRecipes()) {
            RecipeType<?> rt = recipe.getType();
            if (rt == AllRecipeTypes.CUTTING.getType()) continue;
            if (rt == RecipeType.STONECUTTING) continue;
            if (woodcuttingRecipeType != null && rt == woodcuttingRecipeType) continue;
            List<Ingredient> ings = recipe.getIngredients();
            if (ings.size() != 1) continue;
            if (!ings.get(0).test(input)) continue;
            ItemStack rep = recipe.getResultItem(reg);
            if (rep.isEmpty()) continue;
            if (filtering.isActive() && !filtering.test(rep)) continue;
            if (!AllRecipeTypes.shouldIgnoreInAutomation(recipe)) {
                List<ItemStack> outputs = RecipeApplier.applyRecipeOn(level, input.copy(), recipe, true);
                if (!outputs.isEmpty()) return outputs;
            }
        }
        return List.of();
    }

    /**
     * Create Saw-compatible first ingredient check (mirrors RecipeConditions.firstIngredientMatches).
     * Returns true if the recipe's first ingredient matches the input stack.
     */
    private static boolean firstIngredientMatches(ItemStack input, Recipe<?> recipe) {
        List<Ingredient> ings = recipe.getIngredients();
        if (ings.isEmpty()) return false;
        return ings.get(0).test(input);
    }

    @Override
    protected void onProcessingComplete() {
        if (level != null && !level.isClientSide) {
            level.playSound(null, worldPosition, CreateVillagerLabor.SAW_WORK_SOUND.get(),
                net.minecraft.sounds.SoundSource.BLOCKS, 1.0f, 1.0f);
        }
    }
}
