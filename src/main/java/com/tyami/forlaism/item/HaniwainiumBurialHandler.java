package com.tyami.forlaism.item;

import com.tyami.forlaism.registry.Items;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.List;
import java.util.Random;

/**
 * ハニワイニウムの剣の2能力（土葬・地震）の実装。
 */
public final class HaniwainiumBurialHandler {

    /** 土葬タグ。 */
    public static final String TAG_BURIED = "ForlaismHaniwaBuried";
    public static final String TAG_BURIED_UNTIL = "ForlaismHaniwaBuriedUntil";

    /** 土葬の持続時間（tick）。20秒。 */
    public static final int BURIED_DURATION = 400;

    /** 地震の画面揺れ演出用。 */
    private static final Random RANDOM = new Random();

    private HaniwainiumBurialHandler() {
    }

    // =========================================================
    // 土葬
    // =========================================================

    public static void castBurial(ServerLevel level, ServerPlayer caster) {

        Vec3 center = caster.position();

        AABB area = new AABB(
                center.x - HaniwainiumSwordItem.BURIAL_RADIUS,
                center.y - HaniwainiumSwordItem.BURIAL_RADIUS,
                center.z - HaniwainiumSwordItem.BURIAL_RADIUS,
                center.x + HaniwainiumSwordItem.BURIAL_RADIUS,
                center.y + HaniwainiumSwordItem.BURIAL_RADIUS,
                center.z + HaniwainiumSwordItem.BURIAL_RADIUS
        );

        List<LivingEntity> targets = level.getEntitiesOfClass(
                LivingEntity.class, area,
                e -> e.isAlive() && e != caster && !e.isSpectator()
        );

        int buried = 0;

        for (LivingEntity target : targets) {

            // 距離を厳密チェック（球体判定）
            if (target.distanceToSqr(center) > HaniwainiumSwordItem.BURIAL_RADIUS * HaniwainiumSwordItem.BURIAL_RADIUS) {
                continue;
            }

            // =========================================================
            // 3%即死
            // =========================================================
            if (RANDOM.nextFloat() < HaniwainiumSwordItem.BURIAL_INSTANT_KILL_CHANCE) {
                target.invulnerableTime = 0;
                target.setHealth(0.0F);
                if (!target.isDeadOrDying()) {
                    target.die(level.damageSources().playerAttack(caster));
                }

                level.sendParticles(
                        ParticleTypes.EXPLOSION_EMITTER,
                        target.getX(), target.getY() + 1.0, target.getZ(),
                        1, 0, 0, 0, 0
                );

                buried++;
                continue;
            }

            // =========================================================
            // 土葬処理
            // =========================================================
            buryEntity(level, target, caster);
            buried++;
        }

        // =========================================================
        // 演出
        // =========================================================
        level.playSound(null,
                caster.getX(), caster.getY(), caster.getZ(),
                SoundEvents.WARDEN_SONIC_BOOM,
                SoundSource.PLAYERS, 2.0F, 0.5F);
        level.playSound(null,
                caster.getX(), caster.getY(), caster.getZ(),
                SoundEvents.ROOTED_DIRT_PLACE,
                SoundSource.PLAYERS, 2.0F, 0.6F);

        for (int r = 0; r < 5; r++) {
            double radius = (r + 1) * (HaniwainiumSwordItem.BURIAL_RADIUS / 5.0);
            for (int i = 0; i < 32; i++) {
                double angle = (i / 32.0) * Math.PI * 2;
                double x = caster.getX() + Math.cos(angle) * radius;
                double z = caster.getZ() + Math.sin(angle) * radius;
            }
        }

        caster.displayClientMessage(
                Component.literal("§6§l土葬… §f" + buried + "体を土に還した"),
                true
        );
    }

    private static void buryEntity(ServerLevel level, LivingEntity target, ServerPlayer caster) {

        // AI停止
        if (target instanceof Mob mob) {
            mob.setNoAi(true);
            mob.setDeltaMovement(0, 0, 0);
            mob.setTarget(null);
        }

        // 埋める位置を「足元の地面」に固定
        BlockPos feet = target.blockPosition();
        BlockPos buriedPos = new BlockPos(feet.getX(), feet.getY(), feet.getZ());

        target.teleportTo(
                buriedPos.getX() + 0.5,
                buriedPos.getY(),
                buriedPos.getZ() + 0.5
        );

        target.setDeltaMovement(0, -0.5, 0);
        target.hurtMarked = true;

        // 足元と頭を土で埋める（既存空気のみ）
        for (int dy = 0; dy <= 1; dy++) {
            for (int dx = -1; dx <= 1; dx++) {
                for (int dz = -1; dz <= 1; dz++) {
                    if (dx == 0 && dz == 0) continue;
                    BlockPos pos = buriedPos.offset(dx, dy, dz);
                    BlockState s = level.getBlockState(pos);
                    if (s.isAir()) {
                        level.setBlock(pos, Blocks.DIRT.defaultBlockState(), 3);
                    }
                }
            }
        }

        // =========================================================
        // 脱出阻害: 移動不可 + ジャンプ不可 + 暗闇
        // =========================================================
        target.addEffect(new MobEffectInstance(
                MobEffects.MOVEMENT_SLOWDOWN,
                BURIED_DURATION, 254,
                false, false, false
        ));
        target.addEffect(new MobEffectInstance(
                MobEffects.JUMP,
                BURIED_DURATION, 250,
                false, false, false
        ));
        target.addEffect(new MobEffectInstance(
                MobEffects.DARKNESS,
                BURIED_DURATION, 0,
                false, false, false
        ));

        // =========================================================
        // 20秒ごとに埋め直す＆ダメージ
        // =========================================================
        CompoundTag data = target.getPersistentData();
        data.putBoolean(TAG_BURIED, true);
        data.putLong(TAG_BURIED_UNTIL, level.getGameTime() + BURIED_DURATION);

    }

    // =========================================================
    // 地震
    // =========================================================

    public static void castQuake(ServerLevel level, ServerPlayer caster) {

        Vec3 center = caster.position();

        // 画面揺れパケット（クライアントへ）
        com.tyami.forlaism.network.FactotumPacketHandler.CHANNEL.sendTo(
                new com.tyami.forlaism.network.ScreenShakePacket(
                        HaniwainiumSwordItem.QUAKE_DURATION_TICKS,
                        1.0F
                ),
                caster.connection.connection,
                net.minecraftforge.network.NetworkDirection.PLAY_TO_CLIENT
        );

        // サーバー側で「5秒 × 毎秒10ダメージ」をスケジュール
        for (int sec = 0; sec < 5; sec++) {
            final int secIdx = sec;

            com.tyami.forlaism.item.MasterpieceClockScheduler.schedule(
                    level, sec * 20,
                    () -> quakeTick(level, caster, center, secIdx)
            );
        }

        // 発動演出
        level.playSound(null,
                caster.getX(), caster.getY(), caster.getZ(),
                SoundEvents.WARDEN_ROAR,
                SoundSource.PLAYERS, 3.0F, 0.5F);
        level.playSound(null,
                caster.getX(), caster.getY(), caster.getZ(),
                SoundEvents.GENERIC_EXPLODE,
                SoundSource.PLAYERS, 2.0F, 0.4F);

    }

    private static void quakeTick(ServerLevel level, ServerPlayer caster, Vec3 center, int sec) {

        AABB area = new AABB(
                center.x - HaniwainiumSwordItem.QUAKE_RADIUS,
                center.y - HaniwainiumSwordItem.QUAKE_RADIUS,
                center.z - HaniwainiumSwordItem.QUAKE_RADIUS,
                center.x + HaniwainiumSwordItem.QUAKE_RADIUS,
                center.y + HaniwainiumSwordItem.QUAKE_RADIUS,
                center.z + HaniwainiumSwordItem.QUAKE_RADIUS
        );

        List<LivingEntity> targets = level.getEntitiesOfClass(
                LivingEntity.class, area,
                e -> e.isAlive() && e != caster && !e.isSpectator()
        );

        for (LivingEntity target : targets) {
            if (target.distanceToSqr(center) > HaniwainiumSwordItem.QUAKE_RADIUS * HaniwainiumSwordItem.QUAKE_RADIUS) {
                continue;
            }

            target.invulnerableTime = 0;
            target.hurt(
                    level.damageSources().playerAttack(caster),
                    HaniwainiumSwordItem.QUAKE_DAMAGE_PER_SECOND
            );
        }

        // 地割れパーティクル
        for (int i = 0; i < 60; i++) {
            double angle = RANDOM.nextDouble() * Math.PI * 2;
            double r = RANDOM.nextDouble() * HaniwainiumSwordItem.QUAKE_RADIUS;

            double x = center.x + Math.cos(angle) * r;
            double z = center.z + Math.sin(angle) * r;

            BlockPos ground = BlockPos.containing(x, center.y, z);
            for (int dy = 2; dy >= -2; dy--) {
                BlockPos p = ground.offset(0, dy, 0);
                if (!level.getBlockState(p).isAir()) {

                    break;
                }
            }
        }

        level.playSound(null,
                center.x, center.y, center.z,
                SoundEvents.GENERIC_EXPLODE,
                SoundSource.PLAYERS, 1.5F, 0.3F + sec * 0.1F);
    }
}