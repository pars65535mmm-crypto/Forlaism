package com.tyami.forlaism.mixin.gmb;

import com.tyami.forlaism.annihilation.GMBEraseRegistry;
import net.minecraft.server.level.ServerLevel;

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

    @Inject(method = "isAlive", at = @At("HEAD"), cancellable = true)
    private void forlaism$gmbNotAlive(CallbackInfoReturnable<Boolean> cir) {
        if (forlaism$isGMBErased()) cir.setReturnValue(false);
    }

    /**
     * hurt: 消去済みは絶対に成功させない（無敵化）
     */
    @Inject(method = "hurt", at = @At("HEAD"), cancellable = true)
    private void forlaism$gmbBlockHurt(DamageSource source, float amount, CallbackInfoReturnable<Boolean> cir) {
        if (forlaism$isGMBErased()) {
            cir.setReturnValue(false);
            return;
        }
        LivingEntity self = (LivingEntity) (Object) this;
        if (self.level() instanceof ServerLevel level
                && GMBEraseRegistry.isSuppressedAttack(level, source.getEntity(), level.getGameTime())) {
            // 再生成→攻撃→除去のレースより前に、ダメージ適用そのものを遮断する。
            cir.setReturnValue(false);
            return;
        }
        if (self.level() instanceof ServerLevel level
                && GMBEraseRegistry.isInSuppressionZone(level, self, level.getGameTime())) {
            // 独自Manager/独自DamageSourceが攻撃元情報を隠しても、封印領域では通さない。
            cir.setReturnValue(false);
        }
    }

    /**
     * setHealth: 増加拒否
     */
    @Inject(method = "setHealth", at = @At("HEAD"), cancellable = true)
    private void forlaism$gmbSetHealth(float health, CallbackInfo ci) {
        LivingEntity self = (LivingEntity) (Object) this;
        if (forlaism$isGMBErased()) {
            if (health > self.getHealth()) {
                ci.cancel();
            }
        } else if (self.level() instanceof ServerLevel level
                && health < self.getHealth()
                && GMBEraseRegistry.isInSuppressionZone(level, self, level.getGameTime())) {
            // hurt()を経由せずsetHealthを直接呼ぶ独自攻撃への対策。
            ci.cancel();
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
