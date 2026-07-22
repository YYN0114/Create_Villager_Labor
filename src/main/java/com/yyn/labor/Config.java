package com.yyn.labor;

import net.neoforged.neoforge.common.ModConfigSpec;

public class Config {
    public static final ModConfigSpec SPEC;
    public static final ModConfigSpec.BooleanValue ENABLE_TLM_CHAT_BUBBLE;
    public static final ModConfigSpec.IntValue TLM_CHAT_BUBBLE_INTERVAL;

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

        SPEC = builder.build();
    }
}
