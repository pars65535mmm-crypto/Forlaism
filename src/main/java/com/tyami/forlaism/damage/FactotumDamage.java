package com.tyami.forlaism.damage;

import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

public final class FactotumDamage {

    private FactotumDamage() {
    }

    /**
     * 万能なる君主の灯笏に関連する統一された独自ダメージ処理。
     * 既存のMinecraftの無敵フレームを一時的にバイパスし、確実にダメージを適用する。
     *
     * @param target ダメージ対象
     * @param attacker 攻撃者（プレイヤー）
     * @param directSource 実際の攻撃Entity（斬撃、眷属、幻影など）
     * @param amount ダメージ量
     * @return ダメージが適用されたかどうか
     */
    public static boolean dealDamage(LivingEntity target, Player attacker, Entity directSource, float amount) {
        if (target == null || target.isDeadOrDying() || target.level().isClientSide) {
            return false;
        }

        // プレイヤー自身への自傷は無効化
        if (target == attacker) {
            return false;
        }

        DamageSource source;
        if (attacker != null) {
            if (directSource != null && directSource != attacker) {
                source = target.damageSources().indirectMagic(directSource, attacker);
            } else {
                source = target.damageSources().playerAttack(attacker);
            }
        } else {
            source = target.damageSources().generic();
        }

        // 無敵時間の一時解除（連撃・斬撃・眷属の殺到が確実にヒットするようにする）
        int prevInvulnerableTime = target.invulnerableTime;
        target.invulnerableTime = 0;

        boolean success = target.hurt(source, amount);

        // 如果 hurt 被抵消或未完全造成伤害，且生命值大于0，且是独自Damage，做一次HP扣除保底
        if (!success && !target.isInvulnerable() && !(target instanceof Player p && p.isCreative())) {
            float currentHealth = target.getHealth();
            target.setHealth(Math.max(0.0F, currentHealth - amount));
            if (target.getHealth() <= 0.0F && !target.isDeadOrDying()) {
                target.die(source);
            }
            success = true;
        } else if (success) {
            // 被弾後の短縮無敵時間（多段ヒットがスムーズに入るよう短縮）
            target.invulnerableTime = Math.min(5, target.invulnerableTime);
        } else {
            target.invulnerableTime = prevInvulnerableTime;
        }

        return success;
    }

    /**
     * 直接プレイヤー攻撃用
     */
    public static boolean dealPlayerMeleeDamage(LivingEntity target, Player attacker, float amount) {
        return dealDamage(target, attacker, attacker, amount);
    }
}
