package com.yyn.labor;

import java.util.List;
import java.util.UUID;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.Item.TooltipContext;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import com.simibubi.create.content.contraptions.actors.seat.SeatEntity;
import com.simibubi.create.content.contraptions.actors.seat.SeatBlock;

public class VillagerBinderItem extends Item {

    private static final String TAG_VILLAGER_UUID = "BoundVillagerUUID";

    public VillagerBinderItem() {
        super(new Properties().stacksTo(1));
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltipComponents, TooltipFlag tooltipFlag) {
        super.appendHoverText(stack, context, tooltipComponents, tooltipFlag);

        CompoundTag tag = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        if (tag.hasUUID(TAG_VILLAGER_UUID)) {
            UUID uuid = tag.getUUID(TAG_VILLAGER_UUID);
            tooltipComponents.add(Component.translatable("item.create_labor.villager_binder.bound").withStyle(ChatFormatting.GREEN));
            tooltipComponents.add(Component.literal(uuid.toString().substring(0, 8) + "...").withStyle(ChatFormatting.DARK_GREEN));
        } else {
            tooltipComponents.add(Component.translatable("item.create_labor.villager_binder.desc").withStyle(ChatFormatting.GRAY));
        }
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        Player player = context.getPlayer();
        InteractionHand hand = context.getHand();

        if (player == null)
            return InteractionResult.PASS;

        if (level.isClientSide)
            return InteractionResult.PASS;

        if (isWorkerSeat(level.getBlockState(pos).getBlock())) {
            if (player.isShiftKeyDown()) {
                return placeVillagerOnSeat(level, pos, player);
            } else {
                return ejectVillagerFromSeat(level, pos);
            }
        }

        if (player.isShiftKeyDown()) {
            Villager nearest = findNearestVillager(level, pos);
            if (nearest != null) {
                bindVillager(player.getItemInHand(hand), nearest, player, hand);
                return InteractionResult.SUCCESS;
            }
        }

        return InteractionResult.PASS;
    }

    private Villager findNearestVillager(Level level, BlockPos blockPos) {
        AABB searchBox = new AABB(blockPos).inflate(2.0);
        Villager closest = null;
        double closestDist = Double.MAX_VALUE;
        Vec3 center = Vec3.atCenterOf(blockPos);
        for (Villager villager : level.getEntitiesOfClass(Villager.class, searchBox)) {
            double dist = villager.distanceToSqr(center);
            if (dist < closestDist) {
                closestDist = dist;
                closest = villager;
            }
        }
        return closest;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (player.isShiftKeyDown()) {
            if (level.isClientSide)
                return InteractionResultHolder.success(stack);
            clearBoundVillager(stack, player, hand);
            return InteractionResultHolder.success(stack);
        }
        return InteractionResultHolder.pass(stack);
    }

    @Override
    public InteractionResult interactLivingEntity(ItemStack stack, Player player, LivingEntity entity, InteractionHand hand) {
        if (!(entity instanceof Villager villager)) {
            return InteractionResult.PASS;
        }

        if (player.isShiftKeyDown()) {
            if (player.level().isClientSide) {
                return InteractionResult.SUCCESS;
            }

            bindVillager(stack, villager, player, hand);
            return InteractionResult.SUCCESS;
        }
        return InteractionResult.PASS;
    }

    private void bindVillager(ItemStack stack, Villager villager, Player player, InteractionHand hand) {
        ItemStack copy = stack.copy();
        CompoundTag tag = copy.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        tag.putUUID(TAG_VILLAGER_UUID, villager.getUUID());
        copy.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
        player.setItemInHand(hand, copy);

        player.sendSystemMessage(Component.translatable("item.create_labor.villager_binder.bind_success", villager.getDisplayName()).withStyle(ChatFormatting.GREEN));
    }

    private void clearBoundVillager(ItemStack stack, Player player, InteractionHand hand) {
        if (!stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag().hasUUID(TAG_VILLAGER_UUID))
            return;

        ItemStack copy = stack.copy();
        CompoundTag tag = copy.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        tag.remove(TAG_VILLAGER_UUID);
        if (tag.isEmpty()) {
            copy.remove(DataComponents.CUSTOM_DATA);
        } else {
            copy.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
        }
        player.setItemInHand(hand, copy);

        player.sendSystemMessage(Component.translatable("item.create_labor.villager_binder.clear").withStyle(ChatFormatting.YELLOW));
    }

    private UUID getBoundVillagerUUID(ItemStack stack) {
        CompoundTag tag = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        if (tag.hasUUID(TAG_VILLAGER_UUID)) {
            return tag.getUUID(TAG_VILLAGER_UUID);
        }
        return null;
    }

    private InteractionResult placeVillagerOnSeat(Level level, BlockPos pos, Player player) {
        ItemStack stack = player.getMainHandItem();
        if (stack.getItem() != this) {
            stack = player.getOffhandItem();
            if (stack.getItem() != this) {
                return InteractionResult.PASS;
            }
        }

        UUID villagerUUID = getBoundVillagerUUID(stack);
        if (villagerUUID == null) {
            player.sendSystemMessage(Component.translatable("item.create_labor.villager_binder.no_villager").withStyle(ChatFormatting.RED));
            return InteractionResult.PASS;
        }

        if (SeatBlock.isSeatOccupied(level, pos)) {
            player.sendSystemMessage(Component.translatable("item.create_labor.villager_binder.seat_occupied").withStyle(ChatFormatting.RED));
            return InteractionResult.PASS;
        }

        Villager villager = findVillagerByUUID(level, villagerUUID);
        if (villager == null) {
            player.sendSystemMessage(Component.translatable("item.create_labor.villager_binder.villager_not_found").withStyle(ChatFormatting.RED));
            return InteractionResult.PASS;
        }

        villager.stopRiding();
        villager.setPos(pos.getX() + 0.5, pos.getY() + 0.1, pos.getZ() + 0.5);

        if (!player.isCreative()) {
            InteractionHand hand;
            if (player.getMainHandItem().getItem() == this) {
                hand = InteractionHand.MAIN_HAND;
            } else {
                hand = InteractionHand.OFF_HAND;
            }
            clearBoundVillager(player.getItemInHand(hand), player, hand);
        }

        SeatBlock.sitDown(level, pos, villager);
        player.sendSystemMessage(Component.translatable("item.create_labor.villager_binder.place_success").withStyle(ChatFormatting.GREEN));

        return InteractionResult.SUCCESS;
    }

    private Villager findVillagerByUUID(Level level, UUID uuid) {
        if (level instanceof ServerLevel serverLevel) {
            Entity entity = serverLevel.getEntity(uuid);
            if (entity instanceof Villager villager) {
                return villager;
            }
        }
        return null;
    }

    private boolean isWorkerSeat(Block block) {
        return block instanceof com.yyn.labor.blocks.PressSeatBlock ||
               block instanceof com.yyn.labor.blocks.MixerSeatBlock ||
               block instanceof com.yyn.labor.blocks.SawSeatBlock ||
               block instanceof com.yyn.labor.blocks.MillstoneSeatBlock ||
               block instanceof com.yyn.labor.blocks.DeployerSeatBlock;
    }

    private InteractionResult ejectVillagerFromSeat(Level level, BlockPos pos) {
        AABB searchBox = new AABB(pos);

        for (SeatEntity seatEntity : level.getEntitiesOfClass(SeatEntity.class, searchBox)) {
            if (seatEntity.isVehicle()) {
                for (Entity passenger : seatEntity.getPassengers()) {
                    if (passenger instanceof Villager villager) {
                        villager.stopRiding();
                        villager.setPos(pos.getX() + 0.5, pos.getY() + 0.1, pos.getZ() + 0.5);
                        return InteractionResult.SUCCESS;
                    }
                }
            }
        }

        for (Villager villager : level.getEntitiesOfClass(Villager.class, searchBox.inflate(0.5))) {
            if (villager.blockPosition().equals(pos)) {
                villager.setPos(pos.getX() + 0.5, pos.getY() + 0.1, pos.getZ() + 0.5);
                return InteractionResult.SUCCESS;
            }
        }

        return InteractionResult.PASS;
    }
}
