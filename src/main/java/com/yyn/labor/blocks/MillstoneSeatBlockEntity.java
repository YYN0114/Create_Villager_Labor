package com.yyn.labor.blocks;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;

import com.simibubi.create.AllRecipeTypes;
import com.simibubi.create.content.kinetics.crusher.CrushingRecipe;
import com.simibubi.create.content.kinetics.millstone.MillingRecipe;
import com.simibubi.create.content.processing.sequenced.SequencedAssemblyRecipe;
import com.simibubi.create.foundation.recipe.RecipeApplier;
import com.simibubi.create.foundation.recipe.RecipeFinder;

import com.yyn.labor.CreateVillagerLabor;

import net.minecraft.core.BlockPos;
import net.minecraft.core.RegistryAccess;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

public class MillstoneSeatBlockEntity extends WorkerSeatBlockEntity {

    private static final Object millingOrCrushingRecipesKey = new Object();

    // 兼容不同 Create 版本：反射获取 CRUSHING 配方类型（用户"粉碎论"→粉碎轮配方）
    private static final RecipeType<?> CRUSHING_TYPE = resolveCrushingType();

    @SuppressWarnings("unchecked")
    private static RecipeType<?> resolveCrushingType() {
        try {
            java.lang.reflect.Field f = AllRecipeTypes.class.getField("CRUSHING");
            Object provider = f.get(null);
            java.lang.reflect.Method getType = provider.getClass().getMethod("getType");
            return (RecipeType<?>) getType.invoke(provider);
        } catch (Throwable t) {
            CreateVillagerLabor.LOGGER.info("[MillstoneSeat] AllRecipeTypes.CRUSHING not available, crushing recipes disabled. Cause: {}",
                t.toString());
            return null;
        }
    }

    public MillstoneSeatBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state, SeatMaterial material) {
        super(type, pos, state, material);
        maxCooldown = material.scaleCooldown(10);
    }

    @Override
    protected List<ItemStack> processItem(ItemStack input) {
        RegistryAccess reg = level.registryAccess();

        // Priority 1: Sequenced assembly recipes — 先尝试 MILLING 步骤，再尝试 CRUSHING 步骤
        Optional<MillingRecipe> seqMilling = SequencedAssemblyRecipe.getRecipe(
            level, input, AllRecipeTypes.MILLING.getType(), MillingRecipe.class);
        if (seqMilling.isPresent()) {
            MillingRecipe recipe = seqMilling.get();
            if (!AllRecipeTypes.shouldIgnoreInAutomation(recipe)) {
                ItemStack resultItem = recipe.getResultItem(reg);
                if (!filtering.isActive() || filtering.test(resultItem)) {
                    return RecipeApplier.applyRecipeOn(level, input.copy(), recipe, true);
                }
                // 过滤不通过：继续向下，不直接返回空
            }
        }

        // CRUSHING sequenced assembly: removed (static list below already handles crushing recipes)

        // Priority 2: Milling (石磨) + Crushing (粉碎论/粉碎轮) 双配方
        Predicate<Recipe<?>> typePredicate = recipe -> {
            RecipeType<?> rt = recipe.getType();
            if (rt == AllRecipeTypes.MILLING.getType()) return true;
            if (CRUSHING_TYPE != null && rt == CRUSHING_TYPE) return true;
            return false;
        };

        List<Recipe<?>> candidates = RecipeFinder.get(millingOrCrushingRecipesKey, level, typePredicate);

        // 优先级：石磨配方 > 粉碎配方；防止同一输入两种都匹配时直接选粉碎
        List<Recipe<?>> millingFirst = new ArrayList<>();
        List<Recipe<?>> crushingRest = new ArrayList<>();
        for (Recipe<?> r : candidates) {
            if (r.getType() == AllRecipeTypes.MILLING.getType()) millingFirst.add(r);
            else crushingRest.add(r);
        }
        millingFirst.addAll(crushingRest);

        for (Recipe<?> recipe : millingFirst) {
            if (AllRecipeTypes.shouldIgnoreInAutomation(recipe)) continue;
            if (!firstIngredientMatches(input, recipe)) continue;

            // 过滤器嵌入循环：首个不通过的不会挡住后续其它配方
            if (filtering.isActive()) {
                ItemStack rep = recipe.getResultItem(reg);
                if (rep.isEmpty() || !filtering.test(rep)) continue;
            }

            List<ItemStack> outputs = RecipeApplier.applyRecipeOn(level, input.copy(), recipe, true);
            if (!outputs.isEmpty()) return outputs;
        }

        // Priority 3: 兜底全量遍历所有配方（兼容第三方模组把 1:1 加工配方注册为非 Create RecipeType）
        // 纯函数过滤：只保留"单 ingredient 且 ingredient 与 input 匹配"的配方
        for (Recipe<?> recipe : level.getRecipeManager().getRecipes()) {
            RecipeType<?> rt = recipe.getType();
            if (rt == AllRecipeTypes.MILLING.getType()) continue;
            if (CRUSHING_TYPE != null && rt == CRUSHING_TYPE) continue;
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

    /** 1:1 port of RecipeConditions.firstIngredientMatches (not available in 1.20 Create). */
    private static boolean firstIngredientMatches(ItemStack input, Recipe<?> recipe) {
        List<Ingredient> ings = recipe.getIngredients();
        if (ings.isEmpty()) return false;
        return ings.get(0).test(input);
    }

    @Override
    protected void onProcessingComplete() {
        if (level != null && !level.isClientSide) {
            level.playSound(null, worldPosition, CreateVillagerLabor.MILLSTONE_WORK_SOUND.get(),
                net.minecraft.sounds.SoundSource.BLOCKS, 1.0f, 1.0f);
        }
    }
}
