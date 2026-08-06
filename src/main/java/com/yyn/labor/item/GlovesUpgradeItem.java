package com.yyn.labor.item;

import com.yyn.labor.blocks.WorkerSeatBlock;
import com.yyn.labor.blocks.WorkerSeatBlockEntity;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;

import java.util.List;

/**
 * 手套升级 - 对工位使用后使该工位支持加热搅拌配方。
 * 消耗1个物品，不应重复消耗。
 */
public class GlovesUpgradeItem extends Item {

    public GlovesUpgradeItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        Block block = level.getBlockState(pos).getBlock();

        if (!(block instanceof WorkerSeatBlock))
            return InteractionResult.PASS;

        if (level.isClientSide)
            return InteractionResult.SUCCESS;

        BlockEntity be = level.getBlockEntity(pos);
        if (!(be instanceof WorkerSeatBlockEntity seatBE))
            return InteractionResult.PASS;

        // 检查是否已应用
        if (seatBE.hasGlovesUpgrade()) {
            Player player = context.getPlayer();
            if (player != null) {
                player.displayClientMessage(
                    Component.translatable("message.create_labor.gloves_upgrade.already_applied")
                        .withStyle(ChatFormatting.RED), true);
            }
            return InteractionResult.FAIL;
        }

        // 应用升级
        seatBE.setGlovesUpgrade(true);

        // 消耗物品
        ItemStack stack = context.getItemInHand();
        Player player = context.getPlayer();
        if (player != null && !player.getAbilities().instabuild)
            stack.shrink(1);

        if (player != null) {
            player.displayClientMessage(
                Component.translatable("message.create_labor.gloves_upgrade.applied")
                    .withStyle(ChatFormatting.GREEN), true);
        }

        return InteractionResult.SUCCESS;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, context, tooltip, flag);
        tooltip.add(Component.translatable("tooltip.create_labor.gloves_upgrade")
            .withStyle(ChatFormatting.GRAY));
    }
}
