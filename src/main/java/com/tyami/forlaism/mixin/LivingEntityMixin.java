package com.tyami.forlaism.mixin;

import com.tyami.forlaism.item.HaloOfTheAdventAbility;
import com.tyami.forlaism.event.OnsenEffectHandler;
import com.tyami.forlaism.world.DreamDimension;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin {

    /**
     * All damage is rejected.
     *
     * This includes damage which would normally bypass invulnerability.
     */
    @Inject(
            method = "hurt",
            at = @At("HEAD"),
            cancellable = true
    )
    private void forlaism$cancelDamage(
            DamageSource source,
            float amount,
            CallbackInfoReturnable<Boolean> cir
    ) {

        LivingEntity self =
                (LivingEntity) (Object) this;

        if (OnsenEffectHandler.isInOnsen(self)) {
            cir.setReturnValue(false);
            return;
        }

        if (!(self instanceof Player player)) {
            return;
        }

        if (!HaloOfTheAdventAbility.isProtected(player)) {
            return;
        }

        cir.setReturnValue(false);
    }

    /**
     * Death itself becomes the time-reversal trigger.
     *
     * Instead of allowing LivingEntity#die to proceed,
     * restore the latest snapshot and cancel the death method.
     *
     * Dreamディメンションの場合:
     *   最大体力 -1 してその場で復活。
     *   最大体力が 1 になったら 0, -100000, 0 へ飛ばす。
     */
    @Inject(
            method = "die",
            at = @At("HEAD"),
            cancellable = true
    )
    private void forlaism$reverseDeath(
            DamageSource source,
            CallbackInfo ci
    ) {

        LivingEntity self =
                (LivingEntity) (Object) this;

        if (!(self instanceof ServerPlayer player)) {
            return;
        }

        // =========================================================
        // Dreamディメンションでの死亡
        // =========================================================
        if (player.level().dimension().equals(DreamDimension.DREAM_LEVEL)) {

            com.tyami.forlaism.event.DreamDeathHandler.handleDreamDeath(player, source);

            ci.cancel();
            return;
        }

        // =========================================================
        // Halo of the Advent 所持者
        // =========================================================
        if (!HaloOfTheAdventAbility.isProtected(player)) {
            return;
        }

        HaloOfTheAdventAbility.restoreSnapshot(player);

        ci.cancel();
    }

    /**
     * Normal effect application.
     */
    @Inject(
            method = "addEffect(Lnet/minecraft/world/effect/MobEffectInstance;)Z",
            at = @At("HEAD"),
            cancellable = true
    )
    private void forlaism$rejectEffect(
            MobEffectInstance effect,
            CallbackInfoReturnable<Boolean> cir
    ) {

        LivingEntity self =
                (LivingEntity) (Object) this;

        if (self instanceof Player player
                && HaloOfTheAdventAbility.isProtected(player)) {

            cir.setReturnValue(false);
        }
    }

    /**
     * Effect application with an entity source.
     */
    @Inject(
            method = "addEffect(Lnet/minecraft/world/effect/MobEffectInstance;Lnet/minecraft/world/entity/Entity;)Z",
            at = @At("HEAD"),
            cancellable = true
    )
    private void forlaism$rejectSourcedEffect(
            MobEffectInstance effect,
            Entity source,
            CallbackInfoReturnable<Boolean> cir
    ) {

        LivingEntity self =
                (LivingEntity) (Object) this;

        if (self instanceof Player player
                && HaloOfTheAdventAbility.isProtected(player)) {

            cir.setReturnValue(false);
        }
    }

    /**
     * forceAddEffect bypasses normal effect checks, so block it too.
     */
    @Inject(
            method = "forceAddEffect",
            at = @At("HEAD"),
            cancellable = true
    )
    private void forlaism$rejectForcedEffect(
            MobEffectInstance effect,
            Entity source,
            CallbackInfo ci
    ) {

        LivingEntity self =
                (LivingEntity) (Object) this;

        if (self instanceof Player player
                && HaloOfTheAdventAbility.isProtected(player)) {

            ci.cancel();
        }
    }

    /**
     * Prevent external code from removing the halo player's effects.
     *
     * The snapshot itself can still restore the player's state.
     */
    @Inject(
            method = "removeEffect",
            at = @At("HEAD"),
            cancellable = true
    )
    private void forlaism$rejectEffectRemoval(
            MobEffect effect,
            CallbackInfoReturnable<Boolean> cir
    ) {

        LivingEntity self =
                (LivingEntity) (Object) this;

        if (self instanceof Player player
                && HaloOfTheAdventAbility.isProtected(player)) {

            cir.setReturnValue(false);
        }
    }

    /**
     * Prevent all effect removal for the protected player.
     */
    @Inject(
            method = "removeAllEffects",
            at = @At("HEAD"),
            cancellable = true
    )
    private void forlaism$rejectAllEffectRemoval(
            CallbackInfoReturnable<Boolean> cir
    ) {

        LivingEntity self =
                (LivingEntity) (Object) this;

        if (self instanceof Player player
                && HaloOfTheAdventAbility.isProtected(player)) {

            cir.setReturnValue(false);
        }
    }

    /**
     * Prevent health reduction for the protected player.
     */
    @Inject(
            method = "setHealth",
            at = @At("HEAD"),
            cancellable = true
    )
    private void forlaism$preventHealthReduction(
            float health,
            CallbackInfo ci
    ) {

        LivingEntity self =
                (LivingEntity) (Object) this;

        if (!(self instanceof Player player)) {
            return;
        }

        if (!HaloOfTheAdventAbility.isProtected(player)) {
            return;
        }

        /*
         * Halo覚醒者のHPは外部から減少させない。
         */
        if (health < player.getHealth()) {
            ci.cancel();
        }
    }

    /**
     * =========================================================
     * 温泉中Mobのノックバック無効
     * =========================================================
     *
     * 温泉に入っているMobはノックバックされない。
     *
     * Playerは対象外。
     */
    @Inject(
            method = "knockback(DDD)V",
            at = @At("HEAD"),
            cancellable = true
    )
    private void forlaism$cancelOnsenKnockback(
            double strength,
            double ratioX,
            double ratioZ,
            CallbackInfo ci
    ) {

        LivingEntity self =
                (LivingEntity) (Object) this;

        /*
         * Playerは対象外。
         */
        if (self instanceof Player) {
            return;
        }

        /*
         * Mob以外は対象外。
         */
        if (!(self instanceof Mob)) {
            return;
        }

        /*
         * 温泉に入っているMobだけノックバックを拒否。
         */
        if (!OnsenEffectHandler.isInOnsen(self)) {
            return;
        }

        /*
         * ノックバック処理を完全キャンセル。
         */
        ci.cancel();
    }
}