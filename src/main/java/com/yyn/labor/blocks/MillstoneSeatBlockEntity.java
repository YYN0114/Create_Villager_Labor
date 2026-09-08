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
import com.simibubi.create.foundation.recipe.RecipeConditions;
import com.simibubi.create.foundation.recipe.RecipeFinder;

import com.yyn.labor.CreateVillagerLabor;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;
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
        // Priority 1: Sequenced assembly recipes — 先尝试 MILLING 步骤，再尝试 CRUSHING 步骤
        HolderLookup.Provider registries = level.registryAccess();

        Optional<RecipeHolder<MillingRecipe>> seqMilling = SequencedAssemblyRecipe.getRecipe(
            level, input, AllRecipeTypes.MILLING.getType(), MillingRecipe.class);
        if (seqMilling.isPresent()) {
            RecipeHolder<MillingRecipe> holder = seqMilling.get();
            if (!AllRecipeTypes.shouldIgnoreInAutomation(holder)) {
                ItemStack resultItem = holder.value().getResultItem(registries);
                if (!filtering.isActive() || filtering.test(resultItem)) {
                    return RecipeApplier.applyRecipeOn(level, input.copy(), holder.value(), true);
                }
            }
        }

        // CRUSHING sequenced assembly: removed (static list below already handles crushing recipes)

        // Priority 2: Milling (石磨) + Crushing (粉碎论/粉碎轮) 双配方
        Predicate<RecipeHolder<? extends Recipe<?>>> typePredicate = holder -> {
            RecipeType<?> rt = holder.value().getType();
            if (rt == AllRecipeTypes.MILLING.getType()) return true;
            if (CRUSHING_TYPE != null && rt == CRUSHING_TYPE) return true;
            return false;
        };

        List<RecipeHolder<? extends Recipe<?>>> candidates =
            RecipeFinder.get(millingOrCrushingRecipesKey, level, typePredicate);

        // 优先级：石磨 > 粉碎；防止同一输入两种配方都匹配时优先粉碎
        List<RecipeHolder<? extends Recipe<?>>> millingFirst = new ArrayList<>();
        List<RecipeHolder<? extends Recipe<?>>> crushingRest = new ArrayList<>();
        for (RecipeHolder<? extends Recipe<?>> h : candidates) {
            if (h.value().getType() == AllRecipeTypes.MILLING.getType()) millingFirst.add(h);
            else crushingRest.add(h);
        }
        millingFirst.addAll(crushingRest);

        for (RecipeHolder<? extends Recipe<?>> holder : millingFirst) {
            Recipe<?> recipe = holder.value();

            if (AllRecipeTypes.shouldIgnoreInAutomation(holder)) continue;
            if (!RecipeConditions.firstIngredientMatches(input).test(holder)) continue;

            // 过滤器嵌入循环：首个不通过的不会挡住后续其它配方
            if (filtering.isActive()) {
                ItemStack rep = recipe.getResultItem(registries);
                if (rep.isEmpty() || !filtering.test(rep)) continue;
            }

            List<ItemStack> outputs = RecipeApplier.applyRecipeOn(level, input.copy(), recipe, true);
            if (!outputs.isEmpty()) return outputs;
        }

        // Priority 3: 兜底全量遍历所有配方（兼容第三方模组把 1:1 加工配方注册在非 Create RecipeType 下）
        // 纯函数过滤：只保留"单 ingredient 且 ingredient 与 input 匹配"的配方，其他一律跳过
        for (RecipeHolder<?> holder : level.getRecipeManager().getRecipes()) {
            Recipe<?> recipe = holder.value();
            // 已处理的 Create 主配方类型不再重复判定（避免重复出物）
            RecipeType<?> rt = recipe.getType();
            if (rt == AllRecipeTypes.MILLING.getType()) continue;
            if (CRUSHING_TYPE != null && rt == CRUSHING_TYPE) continue;
            // 不考虑多输入配方/序列合成专用类型（序列合成已在 Priority 1 处理）
            java.util.List<net.minecraft.world.item.crafting.Ingredient> ings = recipe.getIngredients();
            if (ings.size() != 1) continue;
            if (!ings.get(0).test(input)) continue;
            ItemStack rep = recipe.getResultItem(registries);
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
            level.playSound(null, worldPosition, CreateVillagerLabor.MILLSTONE_WORK_SOUND.get(),
                net.minecraft.sounds.SoundSource.BLOCKS, 1.0f, 1.0f);
        }
    }
}
