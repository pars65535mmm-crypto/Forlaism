package com.tyami.forlaism.mixin;

import com.tyami.forlaism.world.ChronosSlowManager;

import net.minecraft.world.entity.Entity;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = Entity.class, priority = 1500)
public abstract class ChronosSlowMixin {

    @Shadow
    public int tickCount;

    @Inject(method = "tick", at = @At("HEAD"), cancellable = true)
    private void forlaism$chronosTick(CallbackInfo ci) {
        Entity self = (Entity) (Object) this;
        if (self.level() == null || self.level().isClientSide) return;
        if (!ChronosSlowManager.isSlowed(self)) return;
        if (ChronosSlowManager.shouldPassTick(self)) return;

        this.tickCount++;
        ci.cancel();
    }
}