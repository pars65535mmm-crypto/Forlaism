package com.tyami.forlaism.entity;

import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.phys.Vec3;

/**
 * EndWarden のパーティクル送信を集約するヘルパ。
 *
 * 旧実装では数十箇所で個別に sendParticles を呼んでいたが、
 * 「1tick分 = 1メソッド呼び出し」に統一する。
 *
 * 同じ座標に複数種類のパーティクルを出す場合も、
 * この中でまとめて送ることで sendParticles の呼び出し回数を削減する。
 */
public final class EndWardenParticleHelper {

    private EndWardenParticleHelper() {
    }

    // =========================================================
    // ソニックブーム予兆（チャージ中）
    // =========================================================

    /**
     * チャージ段階に応じた予兆パーティクル。
     *
     * @param level    サーバーレベル
     * @param eyePos   目の位置
     * @param look     視線方向
     * @param progress チャージ進行度 0.0 ～ 1.0
     */
    public static void spawnCharge(ServerLevel level, Vec3 eyePos, Vec3 look, float progress) {

        Vec3 tip = eyePos.add(look.scale(1.5));

        // 進行度に応じて量をスケール
        int endRodCount = (int) (2 + progress * 6);
        int soulCount = (int) (1 + progress * 4);

        // END_ROD
        level.sendParticles(
                ParticleTypes.END_ROD,
                tip.x, tip.y, tip.z,
                endRodCount,
                0.4, 0.4, 0.4,
                0.05
        );

        // SOUL
        level.sendParticles(
                ParticleTypes.SOUL,
                tip.x, tip.y, tip.z,
                soulCount,
                0.4, 0.4, 0.4,
                0.02
        );

        // 進行度が高い時だけ SONIC_BOOM を派手に
        if (progress > 0.5F) {
            level.sendParticles(
                    ParticleTypes.SONIC_BOOM,
                    tip.x, tip.y, tip.z,
                    (int) (2 + progress * 4),
                    0.3, 0.3, 0.3,
                    0.0
            );
        }
    }

    // =========================================================
    // ソニックブーム発射（ビーム状）
    // =========================================================

    /**
     * 視線方向に伸びるビームを1tick分描画する。
     *
     * @param level     サーバーレベル
     * @param start     ビーム始点
     * @param look      方向
     * @param length    ビーム長（ブロック）
     * @param stepTicks 1tickあたりに進む距離（通常 1.0）
     * @param offset    ビーム内の現在位置オフセット
     */
    public static void spawnBeam(
            ServerLevel level,
            Vec3 start,
            Vec3 look,
            double length,
            double stepTicks,
            double offset
    ) {
        double t = 0;
        while (t < offset && t < length) {
            Vec3 p = start.add(look.scale(t));

            level.sendParticles(
                    ParticleTypes.SONIC_BOOM,
                    p.x, p.y, p.z,
                    2,
                    0.1, 0.1, 0.1,
                    0.0
            );
            level.sendParticles(
                    ParticleTypes.SCULK_SOUL,
                    p.x, p.y, p.z,
                    2,
                    0.1, 0.1, 0.1,
                    0.0
            );
            level.sendParticles(
                    ParticleTypes.EFFECT,
                    p.x, p.y, p.z,
                    1,
                    0.1, 0.1, 0.1,
                    0.0
            );

            t += stepTicks;
        }
    }

    // =========================================================
    // 突進（DIVE）の軌跡
    // =========================================================

    /**
     * 突進中の軌跡パーティクル。
     */
    public static void spawnDiveTrail(ServerLevel level, Vec3 pos) {
        level.sendParticles(
                ParticleTypes.END_ROD,
                pos.x, pos.y, pos.z,
                6,
                0.3, 0.3, 0.3,
                0.05
        );
        level.sendParticles(
                ParticleTypes.SCULK_SOUL,
                pos.x, pos.y, pos.z,
                3,
                0.3, 0.3, 0.3,
                0.02
        );
    }

    // =========================================================
    // 突進ヒット時のエフェクト
    // =========================================================

    /**
     * 突進が敵にヒットした瞬間のエフェクト。
     */
    public static void spawnDiveImpact(ServerLevel level, Vec3 pos) {
        level.sendParticles(
                ParticleTypes.SONIC_BOOM,
                pos.x, pos.y, pos.z,
                8,
                0.5, 0.5, 0.5,
                0.0
        );
        level.sendParticles(
                ParticleTypes.CRIT,
                pos.x, pos.y, pos.z,
                12,
                0.4, 0.4, 0.4,
                0.2
        );
    }

    // =========================================================
    // 召喚・消滅演出
    // =========================================================

    /**
     * スポーン時のオーラ。
     */
    public static void spawnAmbient(ServerLevel level, Vec3 pos) {
        level.sendParticles(
                ParticleTypes.SCULK_SOUL,
                pos.x, pos.y + 1.0, pos.z,
                5,
                1.5, 1.5, 1.5,
                0.02
        );
    }

    /**
     * 死亡時の爆発。
     */
    public static void spawnDeathBurst(ServerLevel level, Vec3 pos) {
        level.sendParticles(
                ParticleTypes.EXPLOSION_EMITTER,
                pos.x, pos.y + 1.0, pos.z,
                1,
                0, 0, 0,
                0
        );
        level.sendParticles(
                ParticleTypes.SCULK_SOUL,
                pos.x, pos.y + 1.0, pos.z,
                80,
                2.0, 2.0, 2.0,
                0.15
        );
        level.sendParticles(
                ParticleTypes.SONIC_BOOM,
                pos.x, pos.y + 1.0, pos.z,
                20,
                1.5, 1.5, 1.5,
                0.0
        );
    }

    // =========================================================
    // 汎用：汎用粒子
    // =========================================================

    /**
     * 汎用のパーティクル送信（1種類）。
     */
    public static void spawn(
            ServerLevel level,
            ParticleOptions particle,
            Vec3 pos,
            int count,
            double spread,
            double speed
    ) {
        level.sendParticles(
                particle,
                pos.x, pos.y, pos.z,
                count,
                spread, spread, spread,
                speed
        );
    }
}