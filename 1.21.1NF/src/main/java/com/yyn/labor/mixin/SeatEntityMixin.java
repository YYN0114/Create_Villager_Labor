package com.yyn.labor.mixin;

import com.simibubi.create.content.contraptions.actors.seat.SeatEntity;
import com.yyn.labor.util.WorkerUtil;

import net.minecraft.world.entity.Entity;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Mixin to lower Millenaire villager models by 0.5 blocks when sitting on any Create seat.
 * This compensates for Millenaire's custom model height which sits too high on Create seats.
 */
@Mixin(SeatEntity.class)
public class SeatEntityMixin {

    @Inject(
        method = "getCustomEntitySeatOffset",
        at = @At("RETURN"),
        cancellable = true
    )
    private static void createLabor$lowerMillenaireVillager(Entity entity, CallbackInfoReturnable<Double> cir) {
        if (WorkerUtil.isMillenaireVillager(entity)) {
            cir.setReturnValue(-0.5);
        }
    }
}
