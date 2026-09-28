package com.yyn.labor;

import net.neoforged.neoforge.common.ModConfigSpec;

public class Config {
    public static final ModConfigSpec SPEC;
    public static final ModConfigSpec.BooleanValue ENABLE_TLM_CHAT_BUBBLE;
    public static final ModConfigSpec.IntValue TLM_CHAT_BUBBLE_INTERVAL;
    public static final ModConfigSpec.IntValue EMC_FE_PER_EMC;
    public static final ModConfigSpec.IntValue EMC_SELLER_ENERGY_CAPACITY;
    public static final ModConfigSpec.IntValue EMC_SELLER_MAX_RECEIVE;

    static {
        ModConfigSpec.Builder builder = new ModConfigSpec.Builder();

        builder.comment("Create Villager Labor - TLM Chat Bubble Compatibility")
            .push("tlm_chat_bubble");

        ENABLE_TLM_CHAT_BUBBLE = builder
            .comment("Enable Touhou Little Maid chat bubble compatibility when maids are working.",
                    "When enabled, maids sitting at worker seats will display random chat bubbles.")
            .define("enable", true);

        TLM_CHAT_BUBBLE_INTERVAL = builder
            .comment("Interval (in ticks) between chat bubble updates when a maid is working.",
                    "Default: 100 ticks (5 seconds). Lower values = more frequent bubbles.")
            .defineInRange("interval", 100, 20, 600);

        builder.pop();

        builder.comment("Create Villager Labor - EMC (ProjectE) Integration")
            .push("emc");

        EMC_FE_PER_EMC = builder
            .comment("How many FE are worth 1 EMC when the EMC Seller converts energy.",
                    "Default: 1000, i.e. 1000 FE = 1 EMC (an iron ingot is 256 EMC).")
            .defineInRange("fe_per_emc", 1000, 1, 1000000);

        EMC_SELLER_ENERGY_CAPACITY = builder
            .comment("Internal FE buffer of the EMC Seller.",
                    "Default: 1000000 FE (1 MFE).")
            .defineInRange("seller_energy_capacity", 1000000, 1000, 100000000);

        EMC_SELLER_MAX_RECEIVE = builder
            .comment("Maximum FE the EMC Seller accepts per tick.",
                    "Prevents a huge power grid from dumping everything in a single tick.",
                    "Default: 1000000 FE (1 MFE).")
            .defineInRange("seller_max_receive", 1000000, 1, 100000000);

        builder.pop();

        SPEC = builder.build();
    }
}
