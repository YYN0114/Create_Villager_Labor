package com.yyn.labor;

import com.yyn.labor.blocks.WorkerSeatRenderer;

import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
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
    public CreateVillagerLaborClient(ModContainer container) {
        // Allows NeoForge to create a config screen for this mod's configs.
        // The config screen is accessed by going to the Mods screen > clicking on your mod > clicking on config.
        // Do not forget to add translations for your config options to the en_us.json file.
        container.registerExtensionPoint(IConfigScreenFactory.class, ConfigurationScreen::new);
    }

    @SubscribeEvent
    static void onClientSetup(FMLClientSetupEvent event) {
        // Some client setup code
        CreateVillagerLabor.LOGGER.info("HELLO FROM CLIENT SETUP");
        CreateVillagerLabor.LOGGER.info("MINECRAFT NAME >> {}", Minecraft.getInstance().getUser().getName());
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
    }

    // 染色通过 blockstate 变体引用不同纹理实现，不再需要 BlockColor/ItemColor tintindex 染色
}
