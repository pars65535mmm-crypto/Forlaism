package com.tyami.forlaism.mixin.gmb;

import com.tyami.forlaism.annihilation.GMBEraseRegistry;

import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * GMB消去済みLivingEntityは完全に無力。
 *
 * priority = 30000 で全Mixinの最終段。
 *
 * ※ startRiding は Entity 側のメソッドなので、
 *    EntityGMBAbsoluteEraseMixin に移動済み。
 */
@Mixin(value = LivingEntity.class, priority = 30000)
public abstract class LivingEntityGMBAbsoluteEraseMixin {

    private boolean forlaism$isGMBErased() {
        LivingEntity self = (LivingEntity) (Object) this;
        if (self.level() == null) return false;
        if (self.level().getServer() == null) return false;
        return GMBEraseRegistry.isErased(self.level().getServer(), self.getUUID());
    }

    /**
     * hurt: 消去済みは絶対に成功させない（無敵化）
     */
    @Inject(method = "hurt", at = @At("HEAD"), cancellable = true)
    private void forlaism$gmbBlockHurt(DamageSource source, float amount, CallbackInfoReturnable<Boolean> cir) {
        if (forlaism$isGMBErased()) {
            cir.setReturnValue(false);
        }
    }

    /**
     * setHealth: 増加拒否
     */
    @Inject(method = "setHealth", at = @At("HEAD"), cancellable = true)
    private void forlaism$gmbSetHealth(float health, CallbackInfo ci) {
        if (forlaism$isGMBErased()) {
            LivingEntity self = (LivingEntity) (Object) this;
            if (health > self.getHealth()) {
                ci.cancel();
            }
        }
    }

    /**
     * heal: 完全拒否
     */
    @Inject(method = "heal", at = @At("HEAD"), cancellable = true)
    private void forlaism$gmbHeal(float amount, CallbackInfo ci) {
        if (forlaism$isGMBErased()) {
            ci.cancel();
        }
    }

    /**
     * addEffect: 拒否
     */
    @Inject(method = "addEffect(Lnet/minecraft/world/effect/MobEffectInstance;)Z",
            at = @At("HEAD"), cancellable = true)
    private void forlaism$gmbAddEffect(net.minecraft.world.effect.MobEffectInstance e, CallbackInfoReturnable<Boolean> cir) {
        if (forlaism$isGMBErased()) {
            cir.setReturnValue(false);
        }
    }

    @Inject(method = "addEffect(Lnet/minecraft/world/effect/MobEffectInstance;Lnet/minecraft/world/entity/Entity;)Z",
            at = @At("HEAD"), cancellable = true)
    private void forlaism$gmbAddEffectSourced(net.minecraft.world.effect.MobEffectInstance e,
                                              net.minecraft.world.entity.Entity src,
                                              CallbackInfoReturnable<Boolean> cir) {
        if (forlaism$isGMBErased()) {
            cir.setReturnValue(false);
        }
    }

    @Inject(method = "forceAddEffect", at = @At("HEAD"), cancellable = true)
    private void forlaism$gmbForceAddEffect(net.minecraft.world.effect.MobEffectInstance e,
                                            net.minecraft.world.entity.Entity src,
                                            CallbackInfo ci) {
        if (forlaism$isGMBErased()) {
            ci.cancel();
        }
    }
}