package com.tyami.forlaism.entity;

import com.tyami.forlaism.damage.FrostbiteExecution;
import com.tyami.forlaism.damage.MoonlightDamageSource;

import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.List;
import java.util.Random;

/**
 * ノクスラティアの技実装。
 *
 * アニメーション苦手な分、全部パーティクルで表現する。
 * テキスト表示なし。
 */
public final class NoxslratiaSkillHandler {

    private NoxslratiaSkillHandler() {}

    private static final Random RANDOM = new Random();

    /**
     * 技を発動する。
     */
    public static void cast(NoxslratiaEntity boss, ServerLevel level, LivingEntity target, NoxslratiaSkill skill) {
        switch (skill) {
            case CRESCENT_SLASH -> castCrescentSlash(boss, level, target);
            case MOONLIGHT_WAVE -> castMoonlightWave(boss, level, target);
            case FULL_MOON_FLASH -> castFullMoonFlash(boss, level, target);
            case MOON_ECLIPSE -> castMoonEclipse(boss, level);
            case MOON_SHADOW_STEP -> castMoonShadowStep(boss, level, target);

            case ICE_FLOWER_BURIAL -> castIceFlowerBurial(boss, level, target);
            case ABSOLUTE_ZERO -> castAbsoluteZero(boss, level);
            case ICICLE_HELL -> castIcicleHell(boss, level, target);
            case FREEZING_SCYTHE -> castFreezingScythe(boss, level, target);
            case WHITE_NIGHT -> castWhiteNight(boss, level, target);
        }
    }

    // =========================================================
    // 月光系
    // =========================================================

    /**
     * 三日月斬り。
     * 三日月型のパーティクルが扇状に飛ぶ。
     */
    private static void castCrescentSlash(NoxslratiaEntity boss, ServerLevel level, LivingEntity target) {
        Vec3 look = boss.getLookAngle().normalize();
        Vec3 eye = boss.getEyePosition();
        Vec3 right = new Vec3(-look.z, 0, look.x).normalize();

        level.playSound(null, boss.getX(), boss.getY(), boss.getZ(),
                SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.HOSTILE, 1.5F, 0.8F);

        // 三日月の扇状軌跡
        for (int t = 0; t < 20; t++) {
            double dist = t * 1.2;
            double radius = 1.5 + dist * 0.05;

            // 三日月の外側
            for (int i = 0; i <= 16; i++) {
                double angle = Math.PI * (0.6 + (i / 16.0) * 0.8) - Math.PI / 2;
                Vec3 offset = right.scale(Math.cos(angle) * radius)
                        .add(0, Math.sin(angle) * radius * 0.4, 0);
                Vec3 p = eye.add(look.scale(dist)).add(offset);

                level.sendParticles(
                        ParticleTypes.END_ROD,
                        p.x, p.y, p.z,
                        1, 0, 0, 0, 0
                );
            }
        }

        // ダメージ
        hitCone(boss, level, target, 6.0, 90.0, 25.0F,
                MoonlightDamageSource.of(level, boss));
    }

    /**
     * 月光波。
     * 前方へ波状のリングが広がる。
     */
    private static void castMoonlightWave(NoxslratiaEntity boss, ServerLevel level, LivingEntity target) {
        Vec3 eye = boss.getEyePosition();
        Vec3 look = boss.getLookAngle().normalize();
        Vec3 right = new Vec3(-look.z, 0, look.x).normalize();
        Vec3 up = right.cross(look).normalize();

        level.playSound(null, boss.getX(), boss.getY(), boss.getZ(),
                SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.HOSTILE, 1.5F, 0.6F);

        // 波状に広がる円
        for (int wave = 0; wave < 6; wave++) {
            double dist = 2.0 + wave * 2.0;
            double radius = 0.5 + wave * 0.7;

            for (int i = 0; i < 48; i++) {
                double angle = (i / 48.0) * Math.PI * 2;
                Vec3 offset = right.scale(Math.cos(angle) * radius)
                        .add(up.scale(Math.sin(angle) * radius));
                Vec3 p = eye.add(look.scale(dist)).add(offset);

                level.sendParticles(
                        ParticleTypes.END_ROD,
                        p.x, p.y, p.z,
                        1, 0.02, 0.02, 0.02, 0.02
                );
            }
        }

        hitCone(boss, level, target, 16.0, 60.0, 20.0F,
                MoonlightDamageSource.of(level, boss));
    }

    /**
     * 満月の一閃。
     * 頭上に満月を描いて、直線ビームで斬る。
     */
    private static void castFullMoonFlash(NoxslratiaEntity boss, ServerLevel level, LivingEntity target) {
        Vec3 eye = boss.getEyePosition();
        Vec3 look = boss.getLookAngle().normalize();

        level.playSound(null, boss.getX(), boss.getY(), boss.getZ(),
                SoundEvents.WARDEN_SONIC_BOOM, SoundSource.HOSTILE, 2.0F, 1.0F);

        // 頭上に満月（白い円）
        Vec3 moonCenter = eye.add(0, 4.0, 0);
        for (int r = 0; r <= 32; r++) {
            for (int i = 0; i < 64; i++) {
                double angle = (i / 64.0) * Math.PI * 2;
                if (i % 2 != 0) continue;
                Vec3 p = moonCenter.add(Math.cos(angle) * r * 0.06, Math.sin(angle) * r * 0.06, 0);
                level.sendParticles(ParticleTypes.END_ROD, p.x, p.y, p.z, 1, 0, 0, 0, 0);
            }
        }

        // 直線ビーム
        for (double d = 0; d < 40.0; d += 0.3) {
            Vec3 p = eye.add(look.scale(d));
            level.sendParticles(ParticleTypes.END_ROD, p.x, p.y, p.z, 2, 0.05, 0.05, 0.05, 0.01);
            level.sendParticles(ParticleTypes.FLASH, p.x, p.y, p.z, 1, 0, 0, 0, 0);
        }

        // 直線状ダメージ
        for (double d = 0; d < 40.0; d += 1.0) {
            Vec3 p = eye.add(look.scale(d));
            AABB box = new AABB(p, p).inflate(2.0);
            List<LivingEntity> hits = level.getEntitiesOfClass(LivingEntity.class, box,
                    e -> e.isAlive() && e != boss && !e.isSpectator());
            for (LivingEntity hit : hits) {
                hit.invulnerableTime = 0;
                hit.hurt(MoonlightDamageSource.of(level, boss), 25.0F);
            }
        }
    }

    /**
     * 月蝕。
     * 周囲に暗黒球を作り、範囲月光ダメージ。
     */
    private static void castMoonEclipse(NoxslratiaEntity boss, ServerLevel level) {
        Vec3 center = boss.position().add(0, 1.0, 0);

        level.playSound(null, boss.getX(), boss.getY(), boss.getZ(),
                SoundEvents.WARDEN_ROAR, SoundSource.HOSTILE, 3.0F, 0.5F);

        // 暗黒球（黒い粒子で満たす）
        for (int i = 0; i < 200; i++) {
            double theta = RANDOM.nextDouble() * Math.PI * 2;
            double phi = RANDOM.nextDouble() * Math.PI;
            double r = RANDOM.nextDouble() * 8.0;

            double x = Math.sin(phi) * Math.cos(theta) * r;
            double y = Math.cos(phi) * r;
            double z = Math.sin(phi) * Math.sin(theta) * r;

            level.sendParticles(ParticleTypes.SQUID_INK,
                    center.x + x, center.y + y, center.z + z,
                    1, 0, 0, 0, 0.02);
            if (i % 3 == 0) {
                level.sendParticles(ParticleTypes.END_ROD,
                        center.x + x, center.y + y, center.z + z,
                        1, 0, 0, 0, 0);
            }
        }

        // 範囲ダメージ
        AABB area = new AABB(center, center).inflate(8.0);
        List<LivingEntity> hits = level.getEntitiesOfClass(LivingEntity.class, area,
                e -> e.isAlive() && e != boss && !e.isSpectator());
        for (LivingEntity hit : hits) {
            hit.invulnerableTime = 0;
            hit.hurt(MoonlightDamageSource.of(level, boss), 30.0F);
            hit.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 100, 0, false, true));
        }
    }

    /**
     * 月影歩法。
     * 残像を残してTP、背後へ。
     */
    private static void castMoonShadowStep(NoxslratiaEntity boss, ServerLevel level, LivingEntity target) {
        Vec3 from = boss.position();

        level.sendParticles(ParticleTypes.PORTAL,
                from.x, from.y + 1.0, from.z,
                40, 0.5, 1.0, 0.5, 0.3);

        Vec3 behind = target.position().subtract(target.getLookAngle().normalize().scale(3.0));
        boss.teleportTo(behind.x, behind.y, behind.z);

        level.sendParticles(ParticleTypes.PORTAL,
                behind.x, behind.y + 1.0, behind.z,
                40, 0.5, 1.0, 0.5, 0.3);
        level.sendParticles(ParticleTypes.END_ROD,
                behind.x, behind.y + 1.0, behind.z,
                20, 0.5, 0.5, 0.5, 0.05);

        level.playSound(null, behind.x, behind.y, behind.z,
                SoundEvents.ENDERMAN_TELEPORT, SoundSource.HOSTILE, 2.0F, 0.7F);
    }

    // =========================================================
    // 氷系
    // =========================================================

    /**
     * 氷華葬。
     * 地面から氷の華が螺旋状に咲く。
     */
    private static void castIceFlowerBurial(NoxslratiaEntity boss, ServerLevel level, LivingEntity target) {
        level.playSound(null, boss.getX(), boss.getY(), boss.getZ(),
                SoundEvents.GLASS_BREAK, SoundSource.HOSTILE, 2.0F, 0.5F);

        // 対象の足元に氷の華
        for (int layer = 0; layer < 4; layer++) {
            double radius = 0.5 + layer * 0.8;
            int count = 8 + layer * 4;
            for (int i = 0; i < count; i++) {
                double angle = (i / (double) count) * Math.PI * 2 + layer * 0.3;
                double x = target.getX() + Math.cos(angle) * radius;
                double z = target.getZ() + Math.sin(angle) * radius;
                double y = target.getY() + layer * 0.3;

                level.sendParticles(ParticleTypes.SNOWFLAKE, x, y, z, 1, 0, 0.05, 0, 0.02);
                if (i % 2 == 0) {
                    level.sendParticles(ParticleTypes.ITEM_SNOWBALL, x, y, z, 1, 0, 0.1, 0, 0.05);
                }
            }
        }

        // 範囲ダメージ
        AABB area = target.getBoundingBox().inflate(4.0);
        List<LivingEntity> hits = level.getEntitiesOfClass(LivingEntity.class, area,
                e -> e.isAlive() && e != boss && !e.isSpectator());
        for (LivingEntity hit : hits) {
            hit.invulnerableTime = 0;
            hit.hurt(level.damageSources().mobAttack(boss), 20.0F);
            hit.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 200, 2, false, true));
            FrostbiteExecution.apply(hit, 200);
        }
    }

    /**
     * 絶対零度。
     * 全周凍結フィールド。
     */
    private static void castAbsoluteZero(NoxslratiaEntity boss, ServerLevel level) {
        Vec3 center = boss.position().add(0, 1.0, 0);

        level.playSound(null, boss.getX(), boss.getY(), boss.getZ(),
                SoundEvents.WARDEN_SONIC_BOOM, SoundSource.HOSTILE, 3.0F, 0.4F);

        // 全方位の冷気リング
        for (int ring = 0; ring < 8; ring++) {
            double radius = 1.0 + ring * 1.5;
            int points = 32 + ring * 8;
            for (int i = 0; i < points; i++) {
                double angle = (i / (double) points) * Math.PI * 2;
                double x = center.x + Math.cos(angle) * radius;
                double z = center.z + Math.sin(angle) * radius;

                for (double y = 0; y < 2.0; y += 0.4) {
                    level.sendParticles(ParticleTypes.SNOWFLAKE, x, center.y + y, z, 1, 0, 0, 0, 0.03);
                }
                if (i % 4 == 0) {
                    level.sendParticles(ParticleTypes.END_ROD, x, center.y + 1.0, z, 1, 0, 0, 0, 0);
                }
            }
        }

        // 範囲ダメージ + 凍結
        AABB area = new AABB(center, center).inflate(12.0);
        List<LivingEntity> hits = level.getEntitiesOfClass(LivingEntity.class, area,
                e -> e.isAlive() && e != boss && !e.isSpectator());
        for (LivingEntity hit : hits) {
            hit.invulnerableTime = 0;
            hit.hurt(level.damageSources().mobAttack(boss), 25.0F);
            hit.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 300, 3, false, true));
            hit.addEffect(new MobEffectInstance(MobEffects.DIG_SLOWDOWN, 300, 3, false, true));
            FrostbiteExecution.apply(hit, 300);
        }
    }

    /**
     * 氷柱地獄。
     * 対象の周囲にランダムで氷柱が立つ。
     */
    private static void castIcicleHell(NoxslratiaEntity boss, ServerLevel level, LivingEntity target) {
        level.playSound(null, boss.getX(), boss.getY(), boss.getZ(),
                SoundEvents.GLASS_BREAK, SoundSource.HOSTILE, 2.0F, 0.7F);

        for (int i = 0; i < 12; i++) {
            double offsetX = (RANDOM.nextDouble() - 0.5) * 8.0;
            double offsetZ = (RANDOM.nextDouble() - 0.5) * 8.0;
            double x = target.getX() + offsetX;
            double z = target.getZ() + offsetZ;
            double y = target.getY();

            // 氷柱（上に向かう粒子の柱）
            for (double h = 0; h < 3.0; h += 0.2) {
                level.sendParticles(ParticleTypes.ITEM_SNOWBALL,
                        x, y + h, z, 1, 0, 0.05, 0, 0.02);
                if (h % 0.6 < 0.2) {
                    level.sendParticles(ParticleTypes.SNOWFLAKE,
                            x, y + h, z, 2, 0.1, 0, 0.1, 0.02);
                }
            }

            // 各柱の範囲ダメージ
            AABB box = new AABB(x - 0.5, y, z - 0.5, x + 0.5, y + 3.0, z + 0.5);
            List<LivingEntity> hits = level.getEntitiesOfClass(LivingEntity.class, box,
                    e -> e.isAlive() && e != boss && !e.isSpectator());
            for (LivingEntity hit : hits) {
                hit.invulnerableTime = 0;
                hit.hurt(level.damageSources().mobAttack(boss), 12.0F);
                hit.push(0, 0.6, 0);
            }
        }
    }

    /**
     * 凍結の鎌。
     * 鎌型にパーティクルを扇状に飛ばす。
     */
    private static void castFreezingScythe(NoxslratiaEntity boss, ServerLevel level, LivingEntity target) {
        Vec3 eye = boss.getEyePosition();
        Vec3 look = boss.getLookAngle().normalize();
        Vec3 right = new Vec3(-look.z, 0, look.x).normalize();

        level.playSound(null, boss.getX(), boss.getY(), boss.getZ(),
                SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.HOSTILE, 1.5F, 1.2F);

        // 鎌型: 外側に弧、内側に弧
        for (int side = -1; side <= 1; side += 2) {
            for (int i = 0; i <= 24; i++) {
                double angle = Math.PI * (0.15 + (i / 24.0) * 0.7);
                double radius = 2.0 + Math.sin(angle) * 1.5;

                Vec3 offset = right.scale(Math.cos(angle) * radius * side)
                        .add(0, Math.sin(angle) * radius, 0);
                Vec3 p = eye.add(look.scale(2.0)).add(offset);

                level.sendParticles(ParticleTypes.SNOWFLAKE,
                        p.x, p.y, p.z, 2, 0.05, 0.05, 0.05, 0.02);
                if (i % 3 == 0) {
                    level.sendParticles(ParticleTypes.ITEM_SNOWBALL,
                            p.x, p.y, p.z, 1, 0, 0, 0, 0.05);
                }
            }
        }

        hitCone(boss, level, target, 6.0, 90.0, 18.0F,
                level.damageSources().mobAttack(boss));

        // ヒットした敵に凍傷
        AABB area = boss.getBoundingBox().inflate(6.0);
        for (LivingEntity hit : level.getEntitiesOfClass(LivingEntity.class, area,
                e -> e.isAlive() && e != boss && !e.isSpectator())) {
            FrostbiteExecution.apply(hit, 200);
        }
    }

    // =========================================================
    // ハイブリッド
    // =========================================================

    /**
     * 白夜。
     * 全天発光 + 月光/氷の両属性。
     */
    private static void castWhiteNight(NoxslratiaEntity boss, ServerLevel level, LivingEntity target) {
        Vec3 center = boss.position().add(0, 1.5, 0);

        level.playSound(null, boss.getX(), boss.getY(), boss.getZ(),
                SoundEvents.END_PORTAL_SPAWN, SoundSource.HOSTILE, 3.0F, 0.5F);
        level.playSound(null, boss.getX(), boss.getY(), boss.getZ(),
                SoundEvents.WARDEN_ROAR, SoundSource.HOSTILE, 3.0F, 0.6F);

        // 全天に広がる光の粒子
        for (int i = 0; i < 300; i++) {
            double theta = RANDOM.nextDouble() * Math.PI * 2;
            double phi = RANDOM.nextDouble() * Math.PI;
            double r = 3.0 + RANDOM.nextDouble() * 12.0;

            double x = Math.sin(phi) * Math.cos(theta) * r;
            double y = Math.cos(phi) * r;
            double z = Math.sin(phi) * Math.sin(theta) * r;

            level.sendParticles(ParticleTypes.END_ROD,
                    center.x + x, center.y + y, center.z + z,
                    1, 0, 0, 0, 0.01);

            if (i % 2 == 0) {
                level.sendParticles(ParticleTypes.SNOWFLAKE,
                        center.x + x, center.y + y, center.z + z,
                        1, 0, 0, 0, 0.02);
            }
            if (i % 5 == 0) {
                level.sendParticles(ParticleTypes.FLASH,
                        center.x + x, center.y + y, center.z + z,
                        1, 0, 0, 0, 0);
            }
        }

        // 範囲ダメージ（月光 + 凍傷）
        AABB area = new AABB(center, center).inflate(15.0);
        List<LivingEntity> hits = level.getEntitiesOfClass(LivingEntity.class, area,
                e -> e.isAlive() && e != boss && !e.isSpectator());
        for (LivingEntity hit : hits) {
            hit.invulnerableTime = 0;
            hit.hurt(MoonlightDamageSource.of(level, boss), 40.0F);
            hit.hurt(level.damageSources().mobAttack(boss), 20.0F);
            hit.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 300, 3, false, true));
            hit.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 100, 0, false, true));
            FrostbiteExecution.apply(hit, 300);
        }
    }

    // =========================================================
    // ヘルパ
    // =========================================================

    /**
     * 扇状（コーン状）の範囲ダメージ。
     */
    private static void hitCone(
            NoxslratiaEntity boss,
            ServerLevel level,
            LivingEntity target,
            double range,
            double angleDegrees,
            float damage,
            net.minecraft.world.damagesource.DamageSource source
    ) {
        Vec3 look = boss.getLookAngle().normalize();
        AABB area = boss.getBoundingBox().inflate(range);
        List<LivingEntity> hits = level.getEntitiesOfClass(LivingEntity.class, area,
                e -> e.isAlive() && e != boss && !e.isSpectator());

        for (LivingEntity hit : hits) {
            Vec3 toTarget = hit.position().subtract(boss.position()).normalize();
            double dot = Math.max(-1.0, Math.min(1.0, look.dot(toTarget)));
            double angle = Math.toDegrees(Math.acos(dot));

            if (angle <= angleDegrees / 2.0) {
                hit.invulnerableTime = 0;
                hit.hurt(source, damage);
            }
        }
    }
}