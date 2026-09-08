package com.yyn.labor.blocks;

/**
 * 标记工位方块拥有 SeatMaterial，用于区分普通版/创造版。
 * 创造版工位拆除时不掉落方块本体。
 */
public interface IHasSeatMaterial {
    SeatMaterial getMaterial();
}
