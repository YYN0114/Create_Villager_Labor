package com.yyn.labor;

import org.slf4j.Logger;

import com.mojang.logging.LogUtils;

import com.yyn.labor.blocks.SeatMaterial;
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
import com.yyn.labor.blocks.WorkerSeatBlockItem;

import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
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

    // ==================== Andesite variant ====================
    public static final RegistryObject<Block> ANDESITE_PRESS_SEAT = BLOCKS.register("andesite_press_seat", () -> new PressSeatBlock(SeatMaterial.ANDESITE));
    public static final RegistryObject<Item> ANDESITE_PRESS_SEAT_ITEM = ITEMS.register("andesite_press_seat", () -> new WorkerSeatBlockItem(ANDESITE_PRESS_SEAT.get(), SeatMaterial.ANDESITE, new Item.Properties()));

    public static final RegistryObject<Block> ANDESITE_MIXER_SEAT = BLOCKS.register("andesite_mixer_seat", () -> new MixerSeatBlock(SeatMaterial.ANDESITE));
    public static final RegistryObject<Item> ANDESITE_MIXER_SEAT_ITEM = ITEMS.register("andesite_mixer_seat", () -> new WorkerSeatBlockItem(ANDESITE_MIXER_SEAT.get(), SeatMaterial.ANDESITE, new Item.Properties()));

    public static final RegistryObject<Block> ANDESITE_SAW_SEAT = BLOCKS.register("andesite_saw_seat", () -> new SawSeatBlock(SeatMaterial.ANDESITE));
    public static final RegistryObject<Item> ANDESITE_SAW_SEAT_ITEM = ITEMS.register("andesite_saw_seat", () -> new WorkerSeatBlockItem(ANDESITE_SAW_SEAT.get(), SeatMaterial.ANDESITE, new Item.Properties()));

    public static final RegistryObject<Block> ANDESITE_MILLSTONE_SEAT = BLOCKS.register("andesite_millstone_seat", () -> new MillstoneSeatBlock(SeatMaterial.ANDESITE));
    public static final RegistryObject<Item> ANDESITE_MILLSTONE_SEAT_ITEM = ITEMS.register("andesite_millstone_seat", () -> new WorkerSeatBlockItem(ANDESITE_MILLSTONE_SEAT.get(), SeatMaterial.ANDESITE, new Item.Properties()));

    public static final RegistryObject<Block> ANDESITE_DEPLOYER_SEAT = BLOCKS.register("andesite_deployer_seat", () -> new DeployerSeatBlock(SeatMaterial.ANDESITE));
    public static final RegistryObject<Item> ANDESITE_DEPLOYER_SEAT_ITEM = ITEMS.register("andesite_deployer_seat", () -> new WorkerSeatBlockItem(ANDESITE_DEPLOYER_SEAT.get(), SeatMaterial.ANDESITE, new Item.Properties()));

    public static final RegistryObject<BlockEntityType<PressSeatBlockEntity>> ANDESITE_PRESS_SEAT_ENTITY = BLOCK_ENTITIES.register("andesite_press_seat", () -> {
        BlockEntityType<PressSeatBlockEntity> type = BlockEntityType.Builder.of(
            (pos, state) -> new PressSeatBlockEntity(PressSeatBlock.BLOCK_ENTITY_TYPES.get(SeatMaterial.ANDESITE), pos, state, SeatMaterial.ANDESITE),
            ANDESITE_PRESS_SEAT.get()).build(null);
        PressSeatBlock.BLOCK_ENTITY_TYPES.put(SeatMaterial.ANDESITE, type);
        return type;
    });

    public static final RegistryObject<BlockEntityType<MixerSeatBlockEntity>> ANDESITE_MIXER_SEAT_ENTITY = BLOCK_ENTITIES.register("andesite_mixer_seat", () -> {
        BlockEntityType<MixerSeatBlockEntity> type = BlockEntityType.Builder.of(
            (pos, state) -> new MixerSeatBlockEntity(MixerSeatBlock.BLOCK_ENTITY_TYPES.get(SeatMaterial.ANDESITE), pos, state, SeatMaterial.ANDESITE),
            ANDESITE_MIXER_SEAT.get()).build(null);
        MixerSeatBlock.BLOCK_ENTITY_TYPES.put(SeatMaterial.ANDESITE, type);
        return type;
    });

    public static final RegistryObject<BlockEntityType<SawSeatBlockEntity>> ANDESITE_SAW_SEAT_ENTITY = BLOCK_ENTITIES.register("andesite_saw_seat", () -> {
        BlockEntityType<SawSeatBlockEntity> type = BlockEntityType.Builder.of(
            (pos, state) -> new SawSeatBlockEntity(SawSeatBlock.BLOCK_ENTITY_TYPES.get(SeatMaterial.ANDESITE), pos, state, SeatMaterial.ANDESITE),
            ANDESITE_SAW_SEAT.get()).build(null);
        SawSeatBlock.BLOCK_ENTITY_TYPES.put(SeatMaterial.ANDESITE, type);
        return type;
    });

    public static final RegistryObject<BlockEntityType<MillstoneSeatBlockEntity>> ANDESITE_MILLSTONE_SEAT_ENTITY = BLOCK_ENTITIES.register("andesite_millstone_seat", () -> {
        BlockEntityType<MillstoneSeatBlockEntity> type = BlockEntityType.Builder.of(
            (pos, state) -> new MillstoneSeatBlockEntity(MillstoneSeatBlock.BLOCK_ENTITY_TYPES.get(SeatMaterial.ANDESITE), pos, state, SeatMaterial.ANDESITE),
            ANDESITE_MILLSTONE_SEAT.get()).build(null);
        MillstoneSeatBlock.BLOCK_ENTITY_TYPES.put(SeatMaterial.ANDESITE, type);
        return type;
    });

    public static final RegistryObject<BlockEntityType<DeployerSeatBlockEntity>> ANDESITE_DEPLOYER_SEAT_ENTITY = BLOCK_ENTITIES.register("andesite_deployer_seat", () -> {
        BlockEntityType<DeployerSeatBlockEntity> type = BlockEntityType.Builder.of(
            (pos, state) -> new DeployerSeatBlockEntity(DeployerSeatBlock.BLOCK_ENTITY_TYPES.get(SeatMaterial.ANDESITE), pos, state, SeatMaterial.ANDESITE),
            ANDESITE_DEPLOYER_SEAT.get()).build(null);
        DeployerSeatBlock.BLOCK_ENTITY_TYPES.put(SeatMaterial.ANDESITE, type);
        return type;
    });

    // ==================== Copper variant ====================
    public static final RegistryObject<Block> COPPER_PRESS_SEAT = BLOCKS.register("copper_press_seat", () -> new PressSeatBlock(SeatMaterial.COPPER));
    public static final RegistryObject<Item> COPPER_PRESS_SEAT_ITEM = ITEMS.register("copper_press_seat", () -> new WorkerSeatBlockItem(COPPER_PRESS_SEAT.get(), SeatMaterial.COPPER, new Item.Properties()));

    public static final RegistryObject<Block> COPPER_MIXER_SEAT = BLOCKS.register("copper_mixer_seat", () -> new MixerSeatBlock(SeatMaterial.COPPER));
    public static final RegistryObject<Item> COPPER_MIXER_SEAT_ITEM = ITEMS.register("copper_mixer_seat", () -> new WorkerSeatBlockItem(COPPER_MIXER_SEAT.get(), SeatMaterial.COPPER, new Item.Properties()));

    public static final RegistryObject<Block> COPPER_SAW_SEAT = BLOCKS.register("copper_saw_seat", () -> new SawSeatBlock(SeatMaterial.COPPER));
    public static final RegistryObject<Item> COPPER_SAW_SEAT_ITEM = ITEMS.register("copper_saw_seat", () -> new WorkerSeatBlockItem(COPPER_SAW_SEAT.get(), SeatMaterial.COPPER, new Item.Properties()));

    public static final RegistryObject<Block> COPPER_MILLSTONE_SEAT = BLOCKS.register("copper_millstone_seat", () -> new MillstoneSeatBlock(SeatMaterial.COPPER));
    public static final RegistryObject<Item> COPPER_MILLSTONE_SEAT_ITEM = ITEMS.register("copper_millstone_seat", () -> new WorkerSeatBlockItem(COPPER_MILLSTONE_SEAT.get(), SeatMaterial.COPPER, new Item.Properties()));

    public static final RegistryObject<Block> COPPER_DEPLOYER_SEAT = BLOCKS.register("copper_deployer_seat", () -> new DeployerSeatBlock(SeatMaterial.COPPER));
    public static final RegistryObject<Item> COPPER_DEPLOYER_SEAT_ITEM = ITEMS.register("copper_deployer_seat", () -> new WorkerSeatBlockItem(COPPER_DEPLOYER_SEAT.get(), SeatMaterial.COPPER, new Item.Properties()));

    public static final RegistryObject<BlockEntityType<PressSeatBlockEntity>> COPPER_PRESS_SEAT_ENTITY = BLOCK_ENTITIES.register("copper_press_seat", () -> {
        BlockEntityType<PressSeatBlockEntity> type = BlockEntityType.Builder.of(
            (pos, state) -> new PressSeatBlockEntity(PressSeatBlock.BLOCK_ENTITY_TYPES.get(SeatMaterial.COPPER), pos, state, SeatMaterial.COPPER),
            COPPER_PRESS_SEAT.get()).build(null);
        PressSeatBlock.BLOCK_ENTITY_TYPES.put(SeatMaterial.COPPER, type);
        return type;
    });

    public static final RegistryObject<BlockEntityType<MixerSeatBlockEntity>> COPPER_MIXER_SEAT_ENTITY = BLOCK_ENTITIES.register("copper_mixer_seat", () -> {
        BlockEntityType<MixerSeatBlockEntity> type = BlockEntityType.Builder.of(
            (pos, state) -> new MixerSeatBlockEntity(MixerSeatBlock.BLOCK_ENTITY_TYPES.get(SeatMaterial.COPPER), pos, state, SeatMaterial.COPPER),
            COPPER_MIXER_SEAT.get()).build(null);
        MixerSeatBlock.BLOCK_ENTITY_TYPES.put(SeatMaterial.COPPER, type);
        return type;
    });

    public static final RegistryObject<BlockEntityType<SawSeatBlockEntity>> COPPER_SAW_SEAT_ENTITY = BLOCK_ENTITIES.register("copper_saw_seat", () -> {
        BlockEntityType<SawSeatBlockEntity> type = BlockEntityType.Builder.of(
            (pos, state) -> new SawSeatBlockEntity(SawSeatBlock.BLOCK_ENTITY_TYPES.get(SeatMaterial.COPPER), pos, state, SeatMaterial.COPPER),
            COPPER_SAW_SEAT.get()).build(null);
        SawSeatBlock.BLOCK_ENTITY_TYPES.put(SeatMaterial.COPPER, type);
        return type;
    });

    public static final RegistryObject<BlockEntityType<MillstoneSeatBlockEntity>> COPPER_MILLSTONE_SEAT_ENTITY = BLOCK_ENTITIES.register("copper_millstone_seat", () -> {
        BlockEntityType<MillstoneSeatBlockEntity> type = BlockEntityType.Builder.of(
            (pos, state) -> new MillstoneSeatBlockEntity(MillstoneSeatBlock.BLOCK_ENTITY_TYPES.get(SeatMaterial.COPPER), pos, state, SeatMaterial.COPPER),
            COPPER_MILLSTONE_SEAT.get()).build(null);
        MillstoneSeatBlock.BLOCK_ENTITY_TYPES.put(SeatMaterial.COPPER, type);
        return type;
    });

    public static final RegistryObject<BlockEntityType<DeployerSeatBlockEntity>> COPPER_DEPLOYER_SEAT_ENTITY = BLOCK_ENTITIES.register("copper_deployer_seat", () -> {
        BlockEntityType<DeployerSeatBlockEntity> type = BlockEntityType.Builder.of(
            (pos, state) -> new DeployerSeatBlockEntity(DeployerSeatBlock.BLOCK_ENTITY_TYPES.get(SeatMaterial.COPPER), pos, state, SeatMaterial.COPPER),
            COPPER_DEPLOYER_SEAT.get()).build(null);
        DeployerSeatBlock.BLOCK_ENTITY_TYPES.put(SeatMaterial.COPPER, type);
        return type;
    });

    // ==================== Brass variant ====================
    public static final RegistryObject<Block> BRASS_PRESS_SEAT = BLOCKS.register("brass_press_seat", () -> new PressSeatBlock(SeatMaterial.BRASS));
    public static final RegistryObject<Item> BRASS_PRESS_SEAT_ITEM = ITEMS.register("brass_press_seat", () -> new WorkerSeatBlockItem(BRASS_PRESS_SEAT.get(), SeatMaterial.BRASS, new Item.Properties()));

    public static final RegistryObject<Block> BRASS_MIXER_SEAT = BLOCKS.register("brass_mixer_seat", () -> new MixerSeatBlock(SeatMaterial.BRASS));
    public static final RegistryObject<Item> BRASS_MIXER_SEAT_ITEM = ITEMS.register("brass_mixer_seat", () -> new WorkerSeatBlockItem(BRASS_MIXER_SEAT.get(), SeatMaterial.BRASS, new Item.Properties()));

    public static final RegistryObject<Block> BRASS_SAW_SEAT = BLOCKS.register("brass_saw_seat", () -> new SawSeatBlock(SeatMaterial.BRASS));
    public static final RegistryObject<Item> BRASS_SAW_SEAT_ITEM = ITEMS.register("brass_saw_seat", () -> new WorkerSeatBlockItem(BRASS_SAW_SEAT.get(), SeatMaterial.BRASS, new Item.Properties()));

    public static final RegistryObject<Block> BRASS_MILLSTONE_SEAT = BLOCKS.register("brass_millstone_seat", () -> new MillstoneSeatBlock(SeatMaterial.BRASS));
    public static final RegistryObject<Item> BRASS_MILLSTONE_SEAT_ITEM = ITEMS.register("brass_millstone_seat", () -> new WorkerSeatBlockItem(BRASS_MILLSTONE_SEAT.get(), SeatMaterial.BRASS, new Item.Properties()));

    public static final RegistryObject<Block> BRASS_DEPLOYER_SEAT = BLOCKS.register("brass_deployer_seat", () -> new DeployerSeatBlock(SeatMaterial.BRASS));
    public static final RegistryObject<Item> BRASS_DEPLOYER_SEAT_ITEM = ITEMS.register("brass_deployer_seat", () -> new WorkerSeatBlockItem(BRASS_DEPLOYER_SEAT.get(), SeatMaterial.BRASS, new Item.Properties()));

    public static final RegistryObject<BlockEntityType<PressSeatBlockEntity>> BRASS_PRESS_SEAT_ENTITY = BLOCK_ENTITIES.register("brass_press_seat", () -> {
        BlockEntityType<PressSeatBlockEntity> type = BlockEntityType.Builder.of(
            (pos, state) -> new PressSeatBlockEntity(PressSeatBlock.BLOCK_ENTITY_TYPES.get(SeatMaterial.BRASS), pos, state, SeatMaterial.BRASS),
            BRASS_PRESS_SEAT.get()).build(null);
        PressSeatBlock.BLOCK_ENTITY_TYPES.put(SeatMaterial.BRASS, type);
        return type;
    });

    public static final RegistryObject<BlockEntityType<MixerSeatBlockEntity>> BRASS_MIXER_SEAT_ENTITY = BLOCK_ENTITIES.register("brass_mixer_seat", () -> {
        BlockEntityType<MixerSeatBlockEntity> type = BlockEntityType.Builder.of(
            (pos, state) -> new MixerSeatBlockEntity(MixerSeatBlock.BLOCK_ENTITY_TYPES.get(SeatMaterial.BRASS), pos, state, SeatMaterial.BRASS),
            BRASS_MIXER_SEAT.get()).build(null);
        MixerSeatBlock.BLOCK_ENTITY_TYPES.put(SeatMaterial.BRASS, type);
        return type;
    });

    public static final RegistryObject<BlockEntityType<SawSeatBlockEntity>> BRASS_SAW_SEAT_ENTITY = BLOCK_ENTITIES.register("brass_saw_seat", () -> {
        BlockEntityType<SawSeatBlockEntity> type = BlockEntityType.Builder.of(
            (pos, state) -> new SawSeatBlockEntity(SawSeatBlock.BLOCK_ENTITY_TYPES.get(SeatMaterial.BRASS), pos, state, SeatMaterial.BRASS),
            BRASS_SAW_SEAT.get()).build(null);
        SawSeatBlock.BLOCK_ENTITY_TYPES.put(SeatMaterial.BRASS, type);
        return type;
    });

    public static final RegistryObject<BlockEntityType<MillstoneSeatBlockEntity>> BRASS_MILLSTONE_SEAT_ENTITY = BLOCK_ENTITIES.register("brass_millstone_seat", () -> {
        BlockEntityType<MillstoneSeatBlockEntity> type = BlockEntityType.Builder.of(
            (pos, state) -> new MillstoneSeatBlockEntity(MillstoneSeatBlock.BLOCK_ENTITY_TYPES.get(SeatMaterial.BRASS), pos, state, SeatMaterial.BRASS),
            BRASS_MILLSTONE_SEAT.get()).build(null);
        MillstoneSeatBlock.BLOCK_ENTITY_TYPES.put(SeatMaterial.BRASS, type);
        return type;
    });

    public static final RegistryObject<BlockEntityType<DeployerSeatBlockEntity>> BRASS_DEPLOYER_SEAT_ENTITY = BLOCK_ENTITIES.register("brass_deployer_seat", () -> {
        BlockEntityType<DeployerSeatBlockEntity> type = BlockEntityType.Builder.of(
            (pos, state) -> new DeployerSeatBlockEntity(DeployerSeatBlock.BLOCK_ENTITY_TYPES.get(SeatMaterial.BRASS), pos, state, SeatMaterial.BRASS),
            BRASS_DEPLOYER_SEAT.get()).build(null);
        DeployerSeatBlock.BLOCK_ENTITY_TYPES.put(SeatMaterial.BRASS, type);
        return type;
    });

    // ==================== Creative variant ====================
    public static final RegistryObject<Block> CREATIVE_PRESS_SEAT = BLOCKS.register("creative_press_seat", () -> new PressSeatBlock(SeatMaterial.CREATIVE));
    public static final RegistryObject<Item> CREATIVE_PRESS_SEAT_ITEM = ITEMS.register("creative_press_seat", () -> new WorkerSeatBlockItem(CREATIVE_PRESS_SEAT.get(), SeatMaterial.CREATIVE, new Item.Properties().rarity(Rarity.EPIC)));

    public static final RegistryObject<Block> CREATIVE_MIXER_SEAT = BLOCKS.register("creative_mixer_seat", () -> new MixerSeatBlock(SeatMaterial.CREATIVE));
    public static final RegistryObject<Item> CREATIVE_MIXER_SEAT_ITEM = ITEMS.register("creative_mixer_seat", () -> new WorkerSeatBlockItem(CREATIVE_MIXER_SEAT.get(), SeatMaterial.CREATIVE, new Item.Properties().rarity(Rarity.EPIC)));

    public static final RegistryObject<Block> CREATIVE_SAW_SEAT = BLOCKS.register("creative_saw_seat", () -> new SawSeatBlock(SeatMaterial.CREATIVE));
    public static final RegistryObject<Item> CREATIVE_SAW_SEAT_ITEM = ITEMS.register("creative_saw_seat", () -> new WorkerSeatBlockItem(CREATIVE_SAW_SEAT.get(), SeatMaterial.CREATIVE, new Item.Properties().rarity(Rarity.EPIC)));

    public static final RegistryObject<Block> CREATIVE_MILLSTONE_SEAT = BLOCKS.register("creative_millstone_seat", () -> new MillstoneSeatBlock(SeatMaterial.CREATIVE));
    public static final RegistryObject<Item> CREATIVE_MILLSTONE_SEAT_ITEM = ITEMS.register("creative_millstone_seat", () -> new WorkerSeatBlockItem(CREATIVE_MILLSTONE_SEAT.get(), SeatMaterial.CREATIVE, new Item.Properties().rarity(Rarity.EPIC)));

    public static final RegistryObject<Block> CREATIVE_DEPLOYER_SEAT = BLOCKS.register("creative_deployer_seat", () -> new DeployerSeatBlock(SeatMaterial.CREATIVE));
    public static final RegistryObject<Item> CREATIVE_DEPLOYER_SEAT_ITEM = ITEMS.register("creative_deployer_seat", () -> new WorkerSeatBlockItem(CREATIVE_DEPLOYER_SEAT.get(), SeatMaterial.CREATIVE, new Item.Properties().rarity(Rarity.EPIC)));

    public static final RegistryObject<BlockEntityType<PressSeatBlockEntity>> CREATIVE_PRESS_SEAT_ENTITY = BLOCK_ENTITIES.register("creative_press_seat", () -> {
        BlockEntityType<PressSeatBlockEntity> type = BlockEntityType.Builder.of(
            (pos, state) -> new PressSeatBlockEntity(PressSeatBlock.BLOCK_ENTITY_TYPES.get(SeatMaterial.CREATIVE), pos, state, SeatMaterial.CREATIVE),
            CREATIVE_PRESS_SEAT.get()).build(null);
        PressSeatBlock.BLOCK_ENTITY_TYPES.put(SeatMaterial.CREATIVE, type);
        return type;
    });

    public static final RegistryObject<BlockEntityType<MixerSeatBlockEntity>> CREATIVE_MIXER_SEAT_ENTITY = BLOCK_ENTITIES.register("creative_mixer_seat", () -> {
        BlockEntityType<MixerSeatBlockEntity> type = BlockEntityType.Builder.of(
            (pos, state) -> new MixerSeatBlockEntity(MixerSeatBlock.BLOCK_ENTITY_TYPES.get(SeatMaterial.CREATIVE), pos, state, SeatMaterial.CREATIVE),
            CREATIVE_MIXER_SEAT.get()).build(null);
        MixerSeatBlock.BLOCK_ENTITY_TYPES.put(SeatMaterial.CREATIVE, type);
        return type;
    });

    public static final RegistryObject<BlockEntityType<SawSeatBlockEntity>> CREATIVE_SAW_SEAT_ENTITY = BLOCK_ENTITIES.register("creative_saw_seat", () -> {
        BlockEntityType<SawSeatBlockEntity> type = BlockEntityType.Builder.of(
            (pos, state) -> new SawSeatBlockEntity(SawSeatBlock.BLOCK_ENTITY_TYPES.get(SeatMaterial.CREATIVE), pos, state, SeatMaterial.CREATIVE),
            CREATIVE_SAW_SEAT.get()).build(null);
        SawSeatBlock.BLOCK_ENTITY_TYPES.put(SeatMaterial.CREATIVE, type);
        return type;
    });

    public static final RegistryObject<BlockEntityType<MillstoneSeatBlockEntity>> CREATIVE_MILLSTONE_SEAT_ENTITY = BLOCK_ENTITIES.register("creative_millstone_seat", () -> {
        BlockEntityType<MillstoneSeatBlockEntity> type = BlockEntityType.Builder.of(
            (pos, state) -> new MillstoneSeatBlockEntity(MillstoneSeatBlock.BLOCK_ENTITY_TYPES.get(SeatMaterial.CREATIVE), pos, state, SeatMaterial.CREATIVE),
            CREATIVE_MILLSTONE_SEAT.get()).build(null);
        MillstoneSeatBlock.BLOCK_ENTITY_TYPES.put(SeatMaterial.CREATIVE, type);
        return type;
    });

    public static final RegistryObject<BlockEntityType<DeployerSeatBlockEntity>> CREATIVE_DEPLOYER_SEAT_ENTITY = BLOCK_ENTITIES.register("creative_deployer_seat", () -> {
        BlockEntityType<DeployerSeatBlockEntity> type = BlockEntityType.Builder.of(
            (pos, state) -> new DeployerSeatBlockEntity(DeployerSeatBlock.BLOCK_ENTITY_TYPES.get(SeatMaterial.CREATIVE), pos, state, SeatMaterial.CREATIVE),
            CREATIVE_DEPLOYER_SEAT.get()).build(null);
        DeployerSeatBlock.BLOCK_ENTITY_TYPES.put(SeatMaterial.CREATIVE, type);
        return type;
    });

    // ==================== Items ====================
    public static final RegistryObject<Item> VILLAGER_BINDER = ITEMS.register("villager_binder", VillagerBinderItem::new);

    // ==================== Sounds ====================
    public static final RegistryObject<SoundEvent> DEPLOYER_WORK_SOUND = SOUND_EVENTS.register("deployer_work", () -> SoundEvent.createVariableRangeEvent(new ResourceLocation(MODID, "deployer_work")));
    public static final RegistryObject<SoundEvent> PRESS_WORK_SOUND = SOUND_EVENTS.register("press_work", () -> SoundEvent.createVariableRangeEvent(new ResourceLocation(MODID, "press_work")));
    public static final RegistryObject<SoundEvent> MILLSTONE_WORK_SOUND = SOUND_EVENTS.register("millstone_work", () -> SoundEvent.createVariableRangeEvent(new ResourceLocation(MODID, "millstone_work")));
    public static final RegistryObject<SoundEvent> SAW_WORK_SOUND = SOUND_EVENTS.register("saw_work", () -> SoundEvent.createVariableRangeEvent(new ResourceLocation(MODID, "saw_work")));

    // ==================== Creative Tab ====================
    public static final RegistryObject<CreativeModeTab> CREATE_LABOR_TAB = CREATIVE_MODE_TABS.register("create_labor", () -> CreativeModeTab.builder()
            .title(Component.translatable("itemGroup.create_labor"))
            .icon(() -> ANDESITE_PRESS_SEAT_ITEM.get().getDefaultInstance())
            .displayItems((parameters, output) -> {
                // 冲压 Press
                output.accept(ANDESITE_PRESS_SEAT_ITEM.get());
                output.accept(COPPER_PRESS_SEAT_ITEM.get());
                output.accept(BRASS_PRESS_SEAT_ITEM.get());
                output.accept(CREATIVE_PRESS_SEAT_ITEM.get());
                // 切割 Saw
                output.accept(ANDESITE_SAW_SEAT_ITEM.get());
                output.accept(COPPER_SAW_SEAT_ITEM.get());
                output.accept(BRASS_SAW_SEAT_ITEM.get());
                output.accept(CREATIVE_SAW_SEAT_ITEM.get());
                // 研磨 Millstone
                output.accept(ANDESITE_MILLSTONE_SEAT_ITEM.get());
                output.accept(COPPER_MILLSTONE_SEAT_ITEM.get());
                output.accept(BRASS_MILLSTONE_SEAT_ITEM.get());
                output.accept(CREATIVE_MILLSTONE_SEAT_ITEM.get());
                // 搅拌 Mixer
                output.accept(ANDESITE_MIXER_SEAT_ITEM.get());
                output.accept(COPPER_MIXER_SEAT_ITEM.get());
                output.accept(BRASS_MIXER_SEAT_ITEM.get());
                output.accept(CREATIVE_MIXER_SEAT_ITEM.get());
                // 组合 Deployer
                output.accept(ANDESITE_DEPLOYER_SEAT_ITEM.get());
                output.accept(COPPER_DEPLOYER_SEAT_ITEM.get());
                output.accept(BRASS_DEPLOYER_SEAT_ITEM.get());
                output.accept(CREATIVE_DEPLOYER_SEAT_ITEM.get());
                // 工具
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

    // 渲染器注册已移至 CreateVillagerLaborClient（客户端专用类）
}
