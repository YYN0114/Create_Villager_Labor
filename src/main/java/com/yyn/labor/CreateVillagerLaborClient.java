package com.yyn.labor;

import com.yyn.labor.blocks.WorkerSeatBlock;

import net.minecraft.client.Minecraft;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.RegisterColorHandlersEvent;
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
    static void onRegisterBlockColors(RegisterColorHandlersEvent.Block event) {
        event.getBlockColors().register((state, reader, pos, tintIndex) -> {
            if (tintIndex == 0 && state.hasProperty(WorkerSeatBlock.COLOR))
                return state.getValue(WorkerSeatBlock.COLOR).getTextColor();
            return -1;
        }, CreateVillagerLabor.PRESS_SEAT.get(),
           CreateVillagerLabor.MIXER_SEAT.get(),
           CreateVillagerLabor.SAW_SEAT.get(),
           CreateVillagerLabor.MILLSTONE_SEAT.get(),
           CreateVillagerLabor.DEPLOYER_SEAT.get());
    }

    @SubscribeEvent
    static void onRegisterItemColors(RegisterColorHandlersEvent.Item event) {
        event.getItemColors().register((stack, tintIndex) -> {
            if (tintIndex == 0 && stack.getItem() instanceof BlockItem bi) {
                BlockState defaultState = bi.getBlock().defaultBlockState();
                if (defaultState.hasProperty(WorkerSeatBlock.COLOR))
                    return defaultState.getValue(WorkerSeatBlock.COLOR).getTextColor();
            }
            return -1;
        }, CreateVillagerLabor.PRESS_SEAT_ITEM.get(),
           CreateVillagerLabor.MIXER_SEAT_ITEM.get(),
           CreateVillagerLabor.SAW_SEAT_ITEM.get(),
           CreateVillagerLabor.MILLSTONE_SEAT_ITEM.get(),
           CreateVillagerLabor.DEPLOYER_SEAT_ITEM.get());
    }
}
