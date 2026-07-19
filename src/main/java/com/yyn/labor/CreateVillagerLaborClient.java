package com.yyn.labor;

import com.yyn.labor.blocks.WorkerSeatBlock;

import net.minecraft.client.Minecraft;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterColorHandlersEvent;
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
