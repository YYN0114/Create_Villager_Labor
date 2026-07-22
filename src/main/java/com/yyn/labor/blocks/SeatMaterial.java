package com.yyn.labor.blocks;

/**
 * Represents the material variant of a worker seat.
 * ANDESITE - base version (16 RPM), COPPER - upgraded (64 RPM),
 * BRASS - upgraded (128 RPM), CREATIVE - creative-only (instant).
 */
public enum SeatMaterial {
    ANDESITE("andesite", 16, 1),
    COPPER("copper", 64, 2),
    BRASS("brass", 128, 4),
    CREATIVE("creative", Integer.MAX_VALUE, 64);

    private final String prefix;
    private final int rpm;
    private final int batchSize;

    SeatMaterial(String prefix, int rpm, int batchSize) {
        this.prefix = prefix;
        this.rpm = rpm;
        this.batchSize = batchSize;
    }

    public String getPrefix() {
        return prefix;
    }

    public int getRpm() {
        return rpm;
    }

    /**
     * 单次处理从输入容器提取的物品数量。
     * 安山=1，铜=2，黄铜=4，创造=64。
     */
    public int getBatchSize() {
        return batchSize;
    }

    /**
     * 根据安山版基础 maxCooldown 计算当前材质的 maxCooldown。
     * 安山=1x，铜=1/4，黄铜=1/8，创造=1（最小值）。
     */
    public int scaleCooldown(int baseCooldown) {
        return switch (this) {
            case ANDESITE -> baseCooldown;
            case COPPER -> Math.max(1, baseCooldown / 4);
            case BRASS -> Math.max(1, baseCooldown / 8);
            case CREATIVE -> 1;
        };
    }
}
