package com.tyami.forlaism.mixin;

import com.tyami.forlaism.damage.RinneDamageSource;
import com.tyami.forlaism.damage.RinneRecoveryBlocker;

import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 輪廻ダメージの実処理。
 *
 * 優先度 2600 で、Halo やその他の Mixin より後に走らせる。
 * → 他が「hurt をキャンセルした」としても、輪廻だけは通す。
 *
 * 機能:
 *  - ダメージ計算: newHP = HP - amount
 *  - 最大HP削り（累積）
 *  - 無敵時間貫通
 *  - 防具・エンチャ・耐性 全無視
 *  - 回復阻害（永続・死亡まで解除されない）
 *  - 最大HP固定（setBaseValue を AttributeInstanceMixin で拒否）
 *  - クリエイティブ除外
 *  - 自傷除外
 */
@Mixin(value = LivingEntity.class, priority = 2600)
public abstract class LivingEntityRinneMixin {

    @Inject(method = "hurt", at = @At("HEAD"), cancellable = true)
    private void forlaism$rinneHurt(
            DamageSource source,
            float amount,
            CallbackInfoReturnable<Boolean> cir
    ) {
        LivingEntity target = (LivingEntity) (Object) this;

        // クライアント除外
        if (target.level().isClientSide) return;

        // 輪廻ダメージ以外は素通し
        if (!source.is(RinneDamageSource.RINNE)) return;

        // 自傷除外
        if (source.getEntity() == target) {
            cir.setReturnValue(false);
            return;
        }

        // クリエイティブ除外
        if (target instanceof Player p && p.isCreative()) {
            cir.setReturnValue(false);
            return;
        }

        // =========================================================
        // 1. 最大HPを amount 分削る
        // =========================================================
        AttributeInstance maxHp = target.getAttribute(Attributes.MAX_HEALTH);

        if (maxHp != null) {

            // まだ固定されていないなら、今の値を基準に固定する
            if (!RinneRecoveryBlocker.isMaxHealthLocked(maxHp)) {
                double currentMax = maxHp.getBaseValue();
                double nextMax = Math.max(0.0D, currentMax - amount);

                RinneRecoveryBlocker.lockMaxHealth(maxHp, nextMax);

            } else {
                // 固定済み：ロック値を amount 分削って再固定
                Double locked = RinneRecoveryBlocker.getLockedValue(maxHp);
                if (locked != null) {
                    double nextMax = Math.max(0.0D, locked - amount);
                    // 一度 unlock してから再ロック
                    RinneRecoveryBlocker.unlockMaxHealth(maxHp);
                    RinneRecoveryBlocker.lockMaxHealth(maxHp, nextMax);
                }
            }
        }

        // =========================================================
        // 2. 現在HPを amount 分削る
        // =========================================================
        float newHealth = Math.max(0.0F, target.getHealth() - amount);
        target.setHealth(newHealth);

        // =========================================================
        // 3. 回復阻害を付与（永続）
        // =========================================================
        RinneRecoveryBlocker.blockRecovery(target.getUUID());

        // =========================================================
        // 4. 被弾演出
        // =========================================================
        target.invulnerableTime = 0;
        target.hurtTime = 10;
        target.hurtDuration = 10;

        // =========================================================
        // 5. 発射者記録
        // =========================================================
        if (source.getEntity() instanceof Player p) {
            target.setLastHurtByPlayer(p);
        }

        // =========================================================
        // 6. 死亡判定
        // =========================================================
        if (newHealth <= 0.0F && !target.isDeadOrDying()) {
            target.die(source);
        }

        // バニラ hurt() をスキップ
        cir.setReturnValue(true);
    }

    // =========================================================
    // 回復阻害: heal()
    // =========================================================
    // バニラの heal(float) は void を返すので CallbackInfo を使う
    @Inject(method = "heal", at = @At("HEAD"), cancellable = true)
    private void forlaism$blockHeal(
            float amount,
            CallbackInfo ci
    ) {
        LivingEntity self = (LivingEntity) (Object) this;

        if (self.level().isClientSide) return;

        if (RinneRecoveryBlocker.isRecoveryBlocked(self.getUUID())) {
            ci.cancel();
        }
    }

    // =========================================================
    // 回復阻害: setHealth()（HP増加のみ拒否）
    // =========================================================
    @Inject(method = "setHealth", at = @At("HEAD"), cancellable = true)
    private void forlaism$blockSetHealthIncrease(
            float health,
            CallbackInfo ci
    ) {
        LivingEntity self = (LivingEntity) (Object) this;

        if (self.level().isClientSide) return;

        if (!RinneRecoveryBlocker.isRecoveryBlocked(self.getUUID())) {
            return;
        }

        // 現在HPより大きい値は拒否
        if (health > self.getHealth()) {
            ci.cancel();
        }
    }
}