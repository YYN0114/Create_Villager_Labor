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
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.fml.ModContainer;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

@Mod(CreateVillagerLabor.MODID)
public class CreateVillagerLabor {
    public static final String MODID = "create_labor";
    public static final Logger LOGGER = LogUtils.getLogger();

    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(MODID);
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(MODID);
    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, MODID);
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES = DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, MODID);
    public static final DeferredRegister<SoundEvent> SOUND_EVENTS = DeferredRegister.create(Registries.SOUND_EVENT, MODID);

    public static final DeferredBlock<PressSeatBlock> PRESS_SEAT = BLOCKS.register("press_seat", PressSeatBlock::new);
    public static final DeferredItem<BlockItem> PRESS_SEAT_ITEM = ITEMS.registerSimpleBlockItem("press_seat", PRESS_SEAT);

    public static final DeferredBlock<MixerSeatBlock> MIXER_SEAT = BLOCKS.register("mixer_seat", MixerSeatBlock::new);
    public static final DeferredItem<BlockItem> MIXER_SEAT_ITEM = ITEMS.registerSimpleBlockItem("mixer_seat", MIXER_SEAT);

    public static final DeferredBlock<SawSeatBlock> SAW_SEAT = BLOCKS.register("saw_seat", SawSeatBlock::new);
    public static final DeferredItem<BlockItem> SAW_SEAT_ITEM = ITEMS.registerSimpleBlockItem("saw_seat", SAW_SEAT);

    public static final DeferredBlock<MillstoneSeatBlock> MILLSTONE_SEAT = BLOCKS.register("millstone_seat", MillstoneSeatBlock::new);
    public static final DeferredItem<BlockItem> MILLSTONE_SEAT_ITEM = ITEMS.registerSimpleBlockItem("millstone_seat", MILLSTONE_SEAT);

    public static final DeferredBlock<DeployerSeatBlock> DEPLOYER_SEAT = BLOCKS.register("deployer_seat", DeployerSeatBlock::new);
    public static final DeferredItem<BlockItem> DEPLOYER_SEAT_ITEM = ITEMS.registerSimpleBlockItem("deployer_seat", DEPLOYER_SEAT);

    public static final DeferredItem<VillagerBinderItem> VILLAGER_BINDER = ITEMS.register("villager_binder", VillagerBinderItem::new);

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<PressSeatBlockEntity>> PRESS_SEAT_ENTITY = BLOCK_ENTITIES.register("press_seat", () -> {
        BlockEntityType<PressSeatBlockEntity> type = BlockEntityType.Builder.of(PressSeatBlockEntity::create, PRESS_SEAT.get()).build(null);
        PressSeatBlock.BLOCK_ENTITY_TYPE = type;
        return type;
    });
    
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<MixerSeatBlockEntity>> MIXER_SEAT_ENTITY = BLOCK_ENTITIES.register("mixer_seat", () -> {
        BlockEntityType<MixerSeatBlockEntity> type = BlockEntityType.Builder.of(MixerSeatBlockEntity::create, MIXER_SEAT.get()).build(null);
        MixerSeatBlock.BLOCK_ENTITY_TYPE = type;
        return type;
    });
    
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<SawSeatBlockEntity>> SAW_SEAT_ENTITY = BLOCK_ENTITIES.register("saw_seat", () -> {
        BlockEntityType<SawSeatBlockEntity> type = BlockEntityType.Builder.of(SawSeatBlockEntity::create, SAW_SEAT.get()).build(null);
        SawSeatBlock.BLOCK_ENTITY_TYPE = type;
        return type;
    });
    
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<MillstoneSeatBlockEntity>> MILLSTONE_SEAT_ENTITY = BLOCK_ENTITIES.register("millstone_seat", () -> {
        BlockEntityType<MillstoneSeatBlockEntity> type = BlockEntityType.Builder.of(MillstoneSeatBlockEntity::create, MILLSTONE_SEAT.get()).build(null);
        MillstoneSeatBlock.BLOCK_ENTITY_TYPE = type;
        return type;
    });

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<DeployerSeatBlockEntity>> DEPLOYER_SEAT_ENTITY = BLOCK_ENTITIES.register("deployer_seat", () -> {
        BlockEntityType<DeployerSeatBlockEntity> type = BlockEntityType.Builder.of(DeployerSeatBlockEntity::create, DEPLOYER_SEAT.get()).build(null);
        DeployerSeatBlock.BLOCK_ENTITY_TYPE = type;
        return type;
    });

    public static final DeferredHolder<SoundEvent, SoundEvent> DEPLOYER_WORK_SOUND = SOUND_EVENTS.register("deployer_work", () -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath(MODID, "deployer_work")));
    public static final DeferredHolder<SoundEvent, SoundEvent> PRESS_WORK_SOUND = SOUND_EVENTS.register("press_work", () -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath(MODID, "press_work")));
    public static final DeferredHolder<SoundEvent, SoundEvent> MILLSTONE_WORK_SOUND = SOUND_EVENTS.register("millstone_work", () -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath(MODID, "millstone_work")));
    public static final DeferredHolder<SoundEvent, SoundEvent> SAW_WORK_SOUND = SOUND_EVENTS.register("saw_work", () -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath(MODID, "saw_work")));

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> CREATE_LABOR_TAB = CREATIVE_MODE_TABS.register("create_labor", () -> CreativeModeTab.builder()
            .title(Component.translatable("itemGroup.create_labor"))
            .withTabsBefore(CreativeModeTabs.TOOLS_AND_UTILITIES)
            .icon(() -> PRESS_SEAT_ITEM.get().getDefaultInstance())
            .displayItems((parameters, output) -> {
                output.accept(PRESS_SEAT_ITEM.get());
                output.accept(MIXER_SEAT_ITEM.get());
                output.accept(SAW_SEAT_ITEM.get());
                output.accept(MILLSTONE_SEAT_ITEM.get());
                output.accept(DEPLOYER_SEAT_ITEM.get());
                output.accept(VILLAGER_BINDER.get());
            }).build());

    public CreateVillagerLabor(IEventBus modEventBus, ModContainer modContainer) {
        BLOCKS.register(modEventBus);
        ITEMS.register(modEventBus);
        BLOCK_ENTITIES.register(modEventBus);
        CREATIVE_MODE_TABS.register(modEventBus);
        SOUND_EVENTS.register(modEventBus);

        if (FMLEnvironment.dist == Dist.CLIENT) {
            modEventBus.addListener(this::onRegisterRenderers);
        }

        modContainer.registerConfig(ModConfig.Type.COMMON, Config.SPEC);
    }

    private void onRegisterRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(PRESS_SEAT_ENTITY.get(), ctx -> new WorkerSeatRenderer());
        event.registerBlockEntityRenderer(MIXER_SEAT_ENTITY.get(), ctx -> new WorkerSeatRenderer());
        event.registerBlockEntityRenderer(SAW_SEAT_ENTITY.get(), ctx -> new WorkerSeatRenderer());
        event.registerBlockEntityRenderer(MILLSTONE_SEAT_ENTITY.get(), ctx -> new WorkerSeatRenderer());
        event.registerBlockEntityRenderer(DEPLOYER_SEAT_ENTITY.get(), ctx -> new WorkerSeatRenderer());
    }
}
