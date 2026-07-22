package com.yyn.labor;

import java.util.List;
import java.util.UUID;

import com.yyn.labor.util.WorkerUtil;
import com.simibubi.create.content.contraptions.actors.seat.SeatEntity;
import com.simibubi.create.content.contraptions.actors.seat.SeatBlock;

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

public class VillagerBinderItem extends Item {

    private static final String TAG_WORKER_UUID = "BoundVillagerUUID";

    public VillagerBinderItem() {
        super(new Properties().stacksTo(1));
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltipComponents, TooltipFlag tooltipFlag) {
        super.appendHoverText(stack, context, tooltipComponents, tooltipFlag);

        CompoundTag tag = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        if (tag.hasUUID(TAG_WORKER_UUID)) {
            UUID uuid = tag.getUUID(TAG_WORKER_UUID);
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
                return placeWorkerOnSeat(level, pos, player);
            } else {
                return ejectWorkerFromSeat(level, pos);
            }
        }

        if (player.isShiftKeyDown()) {
            LivingEntity nearest = findNearestWorker(level, pos);
            if (nearest != null) {
                bindWorker(player.getItemInHand(hand), nearest, player, hand);
                return InteractionResult.SUCCESS;
            }
        }

        return InteractionResult.PASS;
    }

    // 查找附近的工人实体（支持所有 mod 兼容类型）
    private LivingEntity findNearestWorker(Level level, BlockPos blockPos) {
        AABB searchBox = new AABB(blockPos).inflate(2.0);
        LivingEntity closest = null;
        double closestDist = Double.MAX_VALUE;
        Vec3 center = Vec3.atCenterOf(blockPos);
        for (LivingEntity entity : level.getEntitiesOfClass(LivingEntity.class, searchBox)) {
            if (!WorkerUtil.isNonPlayerWorker(entity))
                continue;
            double dist = entity.distanceToSqr(center);
            if (dist < closestDist) {
                closestDist = dist;
                closest = entity;
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
            clearBoundWorker(stack, player, hand);
            return InteractionResultHolder.success(stack);
        }
        return InteractionResultHolder.pass(stack);
    }

    @Override
    public InteractionResult interactLivingEntity(ItemStack stack, Player player, LivingEntity entity, InteractionHand hand) {
        if (!WorkerUtil.isNonPlayerWorker(entity)) {
            return InteractionResult.PASS;
        }

        if (player.isShiftKeyDown()) {
            if (player.level().isClientSide) {
                return InteractionResult.SUCCESS;
            }

            bindWorker(stack, entity, player, hand);
            return InteractionResult.SUCCESS;
        }
        return InteractionResult.PASS;
    }

    // 绑定工人（支持任意 LivingEntity）
    private void bindWorker(ItemStack stack, LivingEntity worker, Player player, InteractionHand hand) {
        ItemStack copy = stack.copy();
        CompoundTag tag = copy.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        tag.putUUID(TAG_WORKER_UUID, worker.getUUID());
        copy.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
        player.setItemInHand(hand, copy);

        player.sendSystemMessage(Component.translatable("item.create_labor.villager_binder.bind_success", worker.getDisplayName()).withStyle(ChatFormatting.GREEN));
    }

    private void clearBoundWorker(ItemStack stack, Player player, InteractionHand hand) {
        if (!stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag().hasUUID(TAG_WORKER_UUID))
            return;

        ItemStack copy = stack.copy();
        CompoundTag tag = copy.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        tag.remove(TAG_WORKER_UUID);
        if (tag.isEmpty()) {
            copy.remove(DataComponents.CUSTOM_DATA);
        } else {
            copy.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
        }
        player.setItemInHand(hand, copy);

        player.sendSystemMessage(Component.translatable("item.create_labor.villager_binder.clear").withStyle(ChatFormatting.YELLOW));
    }

    private UUID getBoundWorkerUUID(ItemStack stack) {
        CompoundTag tag = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        if (tag.hasUUID(TAG_WORKER_UUID)) {
            return tag.getUUID(TAG_WORKER_UUID);
        }
        return null;
    }

    private InteractionResult placeWorkerOnSeat(Level level, BlockPos pos, Player player) {
        ItemStack stack = player.getMainHandItem();
        if (stack.getItem() != this) {
            stack = player.getOffhandItem();
            if (stack.getItem() != this) {
                return InteractionResult.PASS;
            }
        }

        UUID workerUUID = getBoundWorkerUUID(stack);
        if (workerUUID == null) {
            player.sendSystemMessage(Component.translatable("item.create_labor.villager_binder.no_villager").withStyle(ChatFormatting.RED));
            return InteractionResult.PASS;
        }

        if (SeatBlock.isSeatOccupied(level, pos)) {
            player.sendSystemMessage(Component.translatable("item.create_labor.villager_binder.seat_occupied").withStyle(ChatFormatting.RED));
            return InteractionResult.PASS;
        }

        LivingEntity worker = findWorkerByUUID(level, workerUUID);
        if (worker == null) {
            player.sendSystemMessage(Component.translatable("item.create_labor.villager_binder.villager_not_found").withStyle(ChatFormatting.RED));
            return InteractionResult.PASS;
        }

        worker.stopRiding();
        worker.setPos(pos.getX() + 0.5, pos.getY() + 0.1, pos.getZ() + 0.5);

        if (!player.isCreative()) {
            InteractionHand hand;
            if (player.getMainHandItem().getItem() == this) {
                hand = InteractionHand.MAIN_HAND;
            } else {
                hand = InteractionHand.OFF_HAND;
            }
            clearBoundWorker(player.getItemInHand(hand), player, hand);
        }

        SeatBlock.sitDown(level, pos, worker);
        player.sendSystemMessage(Component.translatable("item.create_labor.villager_binder.place_success").withStyle(ChatFormatting.GREEN));

        return InteractionResult.SUCCESS;
    }

    // 通过 UUID 查找工人实体（支持任意类型，不限于 Villager）
    private LivingEntity findWorkerByUUID(Level level, UUID uuid) {
        if (level instanceof ServerLevel serverLevel) {
            Entity entity = serverLevel.getEntity(uuid);
            if (entity instanceof LivingEntity living && WorkerUtil.isNonPlayerWorker(living)) {
                return living;
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

    // 弹出工位上的工人（支持所有 mod 兼容类型）
    private InteractionResult ejectWorkerFromSeat(Level level, BlockPos pos) {
        AABB searchBox = new AABB(pos);

        for (SeatEntity seatEntity : level.getEntitiesOfClass(SeatEntity.class, searchBox)) {
            if (seatEntity.isVehicle()) {
                for (Entity passenger : seatEntity.getPassengers()) {
                    if (WorkerUtil.isNonPlayerWorker(passenger)) {
                        passenger.stopRiding();
                        passenger.setPos(pos.getX() + 0.5, pos.getY() + 0.1, pos.getZ() + 0.5);
                        return InteractionResult.SUCCESS;
                    }
                }
            }
        }

        // 兜底：检查工位位置上的任何工人实体
        for (LivingEntity worker : level.getEntitiesOfClass(LivingEntity.class, searchBox.inflate(0.5))) {
            if (!WorkerUtil.isNonPlayerWorker(worker))
                continue;
            if (worker.blockPosition().equals(pos)) {
                worker.setPos(pos.getX() + 0.5, pos.getY() + 0.1, pos.getZ() + 0.5);
                return InteractionResult.SUCCESS;
            }
        }

        return InteractionResult.PASS;
    }
}
