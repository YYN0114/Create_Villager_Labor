package com.yyn.labor.blocks;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;

import com.simibubi.create.foundation.blockEntity.behaviour.filtering.FilteringRenderer;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

@OnlyIn(Dist.CLIENT)
public class WorkerSeatRenderer implements BlockEntityRenderer<WorkerSeatBlockEntity> {

    @Override
    public void render(WorkerSeatBlockEntity be, float partialTick, PoseStack ms, MultiBufferSource buffer, int packedLight, int packedOverlay) {
        FilteringRenderer.renderOnBlockEntity(be, partialTick, ms, buffer, packedLight, packedOverlay);

        ItemStack stack = be.processingStack;
        if (stack.isEmpty()) {
            if (be instanceof DeployerSeatBlockEntity dbe && !dbe.getDeployerHeldItem().isEmpty()) {
                stack = dbe.getDeployerHeldItem();
            }
        }
        if (stack.isEmpty())
            return;

        Level level = be.getLevel();
        BlockPos pos = be.getBlockPos();

        Minecraft mc = Minecraft.getInstance();
        ItemRenderer itemRenderer = mc.getItemRenderer();

        ms.pushPose();

        float floatOffset = (float) (Mth.sin((level.getGameTime() + partialTick) / 10.0f) * 0.05f);

        ms.translate(0.5, 1.1 + floatOffset, 0.5);
        ms.scale(0.6f, 0.6f, 0.6f);
        ms.mulPose(Axis.YP.rotationDegrees((level.getGameTime() + partialTick) * 3));

        itemRenderer.renderStatic(stack, ItemDisplayContext.GROUND, packedLight, OverlayTexture.NO_OVERLAY, ms, buffer, level, 0);

        ms.popPose();
    }
}
