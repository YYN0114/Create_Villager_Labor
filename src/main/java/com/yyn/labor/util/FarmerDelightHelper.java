package com.yyn.labor.util;

import com.simibubi.create.content.processing.basin.BasinBlockEntity;
import com.simibubi.create.foundation.recipe.RecipeFinder;

import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.items.IItemHandler;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * 农夫乐事 (Farmer's Delight) 兼容助手。
 * 使用反射+标准 Recipe API 检测和处理 FD 烹饪配方，无需编译期依赖。
 */
public final class FarmerDelightHelper {

    private static final ResourceLocation FD_COOKING_ID = ResourceLocation.parse("farmersdelight:cooking");

    private FarmerDelightHelper() {}

    /**
     * 检查给定的 Recipe 是否为农夫乐事的烹饪配方。
     */
    @SuppressWarnings("unchecked")
    public static boolean isCookingRecipe(Recipe<?> recipe, RegistryAccess registryAccess) {
        ResourceLocation typeId = BuiltInRegistries.RECIPE_TYPE.getKey((RecipeType<?>) (Object) recipe.getType());
        return typeId != null && typeId.equals(FD_COOKING_ID);
    }

    /**
     * 从 Basin 输入槽中尝试匹配一个农夫乐事烹饪配方。
     * 成功时返回匹配信息，失败时返回 empty。
     */
    @SuppressWarnings("unchecked")
    public static Optional<FdCookingMatch> tryMatchCookingRecipe(Level level, BasinBlockEntity basinBE, IItemHandler inputInv) {
        if (level == null || level.isClientSide)
            return Optional.empty();

        RegistryAccess registryAccess = level.registryAccess();

        // 从配方管理器中获取所有配方
        List<RecipeHolder<Recipe<?>>> allRecipes = level.getRecipeManager().getRecipes()
            .stream()
            .filter(holder -> {
                Recipe<?> r = holder.value();
                return isCookingRecipe(r, registryAccess);
            })
            .map(holder -> (RecipeHolder<Recipe<?>>) (Object) holder)
            .collect(Collectors.toList());

        if (allRecipes.isEmpty())
            return Optional.empty();

        for (RecipeHolder<Recipe<?>> holder : allRecipes) {
            Recipe<?> recipe = holder.value();
            List<Ingredient> ings = recipe.getIngredients();
            if (ings.isEmpty())
                continue;

            // 检查 Basin 输入槽是否有所有 ingredient
            List<Integer> matchedSlots = new ArrayList<>();
            for (Ingredient ing : ings) {
                boolean found = false;
                for (int slot = 0; slot < inputInv.getSlots(); slot++) {
                    if (matchedSlots.contains(slot))
                        continue;
                    ItemStack stack = inputInv.getStackInSlot(slot);
                    if (stack.isEmpty())
                        continue;
                    if (ing.test(stack)) {
                        matchedSlots.add(slot);
                        found = true;
                        break;
                    }
                }
                if (!found)
                    break;
            }

            if (matchedSlots.size() != ings.size())
                continue;

            // 计算实际批量
            int maxBatch = Integer.MAX_VALUE;
            for (int slot : matchedSlots) {
                ItemStack stack = inputInv.getStackInSlot(slot);
                maxBatch = Math.min(maxBatch, stack.getCount());
            }
            if (maxBatch <= 0)
                continue;

            return Optional.of(new FdCookingMatch(recipe, matchedSlots, maxBatch, holder));
        }

        return Optional.empty();
    }

    /**
     * 获取配方结果（物品输出）。
     * 使用反射尝试获取容器输出（如碗），如果失败则只返回主产物。
     */
    public static List<ItemStack> getRecipeOutputs(Recipe<?> recipe, RegistryAccess registryAccess) {
        List<ItemStack> outputs = new ArrayList<>();

        // 主产物
        ItemStack result = recipe.getResultItem(registryAccess);
        if (!result.isEmpty())
            outputs.add(result.copy());

        // 尝试通过反射获取容器输出（getOutputContainer）
        try {
            var method = recipe.getClass().getMethod("getOutputContainer");
            Object containerObj = method.invoke(recipe);
            if (containerObj instanceof ItemStack container && !container.isEmpty()) {
                outputs.add(container.copy());
            }
        } catch (Exception ignored) {
            // FD 不可用或无该方法
        }

        return outputs;
    }

    /**
     * 农夫乐事烹饪配方匹配结果。
     */
    public static class FdCookingMatch {
        public final Recipe<?> recipe;
        public final List<Integer> matchedSlots;
        public final int batchSize;
        public final RecipeHolder<?> holder;

        public FdCookingMatch(Recipe<?> recipe, List<Integer> matchedSlots, int batchSize, RecipeHolder<?> holder) {
            this.recipe = recipe;
            this.matchedSlots = matchedSlots;
            this.batchSize = batchSize;
            this.holder = holder;
        }
    }
}
