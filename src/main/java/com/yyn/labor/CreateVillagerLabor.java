package com.yyn.labor;

import org.slf4j.Logger;

import com.mojang.logging.LogUtils;

import com.yyn.labor.blocks.PressSeatBlock;
import com.yyn.labor.blocks.PressSeatBlockEntity;
import com.yyn.labor.blocks.MixerSeatBlock;
import com.yyn.labor.blocks.MixerSeatBlockEntity;
import com.yyn.labor.blocks.SawSeatBlock;
import com.yyn.labor.blocks.SawSeatBlockEntity;
import com.yyn.labor.blocks.MillstoneSeatBlock;
import com.yyn.labor.blocks.MillstoneSeatBlockEntity;
import com.yyn.labor.blocks.DeployerSeatBlock;
import com.yyn.labor.blocks.DeployerSeatBlockEntity;
import com.yyn.labor.blocks.WorkerSeatRenderer;

import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

@Mod(CreateVillagerLabor.MODID)
public class CreateVillagerLabor {
    public static final String MODID = "create_labor";
    public static final Logger LOGGER = LogUtils.getLogger();

    public static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(ForgeRegistries.BLOCKS, MODID);
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, MODID);
    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, MODID);
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES = DeferredRegister.create(ForgeRegistries.BLOCK_ENTITY_TYPES, MODID);
    public static final DeferredRegister<SoundEvent> SOUND_EVENTS = DeferredRegister.create(ForgeRegistries.SOUND_EVENTS, MODID);

    public static final RegistryObject<Block> PRESS_SEAT = BLOCKS.register("press_seat", PressSeatBlock::new);
    public static final RegistryObject<Item> PRESS_SEAT_ITEM = ITEMS.register("press_seat", () -> new BlockItem(PRESS_SEAT.get(), new Item.Properties()));

    public static final RegistryObject<Block> MIXER_SEAT = BLOCKS.register("mixer_seat", MixerSeatBlock::new);
    public static final RegistryObject<Item> MIXER_SEAT_ITEM = ITEMS.register("mixer_seat", () -> new BlockItem(MIXER_SEAT.get(), new Item.Properties()));

    public static final RegistryObject<Block> SAW_SEAT = BLOCKS.register("saw_seat", SawSeatBlock::new);
    public static final RegistryObject<Item> SAW_SEAT_ITEM = ITEMS.register("saw_seat", () -> new BlockItem(SAW_SEAT.get(), new Item.Properties()));

    public static final RegistryObject<Block> MILLSTONE_SEAT = BLOCKS.register("millstone_seat", MillstoneSeatBlock::new);
    public static final RegistryObject<Item> MILLSTONE_SEAT_ITEM = ITEMS.register("millstone_seat", () -> new BlockItem(MILLSTONE_SEAT.get(), new Item.Properties()));

    public static final RegistryObject<Block> DEPLOYER_SEAT = BLOCKS.register("deployer_seat", DeployerSeatBlock::new);
    public static final RegistryObject<Item> DEPLOYER_SEAT_ITEM = ITEMS.register("deployer_seat", () -> new BlockItem(DEPLOYER_SEAT.get(), new Item.Properties()));

    public static final RegistryObject<Item> VILLAGER_BINDER = ITEMS.register("villager_binder", VillagerBinderItem::new);

    public static final RegistryObject<BlockEntityType<PressSeatBlockEntity>> PRESS_SEAT_ENTITY = BLOCK_ENTITIES.register("press_seat", () -> {
        BlockEntityType<PressSeatBlockEntity> type = BlockEntityType.Builder.of(PressSeatBlockEntity::create, PRESS_SEAT.get()).build(null);
        PressSeatBlock.BLOCK_ENTITY_TYPE = type;
        return type;
    });

    public static final RegistryObject<BlockEntityType<MixerSeatBlockEntity>> MIXER_SEAT_ENTITY = BLOCK_ENTITIES.register("mixer_seat", () -> {
        BlockEntityType<MixerSeatBlockEntity> type = BlockEntityType.Builder.of(MixerSeatBlockEntity::create, MIXER_SEAT.get()).build(null);
        MixerSeatBlock.BLOCK_ENTITY_TYPE = type;
        return type;
    });

    public static final RegistryObject<BlockEntityType<SawSeatBlockEntity>> SAW_SEAT_ENTITY = BLOCK_ENTITIES.register("saw_seat", () -> {
        BlockEntityType<SawSeatBlockEntity> type = BlockEntityType.Builder.of(SawSeatBlockEntity::create, SAW_SEAT.get()).build(null);
        SawSeatBlock.BLOCK_ENTITY_TYPE = type;
        return type;
    });

    public static final RegistryObject<BlockEntityType<MillstoneSeatBlockEntity>> MILLSTONE_SEAT_ENTITY = BLOCK_ENTITIES.register("millstone_seat", () -> {
        BlockEntityType<MillstoneSeatBlockEntity> type = BlockEntityType.Builder.of(MillstoneSeatBlockEntity::create, MILLSTONE_SEAT.get()).build(null);
        MillstoneSeatBlock.BLOCK_ENTITY_TYPE = type;
        return type;
    });

    public static final RegistryObject<BlockEntityType<DeployerSeatBlockEntity>> DEPLOYER_SEAT_ENTITY = BLOCK_ENTITIES.register("deployer_seat", () -> {
        BlockEntityType<DeployerSeatBlockEntity> type = BlockEntityType.Builder.of(DeployerSeatBlockEntity::create, DEPLOYER_SEAT.get()).build(null);
        DeployerSeatBlock.BLOCK_ENTITY_TYPE = type;
        return type;
    });

    public static final RegistryObject<SoundEvent> DEPLOYER_WORK_SOUND = SOUND_EVENTS.register("deployer_work", () -> SoundEvent.createVariableRangeEvent(new ResourceLocation(MODID, "deployer_work")));
    public static final RegistryObject<SoundEvent> PRESS_WORK_SOUND = SOUND_EVENTS.register("press_work", () -> SoundEvent.createVariableRangeEvent(new ResourceLocation(MODID, "press_work")));
    public static final RegistryObject<SoundEvent> MILLSTONE_WORK_SOUND = SOUND_EVENTS.register("millstone_work", () -> SoundEvent.createVariableRangeEvent(new ResourceLocation(MODID, "millstone_work")));
    public static final RegistryObject<SoundEvent> SAW_WORK_SOUND = SOUND_EVENTS.register("saw_work", () -> SoundEvent.createVariableRangeEvent(new ResourceLocation(MODID, "saw_work")));

    public static final RegistryObject<CreativeModeTab> CREATE_LABOR_TAB = CREATIVE_MODE_TABS.register("create_labor", () -> CreativeModeTab.builder()
            .title(Component.translatable("itemGroup.create_labor"))
            .icon(() -> PRESS_SEAT_ITEM.get().getDefaultInstance())
            .displayItems((parameters, output) -> {
                output.accept(PRESS_SEAT_ITEM.get());
                output.accept(MIXER_SEAT_ITEM.get());
                output.accept(SAW_SEAT_ITEM.get());
                output.accept(MILLSTONE_SEAT_ITEM.get());
                output.accept(DEPLOYER_SEAT_ITEM.get());
                output.accept(VILLAGER_BINDER.get());
            }).build());

    public CreateVillagerLabor() {
        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();
        BLOCKS.register(modEventBus);
        ITEMS.register(modEventBus);
        BLOCK_ENTITIES.register(modEventBus);
        CREATIVE_MODE_TABS.register(modEventBus);
        SOUND_EVENTS.register(modEventBus);

        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, Config.SPEC);
    }

    @Mod.EventBusSubscriber(modid = MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
    public static class ClientModEvents {
        @SubscribeEvent
        public static void onRegisterRenderers(EntityRenderersEvent.RegisterRenderers event) {
            event.registerBlockEntityRenderer(PRESS_SEAT_ENTITY.get(), ctx -> new WorkerSeatRenderer());
            event.registerBlockEntityRenderer(MIXER_SEAT_ENTITY.get(), ctx -> new WorkerSeatRenderer());
            event.registerBlockEntityRenderer(SAW_SEAT_ENTITY.get(), ctx -> new WorkerSeatRenderer());
            event.registerBlockEntityRenderer(MILLSTONE_SEAT_ENTITY.get(), ctx -> new WorkerSeatRenderer());
            event.registerBlockEntityRenderer(DEPLOYER_SEAT_ENTITY.get(), ctx -> new WorkerSeatRenderer());
        }
    }
}
