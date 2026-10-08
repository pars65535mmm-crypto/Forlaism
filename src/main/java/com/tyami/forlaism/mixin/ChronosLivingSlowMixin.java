package com.tyami.forlaism.mixin;

import com.tyami.forlaism.world.ChronosSlowManager;

import net.minecraft.world.entity.LivingEntity;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = LivingEntity.class, priority = 1500)
public abstract class ChronosLivingSlowMixin {

    @Inject(method = "aiStep", at = @At("HEAD"), cancellable = true)
    private void forlaism$chronosAiStep(CallbackInfo ci) {
        LivingEntity self = (LivingEntity) (Object) this;
        if (self.level().isClientSide) return;
        if (!ChronosSlowManager.isSlowed(self)) return;
        if (ChronosSlowManager.shouldPassTick(self)) return;
        ci.cancel();
    }
}