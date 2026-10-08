package com.tyami.forlaism.mixin;

import com.tyami.forlaism.world.ChronosSlowManager;

import net.minecraft.world.entity.Mob;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = Mob.class, priority = 1500)
public abstract class ChronosMobSlowMixin {

    @Inject(method = "serverAiStep", at = @At("HEAD"), cancellable = true)
    private void forlaism$chronosServerAi(CallbackInfo ci) {
        Mob self = (Mob) (Object) this;
        if (self.level().isClientSide) return;
        if (!ChronosSlowManager.isSlowed(self)) return;
        if (ChronosSlowManager.shouldPassTick(self)) return;
        ci.cancel();
    }
}