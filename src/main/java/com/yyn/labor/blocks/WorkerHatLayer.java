package com.yyn.labor.blocks;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;

import dev.engine_room.flywheel.lib.model.baked.PartialModel;
import net.createmod.catnip.render.CachedBuffers;
import net.minecraft.client.model.VillagerModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

/**
 * 工帽渲染层 - 参考 Create mod 的 CreateHatArmorLayer 实现。
 * 使用 PartialModel + CachedBuffers 渲染工帽，与 Create 的帽子渲染方式一致。
 * 资源路径：assets/create_labor/models/entity/worker_hat.json
 */
@OnlyIn(Dist.CLIENT)
public class WorkerHatLayer extends RenderLayer<Villager, VillagerModel<Villager>> {

    // 自定义工帽 PartialModel，对应 assets/create_labor/models/entity/worker_hat.json
    public static final PartialModel WORKER_HAT = PartialModel.of(
        new ResourceLocation("create_labor", "entity/worker_hat"));

    public WorkerHatLayer(RenderLayerParent<Villager, VillagerModel<Villager>> renderer) {
        super(renderer);
    }

    @Override
    public void render(PoseStack ms, MultiBufferSource buffer, int packedLight, Villager entity,
                       float limbSwing, float limbSwingAmount, float partialTicks,
                       float ageInTicks, float netHeadYaw, float headPitch) {
        if (!isWorkerOnSeat(entity))
            return;
        if (!entity.getItemBySlot(EquipmentSlot.HEAD).isEmpty())
            return;

        VillagerModel<Villager> model = getParentModel();
        ms.pushPose();

        if (model.young) {
            ms.translate(0.0D, 0.5D, 0.0D);
        }

        ModelPart head = model.getHead();
        head.translateAndRotate(ms);

        // 村民头部为 8x8x8 立方体（默认 TrainHatInfo：offset=(0,0,0), scale=1.0, cubeIndex=0）
        // (cube.minY - cube.maxY) / 16 = (-8 - 0) / 16 = -0.5
        // max = max(8, 8) / 8 * 1.0 = 1.0，scale(1,1,1) 可省略
        ms.translate(0, -0.5F, 0);

        ms.scale(1, -1, -1);
        // 帽子偏移：上 0.2 后再下 0.1（净上 0.1）
        ms.translate(0, -0.6F / 16.0F, 0);
        ms.mulPose(Axis.XP.rotationDegrees(-8.5F));

        BlockState air = Blocks.AIR.defaultBlockState();
        CachedBuffers.partial(WORKER_HAT, air)
            .disableDiffuse()
            .light(packedLight)
            .renderInto(ms, buffer.getBuffer(Sheets.cutoutBlockSheet()));

        ms.popPose();
    }

    // 检查村民是否坐在工位上：优先检查车辆（SeatEntity）位置，fallback 检查自身位置
    private static boolean isWorkerOnSeat(Villager entity) {
        Entity vehicle = entity.getVehicle();
        if (vehicle != null) {
            Block block = entity.level().getBlockState(vehicle.blockPosition()).getBlock();
            if (block instanceof WorkerSeatBlock)
                return true;
        }
        Block block = entity.level().getBlockState(entity.blockPosition()).getBlock();
        return block instanceof WorkerSeatBlock;
    }
}
