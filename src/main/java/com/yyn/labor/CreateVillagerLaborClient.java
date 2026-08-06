package com.yyn.labor;

import com.yyn.labor.blocks.WorkerHatLayer;
import com.yyn.labor.blocks.WorkerSeatRenderer;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.VillagerRenderer;
import net.minecraft.world.entity.EntityType;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

@Mod.EventBusSubscriber(modid = CreateVillagerLabor.MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class CreateVillagerLaborClient {

    @SubscribeEvent
    static void onClientSetup(FMLClientSetupEvent event) {
        CreateVillagerLabor.LOGGER.info("HELLO FROM CLIENT SETUP");
        CreateVillagerLabor.LOGGER.info("MINECRAFT NAME >> {}", Minecraft.getInstance().getUser().getName());

        // 提前触发 WorkerHatLayer 类加载，确保 PartialModel.of() 在模型烘焙前注册
        Object earlyRef = WorkerHatLayer.WORKER_HAT;
    }

    @SubscribeEvent
    static void onRegisterRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(CreateVillagerLabor.ANDESITE_PRESS_SEAT_ENTITY.get(), ctx -> new WorkerSeatRenderer());
        event.registerBlockEntityRenderer(CreateVillagerLabor.ANDESITE_MIXER_SEAT_ENTITY.get(), ctx -> new WorkerSeatRenderer());
        event.registerBlockEntityRenderer(CreateVillagerLabor.ANDESITE_SAW_SEAT_ENTITY.get(), ctx -> new WorkerSeatRenderer());
        event.registerBlockEntityRenderer(CreateVillagerLabor.ANDESITE_MILLSTONE_SEAT_ENTITY.get(), ctx -> new WorkerSeatRenderer());
        event.registerBlockEntityRenderer(CreateVillagerLabor.ANDESITE_DEPLOYER_SEAT_ENTITY.get(), ctx -> new WorkerSeatRenderer());
        event.registerBlockEntityRenderer(CreateVillagerLabor.COPPER_PRESS_SEAT_ENTITY.get(), ctx -> new WorkerSeatRenderer());
        event.registerBlockEntityRenderer(CreateVillagerLabor.COPPER_MIXER_SEAT_ENTITY.get(), ctx -> new WorkerSeatRenderer());
        event.registerBlockEntityRenderer(CreateVillagerLabor.COPPER_SAW_SEAT_ENTITY.get(), ctx -> new WorkerSeatRenderer());
        event.registerBlockEntityRenderer(CreateVillagerLabor.COPPER_MILLSTONE_SEAT_ENTITY.get(), ctx -> new WorkerSeatRenderer());
        event.registerBlockEntityRenderer(CreateVillagerLabor.COPPER_DEPLOYER_SEAT_ENTITY.get(), ctx -> new WorkerSeatRenderer());
        event.registerBlockEntityRenderer(CreateVillagerLabor.BRASS_PRESS_SEAT_ENTITY.get(), ctx -> new WorkerSeatRenderer());
        event.registerBlockEntityRenderer(CreateVillagerLabor.BRASS_MIXER_SEAT_ENTITY.get(), ctx -> new WorkerSeatRenderer());
        event.registerBlockEntityRenderer(CreateVillagerLabor.BRASS_SAW_SEAT_ENTITY.get(), ctx -> new WorkerSeatRenderer());
        event.registerBlockEntityRenderer(CreateVillagerLabor.BRASS_MILLSTONE_SEAT_ENTITY.get(), ctx -> new WorkerSeatRenderer());
        event.registerBlockEntityRenderer(CreateVillagerLabor.BRASS_DEPLOYER_SEAT_ENTITY.get(), ctx -> new WorkerSeatRenderer());
        event.registerBlockEntityRenderer(CreateVillagerLabor.CREATIVE_PRESS_SEAT_ENTITY.get(), ctx -> new WorkerSeatRenderer());
        event.registerBlockEntityRenderer(CreateVillagerLabor.CREATIVE_MIXER_SEAT_ENTITY.get(), ctx -> new WorkerSeatRenderer());
        event.registerBlockEntityRenderer(CreateVillagerLabor.CREATIVE_SAW_SEAT_ENTITY.get(), ctx -> new WorkerSeatRenderer());
        event.registerBlockEntityRenderer(CreateVillagerLabor.CREATIVE_MILLSTONE_SEAT_ENTITY.get(), ctx -> new WorkerSeatRenderer());
        event.registerBlockEntityRenderer(CreateVillagerLabor.CREATIVE_DEPLOYER_SEAT_ENTITY.get(), ctx -> new WorkerSeatRenderer());

        event.registerEntityRenderer(CreateVillagerLabor.LABOR_ENTITY.get(), VillagerRenderer::new);
    }

    // 给所有 VillagerRenderer（原版村民 + LaborEntity）添加工帽渲染层
    @SuppressWarnings({"unchecked", "rawtypes"})
    @SubscribeEvent
    static void onAddLayers(EntityRenderersEvent.AddLayers event) {
        // 原版村民渲染器
        VillagerRenderer villagerRenderer = event.getRenderer(EntityType.VILLAGER);
        if (villagerRenderer != null) {
            villagerRenderer.addLayer(new WorkerHatLayer(villagerRenderer));
        }
        // LaborEntity 渲染器
        LivingEntityRenderer<?, ?> laborRenderer = (LivingEntityRenderer<?, ?>) event.getRenderer(CreateVillagerLabor.LABOR_ENTITY.get());
        if (laborRenderer instanceof VillagerRenderer vr) {
            vr.addLayer(new WorkerHatLayer(vr));
        }
    }
}