package com.yyn.labor.blocks;

import java.util.List;
import java.util.Optional;

import com.simibubi.create.AllRecipeTypes;
import com.simibubi.create.content.kinetics.deployer.DeployerApplicationRecipe;
import com.simibubi.create.content.kinetics.deployer.ItemApplicationRecipe;
import com.simibubi.create.content.kinetics.deployer.ManualApplicationRecipe;
import com.simibubi.create.content.processing.sequenced.SequencedAssemblyRecipe;
import com.simibubi.create.foundation.recipe.RecipeApplier;

import com.yyn.labor.CreateVillagerLabor;

import net.createmod.catnip.data.Iterate;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemStackHandler;
import net.minecraftforge.items.wrapper.RecipeWrapper;

public class DeployerSeatBlockEntity extends WorkerSeatBlockEntity {

    protected ItemStack deployerHeldItem;
    private int heldItemIdleTicks;

    public DeployerSeatBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
        maxCooldown = 20;
        deployerHeldItem = ItemStack.EMPTY;
        heldItemIdleTicks = 0;
    }

    public static DeployerSeatBlockEntity create(BlockPos pos, BlockState state) {
        return new DeployerSeatBlockEntity(DeployerSeatBlock.BLOCK_ENTITY_TYPE, pos, state);
    }

    @Override
    protected void acquireNextItem() {
        if (deployerHeldItem.isEmpty()) {
            tryRefillHeldItem();
            if (deployerHeldItem.isEmpty())
                return;
        }

        for (Direction dir : Iterate.horizontalDirections) {
            BlockPos adjacentPos = worldPosition.relative(dir);
            BlockState state = level.getBlockState(adjacentPos);

            if (state.getBlock() instanceof com.simibubi.create.content.kinetics.belt.BeltBlock) {
                if (tryTakeFromBelt(adjacentPos)) {
                    beltDirection = dir;
                    return;
                }
            } else if (state.getBlock() instanceof com.simibubi.create.content.logistics.depot.DepotBlock) {
                if (tryTakeFromDepot(adjacentPos))
                    return;
            }
        }
    }

    @Override
    protected boolean itemCanBeProcessed(ItemStack stack) {
        if (deployerHeldItem.isEmpty())
            return false;
        ItemApplicationRecipe recipe = findMatchingRecipe(stack, deployerHeldItem);
        if (recipe == null)
            return false;
        if (filtering.isActive())
            return filtering.test(recipe.getResultItem(level.registryAccess()));
        return true;
    }

    @Override
    protected List<ItemStack> processItem(ItemStack input) {
        ItemApplicationRecipe recipe = findMatchingRecipe(input, deployerHeldItem);
        if (recipe == null) {
            CreateVillagerLabor.LOGGER.warn("[DeployerSeat] processItem failed - no recipe for belt={} held={} at {}",
                input.getItem(), deployerHeldItem.getItem(), worldPosition);
            return List.of();
        }

        List<ItemStack> outputs = RecipeApplier.applyRecipeOn(level, input.copy(), recipe, true);

        // Consume held item after outputs are confirmed, preventing item loss
        if (!outputs.isEmpty()) {
            if (deployerHeldItem.isDamageableItem()) {
                // Tool: consume durability instead of consuming the item
                deployerHeldItem.setDamageValue(deployerHeldItem.getDamageValue() + 1);
                if (deployerHeldItem.getDamageValue() >= deployerHeldItem.getMaxDamage()) {
                    deployerHeldItem = ItemStack.EMPTY;
                }
                updateHand();
            } else if (!recipe.shouldKeepHeldItem()) {
                // Non-tool: consume held item as before
                deployerHeldItem = ItemStack.EMPTY;
                updateHand();
            }
            // If shouldKeepHeldItem() is true and not a tool, keep the held item
        }

        return outputs;
    }

    @Override
    protected void processWork() {
        checkHeldItemTimeout();
        super.processWork();
        // During cooldown, try to refill held item early for rendering and next cycle
        if (deployerHeldItem.isEmpty() && outputCooldown > 0) {
            tryRefillHeldItem();
        }
    }

    @Override
    protected ItemStack getHandItem() {
        if (!processingStack.isEmpty())
            return processingStack;
        return deployerHeldItem.isEmpty() ? ItemStack.EMPTY : deployerHeldItem;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private ItemApplicationRecipe findMatchingRecipe(ItemStack beltItem, ItemStack heldItem) {
        ItemStackHandler inv = new ItemStackHandler(2);
        inv.setStackInSlot(0, beltItem);
        inv.setStackInSlot(1, heldItem);
        RecipeWrapper wrapper = new RecipeWrapper(inv);

        // Priority 1: Sequenced assembly recipes (e.g. precision mechanism)
        Optional<DeployerApplicationRecipe> seqRecipe = SequencedAssemblyRecipe.getRecipe(
            level, wrapper, AllRecipeTypes.DEPLOYING.getType(), DeployerApplicationRecipe.class);
        if (seqRecipe.isPresent()) {
            return seqRecipe.get();
        }

        // Priority 2: Standalone deploying recipes
        java.util.Collection<? extends Recipe<?>> deployingRecipes =
            level.getRecipeManager().getAllRecipesFor((RecipeType) AllRecipeTypes.DEPLOYING.getType());
        for (Recipe<?> recipe : deployingRecipes) {
            if (!AllRecipeTypes.CAN_BE_AUTOMATED.test(recipe))
                continue;
            if (!(recipe instanceof ItemApplicationRecipe iar))
                continue;
            if (iar.matches(wrapper, level))
                return iar;
        }

        // Priority 3: Manual application recipes (e.g. andesite casing)
        java.util.Collection<? extends Recipe<?>> manualRecipes =
            level.getRecipeManager().getAllRecipesFor((RecipeType) AllRecipeTypes.ITEM_APPLICATION.getType());
        for (Recipe<?> recipe : manualRecipes) {
            if (!AllRecipeTypes.CAN_BE_AUTOMATED.test(recipe))
                continue;
            if (!(recipe instanceof ManualApplicationRecipe mar))
                continue;
            if (mar.matches(wrapper, level))
                return ManualApplicationRecipe.asDeploying(mar);
        }

        return null;
    }

    private void tryRefillHeldItem() {
        for (Direction dir : Direction.values()) {
            BlockPos chestPos = worldPosition.relative(dir);
            // Exclude belts: they are processed via acquireNextItem, not as held-item source
            if (level.getBlockState(chestPos).getBlock() instanceof com.simibubi.create.content.kinetics.belt.BeltBlock)
                continue;
            BlockEntity be = level.getBlockEntity(chestPos);
            if (be == null)
                continue;
            LazyOptional<IItemHandler> cap = be.getCapability(ForgeCapabilities.ITEM_HANDLER, dir.getOpposite());
            IItemHandler handler = cap.orElse(null);
            if (handler == null)
                continue;

            for (int slot = 0; slot < handler.getSlots(); slot++) {
                ItemStack simulated = handler.extractItem(slot, 1, true);
                if (simulated.isEmpty())
                    continue;

                ItemStack actual = handler.extractItem(slot, 1, false);
                if (actual.isEmpty())
                    continue;

                deployerHeldItem = actual;
                heldItemIdleTicks = 0;
                updateHand();
                notifyUpdate();
                return;
            }
        }
    }

    private void checkHeldItemTimeout() {
        if (deployerHeldItem.isEmpty())
            return;

        if (processingStack.isEmpty() && processingTimer == 0 && outputCooldown == 0) {
            heldItemIdleTicks++;
            if (heldItemIdleTicks > 100) {
                returnHeldItemToChest();
                deployerHeldItem = ItemStack.EMPTY;
                heldItemIdleTicks = 0;
                updateHand();
                notifyUpdate();
            }
        } else {
            heldItemIdleTicks = 0;
        }
    }

    private void returnHeldItemToChest() {
        ItemStack stack = deployerHeldItem;
        if (stack.isEmpty())
            return;

        for (Direction dir : Direction.values()) {
            BlockPos chestPos = worldPosition.relative(dir);
            // Exclude belts when returning held item
            if (level.getBlockState(chestPos).getBlock() instanceof com.simibubi.create.content.kinetics.belt.BeltBlock)
                continue;
            BlockEntity be = level.getBlockEntity(chestPos);
            if (be == null)
                continue;
            LazyOptional<IItemHandler> cap = be.getCapability(ForgeCapabilities.ITEM_HANDLER, dir.getOpposite());
            IItemHandler handler = cap.orElse(null);
            if (handler == null)
                continue;

            for (int slot = 0; slot < handler.getSlots(); slot++) {
                stack = handler.insertItem(slot, stack, false);
                if (stack.isEmpty())
                    return;
            }
        }

        if (!stack.isEmpty())
            Block.popResource(level, worldPosition.above(), stack);
    }

    public ItemStack getDeployerHeldItem() {
        return deployerHeldItem;
    }

    @Override
    protected void onProcessingComplete() {
        if (level != null && !level.isClientSide) {
            level.playSound(null, worldPosition, CreateVillagerLabor.DEPLOYER_WORK_SOUND.get(),
                net.minecraft.sounds.SoundSource.BLOCKS, 1.0f, 1.0f);
        }
    }

    @Override
    public void write(CompoundTag compound, boolean clientPacket) {
        super.write(compound, clientPacket);
        compound.put("DeployerHeldItem", deployerHeldItem.save(new CompoundTag()));
    }

    @Override
    protected void read(CompoundTag compound, boolean clientPacket) {
        super.read(compound, clientPacket);
        deployerHeldItem = ItemStack.of(compound.getCompound("DeployerHeldItem"));
    }
}
