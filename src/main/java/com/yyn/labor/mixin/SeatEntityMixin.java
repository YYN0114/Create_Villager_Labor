package com.yyn.labor.mixin;

import com.simibubi.create.content.contraptions.actors.seat.SeatEntity;
import com.yyn.labor.blocks.WorkerSeatBlock;
import com.yyn.labor.entity.LaborEntity;
import com.yyn.labor.util.WorkerUtil;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.level.block.Block;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Mixin to adjust villager seat offsets on Create seats.
 * - Millenaire villagers: lowered by 0.5 to compensate for custom model height
 * - LaborEntity on WorkerSeatBlock: raised by 0.2
 * - Normal villagers on WorkerSeatBlock: lowered by 0.3 (net -0.3 from default, aligned with LaborEntity + 0.2 raise)
 */
@Mixin(SeatEntity.class)
public class SeatEntityMixin {

    @Inject(
        method = "getCustomEntitySeatOffset",
        at = @At("RETURN"),
        cancellable = true
    )
    private static void createLabor$adjustSeatOffset(Entity entity, CallbackInfoReturnable<Double> cir) {
        // 千年村庄村民：降低 0.5 格
        if (WorkerUtil.isMillenaireVillager(entity)) {
            cir.setReturnValue(-0.5);
            return;
        }

        // 村民坐在工位上时调整高度
        if (entity instanceof Villager) {
            Entity vehicle = entity.getVehicle();
            if (vehicle != null) {
                Block block = entity.level().getBlockState(vehicle.blockPosition()).getBlock();
                if (block instanceof WorkerSeatBlock) {
                    if (entity instanceof LaborEntity) {
                        // LaborEntity：加高 0.2
                        cir.setReturnValue(0.2);
                    } else {
                        // 正常村民：-0.5（对齐 LaborEntity）+ 0.2（加高）= -0.3
                        cir.setReturnValue(-0.3);
                    }
                }
            }
        }
    }
}
