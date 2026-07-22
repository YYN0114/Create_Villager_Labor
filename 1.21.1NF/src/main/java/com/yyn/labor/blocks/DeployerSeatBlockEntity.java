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
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemStackHandler;
import net.neoforged.neoforge.items.wrapper.RecipeWrapper;

public class DeployerSeatBlockEntity extends WorkerSeatBlockEntity {

    protected ItemStack deployerHeldItem;
    private int heldItemIdleTicks;

    public DeployerSeatBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state, SeatMaterial material) {
        super(type, pos, state, material);
        maxCooldown = material.scaleCooldown(20);
        deployerHeldItem = ItemStack.EMPTY;
        heldItemIdleTicks = 0;
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
        Optional<RecipeHolder<DeployerApplicationRecipe>> seqRecipe = SequencedAssemblyRecipe.getRecipe(
            level, wrapper, AllRecipeTypes.DEPLOYING.getType(), DeployerApplicationRecipe.class);
        if (seqRecipe.isPresent()) {
            return seqRecipe.get().value();
        }

        // Priority 2: Standalone deploying recipes
        java.util.Collection<RecipeHolder<?>> deployingRecipes =
            (java.util.Collection) level.getRecipeManager()
                .getAllRecipesFor((RecipeType) AllRecipeTypes.DEPLOYING.getType());
        for (RecipeHolder<?> holder : deployingRecipes) {
            if (!AllRecipeTypes.CAN_BE_AUTOMATED.test(holder))
                continue;
            if (!(holder.value() instanceof ItemApplicationRecipe iar))
                continue;
            if (iar.matches(wrapper, level))
                return iar;
        }

        // Priority 3: Manual application recipes (e.g. andesite casing)
        java.util.Collection<RecipeHolder<?>> manualRecipes =
            (java.util.Collection) level.getRecipeManager()
                .getAllRecipesFor((RecipeType) AllRecipeTypes.ITEM_APPLICATION.getType());
        for (RecipeHolder<?> holder : manualRecipes) {
            if (!AllRecipeTypes.CAN_BE_AUTOMATED.test(holder))
                continue;
            if (!(holder.value() instanceof ManualApplicationRecipe mar))
                continue;
            if (mar.matches(wrapper, level))
                return ManualApplicationRecipe.asDeploying(holder).value();
        }

        return null;
    }

    private void tryRefillHeldItem() {
        for (Direction dir : Direction.values()) {
            BlockPos chestPos = worldPosition.relative(dir);
            // Exclude belts: they are processed via acquireNextItem, not as held-item source
            if (level.getBlockState(chestPos).getBlock() instanceof com.simibubi.create.content.kinetics.belt.BeltBlock)
                continue;
            IItemHandler handler = level.getCapability(Capabilities.ItemHandler.BLOCK, chestPos, dir.getOpposite());
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
            IItemHandler handler = level.getCapability(Capabilities.ItemHandler.BLOCK, chestPos, dir.getOpposite());
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
    protected void write(CompoundTag compound, HolderLookup.Provider registries, boolean clientPacket) {
        super.write(compound, registries, clientPacket);
        compound.put("DeployerHeldItem", deployerHeldItem.saveOptional(registries));
    }

    @Override
    protected void read(CompoundTag compound, HolderLookup.Provider registries, boolean clientPacket) {
        super.read(compound, registries, clientPacket);
        deployerHeldItem = ItemStack.parseOptional(registries, compound.getCompound("DeployerHeldItem"));
    }
}
