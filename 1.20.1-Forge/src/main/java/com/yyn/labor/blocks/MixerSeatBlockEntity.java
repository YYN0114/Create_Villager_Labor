package com.yyn.labor.blocks;

import java.util.ArrayList;
import java.util.List;

import com.simibubi.create.AllRecipeTypes;
import com.simibubi.create.content.processing.basin.BasinBlockEntity;
import com.simibubi.create.content.processing.basin.BasinRecipe;
import com.simibubi.create.content.processing.recipe.HeatCondition;
import com.simibubi.create.foundation.recipe.RecipeFinder;

import net.createmod.catnip.data.Iterate;
import net.createmod.catnip.math.VecHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Axis;
import net.minecraft.core.particles.ItemParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler;

public class MixerSeatBlockEntity extends WorkerSeatBlockEntity {

    // 多 ingredient 配方的额外输入（第 1 个存于父类 processingStack）
    private final List<ItemStack> extraInputs = new ArrayList<>();
    // 记录物品来源的 Basin 位置，用于 finishProcessing 时处理流体
    private BlockPos sourceBasinPos;
    // 记录当前批处理的实际数量（用于倍增输出和流体消耗）
    private int currentBatch = 1;

    public MixerSeatBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state, SeatMaterial material) {
        super(type, pos, state, material);
        maxCooldown = material.scaleCooldown(10);
    }

    // 搅拌工位仅从工作盆（Basin）取物品和输出物品，不检测传送带/Depot
    @Override
    protected void acquireNextItem() {
        for (Direction dir : Iterate.horizontalDirections) {
            BlockPos adjacentPos = worldPosition.relative(dir);
            BlockState adjacentState = level.getBlockState(adjacentPos);
            if (isBasinBlock(adjacentState)) {
                if (tryTakeFromBasin(adjacentPos))
                    return;
            }
        }
    }

    // 重写 tryTakeFromBasin：支持多 ingredient + 流体配方
    @Override
    protected boolean tryTakeFromBasin(BlockPos basinPos) {
        if (!(level.getBlockEntity(basinPos) instanceof BasinBlockEntity basinBE))
            return false;
        var inputInv = basinBE.getInputInventory();

        // 获取 Basin 流体处理器（用于检查流体配方）
        BlockEntity basinBE_raw = level.getBlockEntity(basinPos);
        IFluidHandler fluidHandler = null;
        if (basinBE_raw != null) {
            LazyOptional<IFluidHandler> cap = basinBE_raw.getCapability(ForgeCapabilities.FLUID_HANDLER);
            fluidHandler = cap.orElse(null);
        }

        List<Recipe<?>> candidates = RecipeFinder.get(
            AllRecipeTypes.MIXING.getType(), level,
            r -> r.getType() == AllRecipeTypes.MIXING.getType()
        );

        for (Recipe<?> r : candidates) {
            if (!(r instanceof BasinRecipe recipe))
                continue;

            // 搅拌工位无热源：跳过需要加热的配方
            if (recipe.getRequiredHeat() != HeatCondition.NONE)
                continue;

            // 不再跳过流体配方：允许自动酿造等流体配方

            List<Ingredient> ings = recipe.getIngredients();
            if (ings.isEmpty())
                continue;

            // 检查 Basin 输入槽是否有所有 ingredient（每个 ingredient 对应不同 slot）
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

            // 检查 Basin 是否有足够的流体（如果有流体配方）
            if (!hasRequiredFluids(recipe, fluidHandler))
                continue;

            // 获取配方输出（直接从配方获取，不使用 applyRecipeOn）
            List<ItemStack> outputs = getRecipeOutputs(recipe);
            if (outputs.isEmpty())
                continue;
            if (filtering.isActive() && !outputs.stream().anyMatch(filtering::test))
                continue;

            // 从 Basin 取出所有 ingredient：第 1 个存入 processingStack，其余存入 extraInputs
            // 按材质 batchSize 批量提取（每个 ingredient 提取相同数量）
            int batchSize = material.getBatchSize();

            // 先计算实际可提取数量（取所有匹配 slot 的最小值，确保各 ingredient 数量一致）
            int actualBatch = batchSize;
            for (int slot : matchedSlots) {
                ItemStack stack = inputInv.getStackInSlot(slot);
                actualBatch = Math.min(actualBatch, stack.getCount());
            }
            if (actualBatch <= 0)
                continue;

            extraInputs.clear();
            for (int i = 0; i < matchedSlots.size(); i++) {
                int slot = matchedSlots.get(i);
                ItemStack extracted = inputInv.extractItem(slot, actualBatch, false);
                if (i == 0) {
                    processingStack = extracted;
                } else {
                    extraInputs.add(extracted);
                }
            }
            basinBE.notifyChangeOfContents();

            // 记录 Basin 位置和实际批量，用于 finishProcessing 时处理流体和倍增输出
            sourceBasinPos = basinPos;
            currentBatch = actualBatch;

            processingTimer = maxCooldown;
            updateHand();
            notifyUpdate();
            return true;
        }
        return false;
    }

    // 检查 Basin 是否有配方所需的所有流体
    private boolean hasRequiredFluids(BasinRecipe recipe, IFluidHandler fluidHandler) {
        var fluidIngredients = recipe.getFluidIngredients();
        if (fluidIngredients.isEmpty())
            return true;
        if (fluidHandler == null)
            return false;

        for (var fi : fluidIngredients) {
            boolean found = false;
            for (int tank = 0; tank < fluidHandler.getTanks(); tank++) {
                FluidStack tankFluid = fluidHandler.getFluidInTank(tank);
                if (fi.test(tankFluid)) {
                    found = true;
                    break;
                }
            }
            if (!found)
                return false;
        }
        return true;
    }

    // 直接从配方获取输出物品（不使用 applyRecipeOn，避免多 ingredient 匹配失败）
    private List<ItemStack> getRecipeOutputs(BasinRecipe recipe) {
        List<ItemStack> outputs = new ArrayList<>();
        try {
            // 尝试使用 rollResults（支持概率滚动）
            var rollable = recipe.getRollableResults();
            for (Object result : rollable) {
                try {
                    var stackMethod = result.getClass().getMethod("getStack");
                    ItemStack stack = (ItemStack) stackMethod.invoke(result);
                    if (stack != null && !stack.isEmpty())
                        outputs.add(stack.copy());
                } catch (Exception e) {
                    // 如果 getStack() 失败，尝试其他方法
                }
            }
        } catch (Exception e) {
            // 如果 getRollableResults() 失败，返回空列表
        }
        return outputs;
    }

    // 重写 finishProcessing：重新匹配配方并输出到 Basin 输出槽
    @Override
    protected void finishProcessing() {
        if (processingStack.isEmpty())
            return;

        List<Recipe<?>> candidates = RecipeFinder.get(
            AllRecipeTypes.MIXING.getType(), level,
            r -> r.getType() == AllRecipeTypes.MIXING.getType()
        );

        // 收集所有输入物品（processingStack + extraInputs）
        List<ItemStack> inputs = new ArrayList<>();
        inputs.add(processingStack);
        inputs.addAll(extraInputs);

        BasinRecipe matchedRecipe = null;
        for (Recipe<?> r : candidates) {
            if (!(r instanceof BasinRecipe recipe))
                continue;
            if (recipe.getRequiredHeat() != HeatCondition.NONE)
                continue;

            List<Ingredient> ings = recipe.getIngredients();
            if (ings.isEmpty() || ings.size() != inputs.size())
                continue;

            // 顺序无关匹配：每个输入必须匹配某个 ingredient，且每个 ingredient 只能匹配一次
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
            // 处理失败：把所有输入物品弹出，防止丢失
            Block.popResource(level, worldPosition.above(), processingStack.copy());
            for (ItemStack extra : extraInputs)
                Block.popResource(level, worldPosition.above(), extra.copy());
            processingStack = ItemStack.EMPTY;
            extraInputs.clear();
            sourceBasinPos = null;
            currentBatch = 1;
            updateHand();
            outputCooldown = outputCooldownDuration;
            notifyUpdate();
            return;
        }

        // 获取输出
        List<ItemStack> outputs = getRecipeOutputs(matchedRecipe);

        // 按批量倍增输出
        if (currentBatch > 1) {
            for (int i = 0; i < outputs.size(); i++) {
                ItemStack stack = outputs.get(i);
                if (!stack.isEmpty()) {
                    outputs.set(i, stack.copyWithCount(stack.getCount() * currentBatch));
                }
            }
        }

        // 处理流体：消耗输入流体，产生输出流体
        if (sourceBasinPos != null) {
            processFluids(matchedRecipe, sourceBasinPos, currentBatch);
        }

        updateHand();

        // 输出到 Basin 输出槽（父类 outputToAdjacent 已修复为插入输出槽）
        for (ItemStack output : outputs) {
            if (!outputToAdjacent(output))
                Block.popResource(level, worldPosition.above(), output);
        }

        processingStack = ItemStack.EMPTY;
        extraInputs.clear();
        sourceBasinPos = null;
        currentBatch = 1;
        outputCooldown = outputCooldownDuration;
        notifyUpdate();

        onProcessingComplete();
    }

    // 消耗 Basin 中的流体输入，产生流体输出
    private void processFluids(BasinRecipe recipe, BlockPos basinPos, int batch) {
        BlockEntity be = level.getBlockEntity(basinPos);
        if (be == null)
            return;
        LazyOptional<IFluidHandler> cap = be.getCapability(ForgeCapabilities.FLUID_HANDLER);
        IFluidHandler fluidHandler = cap.orElse(null);
        if (fluidHandler == null)
            return;

        // 消耗流体输入（按 batch 倍增）
        for (var fi : recipe.getFluidIngredients()) {
            for (int tank = 0; tank < fluidHandler.getTanks(); tank++) {
                FluidStack tankFluid = fluidHandler.getFluidInTank(tank);
                if (fi.test(tankFluid)) {
                    int amount = 1000;
                    try {
                        var amountMethod = fi.getClass().getMethod("getRequiredAmount");
                        amount = (int) amountMethod.invoke(fi);
                    } catch (Exception e) {
                        // 使用默认值
                    }
                    amount *= batch;
                    fluidHandler.drain(new FluidStack(tankFluid.getFluid(), amount),
                        IFluidHandler.FluidAction.EXECUTE);
                    break;
                }
            }
        }

        // 产生流体输出（按 batch 倍增）
        try {
            var fluidResults = recipe.getFluidResults();
            for (Object fs : fluidResults) {
                if (fs instanceof FluidStack fluidStack) {
                    FluidStack copy = fluidStack.copy();
                    copy.setAmount(copy.getAmount() * batch);
                    fluidHandler.fill(copy, IFluidHandler.FluidAction.EXECUTE);
                }
            }
        } catch (Exception e) {
            // 流体输出处理失败不影响物品输出
        }
    }

    // processItem 不再使用（搅拌工位用 tryTakeFromBasin + finishProcessing 处理多输入）
    @Override
    protected List<ItemStack> processItem(ItemStack input) {
        return List.of();
    }

    // 客户端粒子效果：模仿 Create 原版搅拌器的物品碎屑粒子
    @Override
    public void tick() {
        super.tick();
        if (level == null || !level.isClientSide)
            return;
        if (processingStack.isEmpty() || processingTimer <= 0)
            return;
        // 每 3 tick 产生一组粒子，避免每 tick 刷屏
        if (processingTimer % 3 != 0)
            return;

        ItemParticleOption data = new ItemParticleOption(ParticleTypes.ITEM, processingStack);
        // 圆环位置（半径 0.25），与原版搅拌器一致
        float angle = level.random.nextFloat() * 360;
        Vec3 offset = VecHelper.rotate(new Vec3(0, 0, 0.25f), angle, Axis.Y);
        Vec3 center = offset.add(VecHelper.getCenterOf(worldPosition));
        // 速度方向跟随圆环切向，向上 0.15
        Vec3 target = VecHelper.rotate(offset, 25, Axis.Y).add(0, 0.15f, 0);
        // y +0.4 大约是村民坐姿手部高度
        level.addParticle(data,
            center.x, center.y + 0.4f, center.z,
            target.x, target.y, target.z);
    }
}
