package com.tyami.forlaism.entity;

import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.phys.Vec3;

/**
 * エンディストウォーデンのパーティクル集約。
 */
public final class EndWardenFinalParticleHelper {

    private EndWardenFinalParticleHelper() {}

    /** 虚無の衣（常時オーラ）。 */
    public static void spawnVoidCloak(ServerLevel level, Vec3 pos) {
        level.sendParticles(
                ParticleTypes.SCULK_SOUL,
                pos.x, pos.y + 1.5, pos.z,
                8, 1.2, 1.8, 1.2, 0.02
        );
        level.sendParticles(
                ParticleTypes.SQUID_INK,
                pos.x, pos.y + 1.5, pos.z,
                3, 1.0, 1.5, 1.0, 0.01
        );
    }

    /** 第二形態: TP攻撃の溜め。 */
    public static void spawnTpCharge(ServerLevel level, Vec3 eye, Vec3 look, float progress) {
        Vec3 tip = eye.add(look.scale(2.0));

        level.sendParticles(
                ParticleTypes.SONIC_BOOM,
                tip.x, tip.y, tip.z,
                (int) (3 + progress * 15),
                0.3, 0.3, 0.3, 0.0
        );
        level.sendParticles(
                ParticleTypes.SCULK_SOUL,
                tip.x, tip.y, tip.z,
                (int) (2 + progress * 10),
                0.5, 0.5, 0.5, 0.05
        );
    }

    /** 第二形態: 反射可能なレーザー。 */
    public static void spawnReflectiveBeam(ServerLevel level, Vec3 start, Vec3 end) {
        Vec3 diff = end.subtract(start);
        double dist = diff.length();
        if (dist < 0.01) return;
        Vec3 dir = diff.normalize();

        double step = 0.5;
        for (double d = 0; d < dist; d += step) {
            Vec3 p = start.add(dir.scale(d));
            level.sendParticles(
                    ParticleTypes.SONIC_BOOM,
                    p.x, p.y, p.z,
                    1, 0.05, 0.05, 0.05, 0.0
            );
            level.sendParticles(
                    ParticleTypes.END_ROD,
                    p.x, p.y, p.z,
                    1, 0.05, 0.05, 0.05, 0.0
            );
        }
    }

    /** 第三形態: 属性乱射の予兆。 */
    public static void spawnElementalHint(ServerLevel level, Vec3 pos, int type) {
        ParticleOptions particle = switch (type % 5) {
            case 0 -> ParticleTypes.FLAME;            // 火
            case 1 -> ParticleTypes.SNOWFLAKE;        // 氷
            case 2 -> ParticleTypes.ELECTRIC_SPARK;   // 雷
            case 3 -> ParticleTypes.EXPLOSION;        // TNT
            default -> ParticleTypes.DRAGON_BREATH;   // 隕石
        };
        level.sendParticles(
                particle,
                pos.x, pos.y + 1.5, pos.z,
                20, 1.0, 1.0, 1.0, 0.1
        );
    }

    /** 隕石の軌跡。 */
    public static void spawnMeteorTrail(ServerLevel level, Vec3 pos) {
        level.sendParticles(
                ParticleTypes.FLAME,
                pos.x, pos.y, pos.z,
                15, 0.5, 0.5, 0.5, 0.05
        );
        level.sendParticles(
                ParticleTypes.LAVA,
                pos.x, pos.y, pos.z,
                3, 0.3, 0.3, 0.3, 0.0
        );
        level.sendParticles(
                ParticleTypes.SMOKE,
                pos.x, pos.y, pos.z,
                10, 0.5, 0.5, 0.5, 0.02
        );
    }

    /** レーザー纏う隕石。 */
    public static void spawnLaserMeteorTrail(ServerLevel level, Vec3 pos) {
        level.sendParticles(
                ParticleTypes.SONIC_BOOM,
                pos.x, pos.y, pos.z,
                20, 0.8, 0.8, 0.8, 0.0
        );
        level.sendParticles(
                ParticleTypes.FLAME,
                pos.x, pos.y, pos.z,
                10, 0.5, 0.5, 0.5, 0.05
        );
    }

    /** 死亡時の大爆発。 */
    public static void spawnFinalDeath(ServerLevel level, Vec3 pos) {
        level.sendParticles(
                ParticleTypes.EXPLOSION_EMITTER,
                pos.x, pos.y + 1.0, pos.z,
                5, 2.0, 2.0, 2.0, 0
        );
        level.sendParticles(
                ParticleTypes.SCULK_SOUL,
                pos.x, pos.y + 1.0, pos.z,
                200, 3.0, 3.0, 3.0, 0.2
        );
        level.sendParticles(
                ParticleTypes.SONIC_BOOM,
                pos.x, pos.y + 1.0, pos.z,
                60, 2.0, 2.0, 2.0, 0
        );
    }
}