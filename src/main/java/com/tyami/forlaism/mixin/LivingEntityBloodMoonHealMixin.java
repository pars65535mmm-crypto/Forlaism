package com.tyami.forlaism.mixin;

import com.tyami.forlaism.world.BloodMoonManager;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * ブラッドムーン中、プレイヤーの HP 回復量を「ほんの少し」減らす。
 */
@Mixin(LivingEntity.class)
public abstract class LivingEntityBloodMoonHealMixin {

    @Inject(method = "heal", at = @At("HEAD"), cancellable = true)
    private void forlaism$bloodMoonReduceHeal(float amount, CallbackInfo ci) {
        LivingEntity self = (LivingEntity) (Object) this;

        if (self.level().isClientSide) return;
        if (!(self instanceof Player player)) return;
        if (!(self.level() instanceof net.minecraft.server.level.ServerLevel sl)) return;
        if (!BloodMoonManager.isActive(sl)) return;

        // 「ほんの少し」= 15% 減
        float reduced = amount * 0.85F;

        // 元の heal を呼び直す
        self.setHealth(Math.min(self.getMaxHealth(), self.getHealth() + reduced));
        ci.cancel();
    }
}