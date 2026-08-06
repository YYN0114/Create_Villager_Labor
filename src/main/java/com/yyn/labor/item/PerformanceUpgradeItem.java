package com.yyn.labor.item;

import com.yyn.labor.CreateVillagerLabor;
import com.yyn.labor.blocks.WorkerSeatBlock;
import com.yyn.labor.entity.LaborEntity;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.AABB;

import java.util.List;

/**
 * 性能升级 - 对工位使用后生成一个村民工人实体，作为永久工人。
 * 消耗1个物品，不应重复消耗。
 */
public class PerformanceUpgradeItem extends Item {

    public PerformanceUpgradeItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        Block block = level.getBlockState(pos).getBlock();

        // 检查目标是否为工位方块
        if (!(block instanceof WorkerSeatBlock))
            return InteractionResult.PASS;

        if (level.isClientSide)
            return InteractionResult.SUCCESS;

        // 检查该工位是否已有存活的 LaborEntity（搜索框已限定范围，只需 isAlive 即可）
        AABB searchBox = new AABB(pos).inflate(0.5);
        List<LaborEntity> existing = level.getEntitiesOfClass(LaborEntity.class, searchBox,
            e -> e.isAlive());
        if (!existing.isEmpty()) {
            // 已有村民工人，提示
            context.getPlayer().displayClientMessage(
                Component.translatable("message.create_labor.performance_upgrade.already_applied")
                    .withStyle(ChatFormatting.RED), true);
            return InteractionResult.FAIL;
        }

        // 检查是否有其他实体坐在工位上
        AABB seatBox = new AABB(pos).inflate(0.5);
        List<com.simibubi.create.content.contraptions.actors.seat.SeatEntity> seats =
            level.getEntitiesOfClass(com.simibubi.create.content.contraptions.actors.seat.SeatEntity.class, seatBox);
        if (!seats.isEmpty() && seats.get(0).isVehicle()) {
            context.getPlayer().displayClientMessage(
                Component.translatable("message.create_labor.performance_upgrade.seat_occupied")
                    .withStyle(ChatFormatting.RED), true);
            return InteractionResult.FAIL;
        }

        // 生成村民工人实体
        if (level instanceof ServerLevel serverLevel) {
            LaborEntity labor = CreateVillagerLabor.LABOR_ENTITY.get().create(serverLevel);
            if (labor != null) {
                // 放置在工位上方
                labor.setPos(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5);
                labor.setYRot(0);
                labor.yBodyRot = 0;
                labor.yHeadRot = 0;
                labor.setNoAi(true);
                labor.setInvulnerable(true);
                labor.setSilent(true);
                serverLevel.addFreshEntity(labor);

                // 消耗物品
                ItemStack stack = context.getItemInHand();
                if (!context.getPlayer().getAbilities().instabuild)
                    stack.shrink(1);

                context.getPlayer().displayClientMessage(
                    Component.translatable("message.create_labor.performance_upgrade.applied")
                        .withStyle(ChatFormatting.GREEN), true);
            }
        }

        return InteractionResult.SUCCESS;
    }

    @Override
    public void appendHoverText(ItemStack stack, Level level, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, level, tooltip, flag);
        tooltip.add(Component.translatable("tooltip.create_labor.performance_upgrade")
            .withStyle(ChatFormatting.GRAY));
    }
}
