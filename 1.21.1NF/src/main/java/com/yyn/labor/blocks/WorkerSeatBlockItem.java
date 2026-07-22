package com.yyn.labor.blocks;

import java.util.List;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;

public class WorkerSeatBlockItem extends BlockItem {
    private final SeatMaterial material;

    public WorkerSeatBlockItem(Block block, SeatMaterial material, Item.Properties properties) {
        super(block, properties);
        this.material = material;
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, context, tooltip, flag);

        if (material == SeatMaterial.CREATIVE) {
            tooltip.add(Component.translatable("tooltip.create_labor.creative")
                .withStyle(ChatFormatting.LIGHT_PURPLE));
        } else {
            tooltip.add(Component.translatable("tooltip.create_labor.belt_warning", material.getRpm())
                .withStyle(ChatFormatting.GRAY));
        }
    }
}
