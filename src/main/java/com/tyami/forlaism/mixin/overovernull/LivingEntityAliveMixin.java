package com.tyami.forlaism.mixin.overovernull;

import com.tyami.forlaism.erase.EraseRegistry;
import com.tyami.forlaism.erase.EraseTracker;

import net.minecraft.world.entity.LivingEntity;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;

/**
 * OverOverNull 実行中 / 抹消済み は isAlive / isDeadOrDying を false にする。
 *
 * MixinExtras の @ModifyReturnValue を使用。
 * （Forge 1.20.1 に標準搭載）
 */
@Mixin(LivingEntity.class)
public abstract class LivingEntityAliveMixin {

    @ModifyReturnValue(
            method = "isAlive",
            at = @At("RETURN")
    )
    private boolean forlaism$overrideIsAlive(boolean original) {
        LivingEntity self = (LivingEntity) (Object) this;

        // 実行中 or 抹消済み → false
        if (EraseTracker.isExecuting()) return false;

        if (self.level() != null
                && !self.level().isClientSide
                && self.level().getServer() != null
                && EraseRegistry.isErased(
                        self.level().getServer(),
                        self.getUUID()
                )) {
            return false;
        }

        return original;
    }

    @ModifyReturnValue(
            method = "isDeadOrDying",
            at = @At("RETURN")
    )
    private boolean forlaism$overrideIsDeadOrDying(boolean original) {
        LivingEntity self = (LivingEntity) (Object) this;

        if (EraseTracker.isExecuting()) return true;

        if (self.level() != null
                && !self.level().isClientSide
                && self.level().getServer() != null
                && EraseRegistry.isErased(
                        self.level().getServer(),
                        self.getUUID()
                )) {
            return true;
        }

        return original;
    }
}