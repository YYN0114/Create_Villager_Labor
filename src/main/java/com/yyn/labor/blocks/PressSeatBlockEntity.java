package com.yyn.labor.blocks;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;

import com.simibubi.create.AllRecipeTypes;
import com.simibubi.create.content.kinetics.crafter.MechanicalCraftingRecipe;
import com.simibubi.create.content.kinetics.press.MechanicalPressBlockEntity;
import com.simibubi.create.content.kinetics.press.PressingRecipe;
import com.simibubi.create.content.processing.basin.BasinBlockEntity;
import com.simibubi.create.content.processing.basin.BasinRecipe;
import com.simibubi.create.content.processing.sequenced.SequencedAssemblyRecipe;
import com.simibubi.create.foundation.recipe.RecipeApplier;
import com.simibubi.create.foundation.recipe.RecipeConditions;
import com.simibubi.create.foundation.recipe.RecipeFinder;

import com.yyn.labor.CreateVillagerLabor;

import net.createmod.catnip.data.Iterate;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

import net.neoforged.neoforge.items.IItemHandler;

public class PressSeatBlockEntity extends WorkerSeatBlockEntity {

    // 多 ingredient 配方（Compacting / 自动压缩）的额外输入
    private final List<ItemStack> extraInputs = new ArrayList<>();
    private BlockPos sourceBasinPos;
    private int currentBatch = 1;
    // 标记当前处理的是工作盆配方（Compacting / 自动压缩），而非传送带 Pressing
    private boolean isBasinRecipe = false;
    // 自动压缩缓存结果
    private ItemStack cachedAutoCompressResult = ItemStack.EMPTY;

    private static final Object pressingRecipesKey = new Object();

    public PressSeatBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state, SeatMaterial material) {
        super(type, pos, state, material);
        maxCooldown = material.scaleCooldown(10);
    }

    // 冲压工位：先尝试工作盆（Compacting + 自动压缩），再尝试传送带/Depot（Pressing）
    @Override
    protected void acquireNextItem() {
        // 先尝试工作盆
        for (Direction dir : Iterate.horizontalDirections) {
            BlockPos adjacentPos = worldPosition.relative(dir);
            BlockState adjacentState = level.getBlockState(adjacentPos);
            if (isBasinBlock(adjacentState)) {
                if (tryTakeFromBasin(adjacentPos))
                    return;
            }
        }
        // 再尝试传送带/Depot（使用父类逻辑）
        for (Direction dir : Iterate.horizontalDirections) {
            BlockPos adjacentPos = worldPosition.relative(dir);
            BlockState adjacentState = level.getBlockState(adjacentPos);
            Block block = adjacentState.getBlock();
            if (block instanceof com.simibubi.create.content.kinetics.belt.BeltBlock) {
                if (tryTakeFromBelt(adjacentPos)) {
                    beltDirection = dir;
                    return;
                }
            } else if (block instanceof com.simibubi.create.content.logistics.depot.DepotBlock) {
                if (tryTakeFromDepot(adjacentPos))
                    return;
            }
        }
    }

    // 重写 tryTakeFromBasin：支持 Compacting 配方 + 自动压缩（2x2/3x3 合成）
    @Override
    @SuppressWarnings("rawtypes")
    protected boolean tryTakeFromBasin(BlockPos basinPos) {
        if (!(level.getBlockEntity(basinPos) instanceof BasinBlockEntity basinBE))
            return false;
        var inputInv = basinBE.getInputInventory();

        // === 1. Compacting 配方（AllRecipeTypes.COMPACTING）===
        List candidatesRaw = RecipeFinder.get(AllRecipeTypes.COMPACTING.getType(), level,
            r -> r instanceof RecipeHolder holder
                && holder.value().getType() == AllRecipeTypes.COMPACTING.getType());

        for (Object o : candidatesRaw) {
            if (!(o instanceof RecipeHolder holder)) continue;
            if (!(holder.value() instanceof BasinRecipe recipe)) continue;

            List<Ingredient> ings = recipe.getIngredients();
            if (ings.isEmpty()) continue;

            // 多 ingredient 匹配（与 Mixer 相同逻辑）
            List<Integer> matchedSlots = matchIngredientsToSlots(ings, inputInv);
            if (matchedSlots == null) continue;

            List<ItemStack> outputs = getBasinRecipeOutputs(recipe);
            if (outputs.isEmpty()) continue;
            if (filtering.isActive() && !outputs.stream().anyMatch(filtering::test)) continue;

            // 提取物品
            int batchSize = material.getBatchSize();
            int actualBatch = batchSize;
            for (int slot : matchedSlots) {
                actualBatch = Math.min(actualBatch, inputInv.getStackInSlot(slot).getCount());
            }
            if (actualBatch <= 0) continue;

            extraInputs.clear();
            for (int i = 0; i < matchedSlots.size(); i++) {
                ItemStack extracted = inputInv.extractItem(matchedSlots.get(i), actualBatch, false);
                if (i == 0) {
                    processingStack = extracted;
                } else {
                    extraInputs.add(extracted);
                }
            }
            basinBE.notifyChangeOfContents();

            sourceBasinPos = basinPos;
            currentBatch = actualBatch;
            isBasinRecipe = true;
            cachedAutoCompressResult = ItemStack.EMPTY;

            processingTimer = maxCooldown;
            updateHand();
            notifyUpdate();
            return true;
        }

        // === 2. 自动压缩（2x2/3x3 原版合成配方）===
        for (RecipeHolder<?> holder : level.getRecipeManager().getRecipes()) {
            Recipe<?> r = holder.value();
            if (!canCompress(r)) continue;
            if (AllRecipeTypes.shouldIgnoreInAutomation(holder)) continue;

            List<Ingredient> ings = r.getIngredients();
            int requiredCount = ings.size(); // 4 或 9
            Ingredient ing = ings.get(0);

            // 找到第一个匹配且有足够数量的 slot
            int matchedSlot = -1;
            for (int slot = 0; slot < inputInv.getSlots(); slot++) {
                ItemStack stack = inputInv.getStackInSlot(slot);
                if (stack.isEmpty()) continue;
                if (ing.test(stack) && stack.getCount() >= requiredCount) {
                    matchedSlot = slot;
                    break;
                }
            }
            if (matchedSlot == -1) continue;

            // 获取输出
            ItemStack result = r.getResultItem(level.registryAccess());
            if (result.isEmpty()) continue;
            if (filtering.isActive() && !filtering.test(result)) continue;

            // 计算批量
            int batchSize = material.getBatchSize();
            int available = inputInv.getStackInSlot(matchedSlot).getCount() / requiredCount;
            int actualBatch = Math.min(batchSize, available);
            if (actualBatch <= 0) continue;

            // 提取
            int toExtract = requiredCount * actualBatch;
            ItemStack extracted = inputInv.extractItem(matchedSlot, toExtract, false);
            processingStack = extracted;
            extraInputs.clear();
            basinBE.notifyChangeOfContents();

            sourceBasinPos = basinPos;
            currentBatch = actualBatch;
            isBasinRecipe = true;
            cachedAutoCompressResult = result.copyWithCount(result.getCount() * actualBatch);

            processingTimer = maxCooldown;
            updateHand();
            notifyUpdate();
            return true;
        }

        return false;
    }

    // 重写 finishProcessing：区分工作盆配方和传送带配方
    @Override
    protected void finishProcessing() {
        if (processingStack.isEmpty())
            return;

        if (isBasinRecipe) {
            finishBasinRecipe();
            return;
        }

        // 传送带/Depot 物品：使用父类逻辑（Pressing 配方）
        super.finishProcessing();
    }

    // 处理工作盆配方（Compacting 或自动压缩）
    @SuppressWarnings("rawtypes")
    private void finishBasinRecipe() {
        // 自动压缩：直接使用缓存结果
        if (!cachedAutoCompressResult.isEmpty()) {
            List<ItemStack> outputs = new ArrayList<>();
            outputs.add(cachedAutoCompressResult.copy());

            updateHand();
            for (ItemStack output : outputs) {
                if (!outputToAdjacent(output))
                    Block.popResource(level, worldPosition.above(), output);
            }

            clearBasinState();
            onProcessingComplete();
            return;
        }

        // Compacting 配方：重新匹配并获取输出（与 Mixer 相同逻辑）
        List candidatesRaw = RecipeFinder.get(AllRecipeTypes.COMPACTING.getType(), level,
            r -> r instanceof RecipeHolder holder
                && holder.value().getType() == AllRecipeTypes.COMPACTING.getType());

        List<Recipe<?>> candidates = new ArrayList<>();
        for (Object o : candidatesRaw) {
            if (o instanceof RecipeHolder holder) {
                candidates.add(holder.value());
            }
        }

        List<ItemStack> inputs = new ArrayList<>();
        inputs.add(processingStack);
        inputs.addAll(extraInputs);

        BasinRecipe matchedRecipe = null;
        for (Recipe<?> r : candidates) {
            if (!(r instanceof BasinRecipe recipe)) continue;

            List<Ingredient> ings = recipe.getIngredients();
            if (ings.isEmpty() || ings.size() != inputs.size()) continue;

            // 顺序无关匹配
            List<Ingredient> remaining = new ArrayList<>(ings);
            boolean allMatched = true;
            for (ItemStack input : inputs) {
                boolean found = false;
                for (int i = 0; i < remaining.size(); i++) {
                    if (remaining.get(i).test(input)) {
                        remaining.remove(i);
                        found = true;
                        break;
                    }
                }
                if (!found) {
                    allMatched = false;
                    break;
                }
            }

            if (allMatched && remaining.isEmpty()) {
                matchedRecipe = recipe;
                break;
            }
        }

        if (matchedRecipe == null) {
            Block.popResource(level, worldPosition.above(), processingStack.copy());
            for (ItemStack extra : extraInputs)
                Block.popResource(level, worldPosition.above(), extra.copy());
            clearBasinState();
            return;
        }

        List<ItemStack> outputs = getBasinRecipeOutputs(matchedRecipe);

        // 按批量倍增输出
        if (currentBatch > 1) {
            for (int i = 0; i < outputs.size(); i++) {
                ItemStack stack = outputs.get(i);
                if (!stack.isEmpty()) {
                    outputs.set(i, stack.copyWithCount(stack.getCount() * currentBatch));
                }
            }
        }

        updateHand();
        for (ItemStack output : outputs) {
            if (!outputToAdjacent(output))
                Block.popResource(level, worldPosition.above(), output);
        }

        clearBasinState();
        onProcessingComplete();
    }

    // 多 ingredient 匹配：检查 Basin 输入槽是否有所有 ingredient
    private List<Integer> matchIngredientsToSlots(List<Ingredient> ings, IItemHandler inputInv) {
        List<Integer> matchedSlots = new ArrayList<>();
        for (Ingredient ing : ings) {
            boolean found = false;
            for (int slot = 0; slot < inputInv.getSlots(); slot++) {
                if (matchedSlots.contains(slot)) continue;
                ItemStack stack = inputInv.getStackInSlot(slot);
                if (stack.isEmpty()) continue;
                if (ing.test(stack)) {
                    matchedSlots.add(slot);
                    found = true;
                    break;
                }
            }
            if (!found) return null;
        }
        return matchedSlots;
    }

    // 从 BasinRecipe 获取输出物品
    @SuppressWarnings("unchecked")
    private List<ItemStack> getBasinRecipeOutputs(BasinRecipe recipe) {
        List<ItemStack> outputs = new ArrayList<>();
        try {
            var rollable = recipe.getRollableResults();
            for (Object result : rollable) {
                try {
                    var stackMethod = result.getClass().getMethod("getStack");
                    ItemStack stack = (ItemStack) stackMethod.invoke(result);
                    if (stack != null && !stack.isEmpty())
                        outputs.add(stack.copy());
                } catch (Exception e) {
                    // getStack() 失败
                }
            }
        } catch (Exception e) {
            // getRollableResults() 失败
        }
        return outputs;
    }

    // 判断原版合成配方是否可被自动压缩（2x2/3x3 同原料合成）
    private static boolean canCompress(Recipe<?> recipe) {
        if (!(recipe instanceof CraftingRecipe)) return false;
        if (recipe instanceof MechanicalCraftingRecipe) return false;
        return MechanicalPressBlockEntity.canCompress(recipe);
    }

    private void clearBasinState() {
        processingStack = ItemStack.EMPTY;
        extraInputs.clear();
        sourceBasinPos = null;
        currentBatch = 1;
        isBasinRecipe = false;
        cachedAutoCompressResult = ItemStack.EMPTY;
        updateHand();
        outputCooldown = outputCooldownDuration;
        notifyUpdate();
    }

    // 传送带/Depot 物品的 Pressing 配方处理
    // 关键修复：Sequenced assembly 匹配但不通过过滤时，继续 fallback 到普通 Pressing 配方；
    // 普通配方遍历 + 过滤器嵌入循环，避免单个配方过滤不通过就阻塞全部。
    @Override
    protected List<ItemStack> processItem(ItemStack input) {
        HolderLookup.Provider registries = level.registryAccess();

        // Priority 1: Sequenced assembly recipes (e.g. sturdy plate)
        Optional<RecipeHolder<PressingRecipe>> seqRecipe = SequencedAssemblyRecipe.getRecipe(
            level, input, AllRecipeTypes.PRESSING.getType(), PressingRecipe.class);
        if (seqRecipe.isPresent()) {
            RecipeHolder<PressingRecipe> holder = seqRecipe.get();
            if (!AllRecipeTypes.shouldIgnoreInAutomation(holder)) {
                ItemStack rep = holder.value().getResultItem(registries);
                if (!filtering.isActive() || (!rep.isEmpty() && filtering.test(rep))) {
                    return RecipeApplier.applyRecipeOn(level, input.copy(), holder.value(), true);
                }
                // 过滤不通过：继续 fallback 到 standalone pressing，不要直接返回空
            }
        }

        // Priority 2: Standalone pressing recipes — use RecipeFinder + iteration
        Predicate<RecipeHolder<? extends Recipe<?>>> typePredicate = h ->
            h.value().getType() == AllRecipeTypes.PRESSING.getType();
        List<RecipeHolder<? extends Recipe<?>>> candidates =
            RecipeFinder.get(pressingRecipesKey, level, typePredicate);

        for (RecipeHolder<? extends Recipe<?>> holder : candidates) {
            Recipe<?> recipe = holder.value();
            if (AllRecipeTypes.shouldIgnoreInAutomation(holder)) continue;
            if (!RecipeConditions.firstIngredientMatches(input).test(holder)) continue;

            if (filtering.isActive()) {
                ItemStack rep = recipe.getResultItem(registries);
                if (rep.isEmpty() || !filtering.test(rep)) continue;
            }

            List<ItemStack> outputs = RecipeApplier.applyRecipeOn(level, input.copy(), recipe, true);
            if (!outputs.isEmpty()) return outputs;
        }

        return List.of();
    }

    @Override
    protected void onProcessingComplete() {
        if (level != null && !level.isClientSide) {
            level.playSound(null, worldPosition, CreateVillagerLabor.PRESS_WORK_SOUND.get(),
                net.minecraft.sounds.SoundSource.BLOCKS, 1.0f, 1.0f);
        }
    }
}
