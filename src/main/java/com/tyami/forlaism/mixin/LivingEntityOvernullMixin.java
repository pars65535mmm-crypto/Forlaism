package com.tyami.forlaism.mixin;

import com.tyami.forlaism.util.Overnull;

import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Overnull 実行中は、対象に飛んでくる防御を全部無効化する。
 *
 * priority = 10000 で、既存の全防御 Mixin より後に走らせる。
 *
 * これにより:
 *  - Halo の setHealth 減少拒否 (priority default) を上書き
 *  - Halo の die キャンセル (priority default) を上書き
 *  - Freeze の hurt 拒否 (priority 2500) を上書き
 *  - Swrequimet / Madoromi / Rinne より後で確定させる
 */
@Mixin(value = LivingEntity.class, priority = 10000)
public abstract class LivingEntityOvernullMixin {

    // =========================================================
    // hurt
    // =========================================================

    @Inject(
            method = "hurt",
            at = @At("HEAD"),
            cancellable = true
    )
    private void forlaism$overnullHurt(
            DamageSource source,
            float amount,
            CallbackInfoReturnable<Boolean> cir
    ) {
        if (!Overnull.isExecuting()) return;

        LivingEntity self = (LivingEntity) (Object) this;

        // クライアントは関係なし
        if (self.level().isClientSide) return;

        // 確定ダメージを通す
        self.invulnerableTime = 0;
        self.hurtTime = 0;

        // hurt() 本体はスキップして、setHealth を直接叩く
        // （hurt() の内部ロジックで弾かれる可能性を消す）
        self.setHealth(0.0F);

        if (!self.isDeadOrDying()) {
            self.die(source);
        }

        cir.setReturnValue(true);
    }

    // =========================================================
    // setHealth
    // =========================================================

    @Inject(
            method = "setHealth",
            at = @At("HEAD"),
            cancellable = true
    )
    private void forlaism$overnullSetHealth(
            float health,
            CallbackInfo ci
    ) {
        if (!Overnull.isExecuting()) return;

        LivingEntity self = (LivingEntity) (Object) this;

        // 減少は常に許可
        // 増加（Halo の保護）は拒否
        if (health > self.getHealth()) {
            ci.cancel();
        }
    }

    // =========================================================
    // die
    // =========================================================

    @Inject(
            method = "die",
            at = @At("HEAD"),
            cancellable = true
    )
    private void forlaism$overnullDie(
            DamageSource source,
            CallbackInfo ci
    ) {
        if (!Overnull.isExecuting()) return;

        // die は素通し（キャンセルしない）
        // → 各防御 Mixin の ci.cancel() より我々が後なら、ここは何もしないだけで通る
    }

    // =========================================================
    // kill
    // =========================================================

    @Inject(
            method = "kill",
            at = @At("HEAD"),
            cancellable = true
    )
    private void forlaism$overnullKill(
            CallbackInfo ci
    ) {
        if (!Overnull.isExecuting()) return;

        // kill も素通し
    }

    // =========================================================
    // removeEffect / removeAllEffects
    // =========================================================

    @Inject(
            method = "removeEffect",
            at = @At("HEAD"),
            cancellable = true
    )
    private void forlaism$overnullRemoveEffect(
            net.minecraft.world.effect.MobEffect effect,
            CallbackInfoReturnable<Boolean> cir
    ) {
        if (!Overnull.isExecuting()) return;

        // エフェクト剥がしは許可（素通し）
    }

    @Inject(
            method = "removeAllEffects",
            at = @At("HEAD"),
            cancellable = true
    )
    private void forlaism$overnullRemoveAllEffects(
            CallbackInfoReturnable<Boolean> cir
    ) {
        if (!Overnull.isExecuting()) return;

        // 素通し
    }
}