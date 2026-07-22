package com.yyn.labor;

<<<<<<< Updated upstream
import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
=======
import com.yyn.labor.blocks.WorkerSeatRenderer;

import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
>>>>>>> Stashed changes
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

@Mod.EventBusSubscriber(modid = CreateVillagerLabor.MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class CreateVillagerLaborClient {

    @SubscribeEvent
    static void onClientSetup(FMLClientSetupEvent event) {
        CreateVillagerLabor.LOGGER.info("HELLO FROM CLIENT SETUP");
        CreateVillagerLabor.LOGGER.info("MINECRAFT NAME >> {}", Minecraft.getInstance().getUser().getName());
    }

<<<<<<< Updated upstream
=======
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

>>>>>>> Stashed changes
    // 染色通过 blockstate 变体引用不同纹理实现，不再需要 BlockColor/ItemColor tintindex 染色
}
