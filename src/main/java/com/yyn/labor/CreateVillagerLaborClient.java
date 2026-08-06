package com.yyn.labor;

import com.yyn.labor.blocks.WorkerHatLayer;
import com.yyn.labor.blocks.WorkerSeatRenderer;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.VillagerRenderer;
import net.minecraft.world.entity.EntityType;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.gui.ConfigurationScreen;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;

// This class will not load on dedicated servers. Accessing client side code from here is safe.
@Mod(value = CreateVillagerLabor.MODID, dist = Dist.CLIENT)
// You can use EventBusSubscriber to automatically register all static methods in the class annotated with @SubscribeEvent
@EventBusSubscriber(modid = CreateVillagerLabor.MODID, value = Dist.CLIENT)
public class CreateVillagerLaborClient {
    public CreateVillagerLaborClient(IEventBus modEventBus, ModContainer container) {
        // Allows NeoForge to create a config screen for this mod's configs.
        container.registerExtensionPoint(IConfigScreenFactory.class, ConfigurationScreen::new);

        // 在构造器中触发 WorkerHatLayer 类加载，确保 PartialModel.of() 在 ModelEvent.RegisterAdditional 之前执行
        // 否则模型不会被烘焙，渲染时为紫黑方块
        Object earlyRef = WorkerHatLayer.WORKER_HAT;

        // 注册渲染器（客户端专用，避免主类在服务端加载客户端类）
        modEventBus.addListener(CreateVillagerLaborClient::onRegisterRenderers);
        // 添加渲染层（在所有渲染器注册完成后，给村民渲染器添加工帽层）
        modEventBus.addListener(CreateVillagerLaborClient::onAddLayers);
    }

    @SubscribeEvent
    static void onClientSetup(FMLClientSetupEvent event) {
        // Some client setup code
        CreateVillagerLabor.LOGGER.info("HELLO FROM CLIENT SETUP");
        CreateVillagerLabor.LOGGER.info("MINECRAFT NAME >> {}", Minecraft.getInstance().getUser().getName());
    }

    // 客户端渲染器注册：BlockEntityRenderer + LaborEntity 渲染器
    private static void onRegisterRenderers(EntityRenderersEvent.RegisterRenderers event) {
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

        // LaborEntity 使用普通 VillagerRenderer（工帽层在 AddLayers 中统一添加）
        event.registerEntityRenderer(CreateVillagerLabor.LABOR_ENTITY.get(), VillagerRenderer::new);
    }

    // 给所有 VillagerRenderer（原版村民 + LaborEntity）添加工帽渲染层
    @SuppressWarnings({"unchecked", "rawtypes"})
    private static void onAddLayers(EntityRenderersEvent.AddLayers event) {
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

    // 染色通过 blockstate 变体引用不同纹理实现，不再需要 BlockColor/ItemColor tintindex 染色
}
