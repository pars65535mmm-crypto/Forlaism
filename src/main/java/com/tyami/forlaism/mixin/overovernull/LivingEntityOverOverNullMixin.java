package com.tyami.forlaism.mixin.overovernull;

import com.tyami.forlaism.erase.EraseTracker;

import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * OverOverNull 実行中は、全防御を無効化する。
 *
 * priority = 30000 で、既存の全Mixin（Overnull=10000含む）より後に走る。
 */
@Mixin(value = LivingEntity.class, priority = 30000)
public abstract class LivingEntityOverOverNullMixin {

    // =========================================================
    // hurt: 強制成功
    // =========================================================

    @Inject(
            method = "hurt",
            at = @At("HEAD"),
            cancellable = true
    )
    private void forlaism$overOverNullHurt(
            DamageSource source,
            float amount,
            CallbackInfoReturnable<Boolean> cir
    ) {
        if (!EraseTracker.isExecuting()) return;

        LivingEntity self = (LivingEntity) (Object) this;
        if (self.level().isClientSide) return;

        // 無敵時間リセット
        self.invulnerableTime = 0;
        self.hurtTime = 0;

        // 強制HP0
        self.setHealth(0.0F);

        if (!self.isDeadOrDying()) {
            self.die(source);
        }

        cir.setReturnValue(true);
    }

    // =========================================================
    // setHealth: 増加を拒否、減少は許可
    // =========================================================

    @Inject(
            method = "setHealth",
            at = @At("HEAD"),
            cancellable = true
    )
    private void forlaism$overOverNullSetHealth(
            float health,
            CallbackInfo ci
    ) {
        if (!EraseTracker.isExecuting()) return;

        LivingEntity self = (LivingEntity) (Object) this;

        // 増加は拒否（Haloの保護対策）
        if (health > self.getHealth()) {
            ci.cancel();
        }
    }

    // =========================================================
    // die: キャンセルしない（素通し）
    // =========================================================

    @Inject(
            method = "die",
            at = @At("HEAD"),
            cancellable = true
    )
    private void forlaism$overOverNullDie(
            DamageSource source,
            CallbackInfo ci
    ) {
        if (!EraseTracker.isExecuting()) return;

        // die は素通し（キャンセルしない）
        // → 他Mixinがキャンセルしようとしても、priority で我々が後
    }

    // =========================================================
    // kill: 素通し
    // =========================================================

    @Inject(
            method = "kill",
            at = @At("HEAD"),
            cancellable = true
    )
    private void forlaism$overOverNullKill(
            CallbackInfo ci
    ) {
        if (!EraseTracker.isExecuting()) return;

        // kill も素通し
    }

    // =========================================================
    // removeEffect / removeAllEffects: 素通し
    // =========================================================

    @Inject(
            method = "removeEffect",
            at = @At("HEAD"),
            cancellable = true
    )
    private void forlaism$overOverNullRemoveEffect(
            net.minecraft.world.effect.MobEffect effect,
            CallbackInfoReturnable<Boolean> cir
    ) {
        if (!EraseTracker.isExecuting()) return;

        // エフェクト剥がしは許可
    }

    @Inject(
            method = "removeAllEffects",
            at = @At("HEAD"),
            cancellable = true
    )
    private void forlaism$overOverNullRemoveAllEffects(
            CallbackInfoReturnable<Boolean> cir
    ) {
        if (!EraseTracker.isExecuting()) return;

        // 素通し
    }
}