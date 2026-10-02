package com.tyami.forlaism.annihilation;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundRemoveEntitiesPacket;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.entity.EntityAccess;
import net.minecraft.world.phys.AABB;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.List;
import java.util.UUID;

/**
 * GMB の世界消去システム。
 *
 * 【重要】
 *   EXECUTING フラグで実行中を明示し、
 *   EntityMixin / MinionHaloMixin / LivingEntityMixin の防御を突破する。
 */
public final class GMBAnnihilation {

    /** 消去半径。 */
    public static final double RADIUS = 128.0D;

    // =========================================================
    // 実行中フラグ（ThreadLocal）
    // =========================================================

    private static final ThreadLocal<Boolean> EXECUTING =
            ThreadLocal.withInitial(() -> false);

    public static boolean isExecuting() {
        return EXECUTING.get();
    }

    private static void beginExecution() {
        EXECUTING.set(true);
    }

    private static void endExecution() {
        EXECUTING.set(false);
    }

    private GMBAnnihilation() {
    }

    // =========================================================
    // 発動
    // =========================================================

    public static void annihilateAround(ServerPlayer caster) {

        if (!(caster.level() instanceof ServerLevel level)) return;

        UUID casterUUID = caster.getUUID();

        // =========================================================
        // 発動者を中心とした球状範囲。inflate(128) だと立方体になるが、
        // あとで distSqr で球体判定する。
        // =========================================================
        AABB area = caster.getBoundingBox().inflate(RADIUS);
        double radiusSqr = RADIUS * RADIUS;
        double cx = caster.getX();
        double cy = caster.getY();
        double cz = caster.getZ();

        List<Entity> targets = level.getEntities(
                (Entity) null,
                area,
                e -> e != null
                        && !e.getUUID().equals(casterUUID)
                        && e.distanceToSqr(cx, cy, cz) <= radiusSqr
        );

        int count = 0;

        // =========================================================
        // 実行開始！ これで全防御Mixinが一時停止する
        // =========================================================
        beginExecution();
        try {
            for (Entity target : targets) {
                try {
                    erase(level, target, caster);
                    count++;
                } catch (Throwable t) {
                    System.err.println("[GMB] erase failed for "
                            + target.getName().getString() + ": " + t);
                }
            }
        } finally {
            endExecution();
        }

        // =========================================================
        // 演出
        // =========================================================
        level.playSound(
                null,
                caster.getX(), caster.getY(), caster.getZ(),
                SoundEvents.WARDEN_SONIC_BOOM,
                SoundSource.PLAYERS,
                5.0F, 0.3F
        );

        level.playSound(
                null,
                caster.getX(), caster.getY(), caster.getZ(),
                SoundEvents.SCULK_SHRIEKER_SHRIEK,
                SoundSource.PLAYERS,
                5.0F, 0.5F
        );

        for (int r = 0; r < 6; r++) {
            double radiusRing = (r + 1) * (RADIUS / 6.0);
            for (int i = 0; i < 64; i++) {
                double angle = (i / 64.0) * Math.PI * 2;
                double x = cx + Math.cos(angle) * radiusRing;
                double z = cz + Math.sin(angle) * radiusRing;
                level.sendParticles(
                        ParticleTypes.SCULK_SOUL,
                        x, cy + 0.5, z,
                        1, 0, 0, 0, 0
                );
            }
        }

        caster.displayClientMessage(
                Component.literal("§d§lGMB §f世界消去… §c" + count + "体抹消")
                        .withStyle(net.minecraft.ChatFormatting.LIGHT_PURPLE),
                true
        );
    }

    // =========================================================
    // 単体消去
    // =========================================================

    public static void erase(ServerLevel level, Entity target, ServerPlayer caster) {

        if (target == null || target.isRemoved()) return;

        UUID uuid = target.getUUID();

        // =========================================================
        // 登録は最初にやる。これで以降の防御Mixinが効かなくなる
        // =========================================================
        GMBEraseRegistry.markErased(level.getServer(), uuid);
        GMBEraseRegistry.markErasedClient(uuid);

        // =========================================================
        // Phase 1: 無力化
        // =========================================================
        neutralize(target);

        // =========================================================
        // Phase 2: ダメージ
        // =========================================================
        applyEraseDamage(level, target, caster);

        // =========================================================
        // Phase 3: 死亡処理
        // =========================================================
        forceDie(level, target, caster);

        // =========================================================
        // Phase 4: 物理削除
        // =========================================================
        forceRemove(level, target);

        // =========================================================
        // Phase 5: 全プレイヤーに削除パケット
        // =========================================================
        broadcastRemove(level, target);
    }

    // =========================================================
    // Phase 1: 無力化
    // =========================================================

    private static void neutralize(Entity target) {

        target.setDeltaMovement(0, 0, 0);
        target.fallDistance = 0.0F;

        if (target instanceof LivingEntity living) {
            living.invulnerableTime = 0;
            living.hurtTime = 0;
            living.hurtDuration = 0;
            living.deathTime = 0;

            if (target instanceof Mob mob) {
                mob.setNoAi(true);
                mob.setTarget(null);
            }

            living.removeAllEffects();
        }

        target.stopRiding();
        for (Entity passenger : target.getPassengers()) {
            passenger.stopRiding();
        }

        try {
            target.setInvulnerable(false);
        } catch (Throwable ignored) {}
    }

    // =========================================================
    // Phase 2: ダメージ
    // =========================================================

    private static void applyEraseDamage(ServerLevel level, Entity target, ServerPlayer caster) {

        if (!(target instanceof LivingEntity living)) return;

        DamageSource src = GMBEraseDamageSource.of(level, caster);

        try {
            living.hurt(src, Float.MAX_VALUE);
        } catch (Throwable ignored) {}

        forceSyncHealthZero(living);

        try {
            living.setHealth(0.0F);
        } catch (Throwable ignored) {}
    }

    private static void forceSyncHealthZero(LivingEntity living) {
        try {
            Field idField;
            try {
                idField = LivingEntity.class.getDeclaredField("DATA_HEALTH_ID");
            } catch (NoSuchFieldException e) {
                idField = LivingEntity.class.getDeclaredField("f_20883_");
            }
            idField.setAccessible(true);
            Object dataAccessor = idField.get(null);
            if (dataAccessor instanceof net.minecraft.network.syncher.EntityDataAccessor<?> accessor) {
                living.getEntityData().set(
                        (net.minecraft.network.syncher.EntityDataAccessor<Float>) accessor,
                        0.0F
                );
            }
        } catch (Throwable ignored) {}
    }

    // =========================================================
    // Phase 3: 死亡
    // =========================================================

    private static void forceDie(ServerLevel level, Entity target, ServerPlayer caster) {

        if (!(target instanceof LivingEntity living)) return;

        DamageSource src = GMBEraseDamageSource.of(level, caster);

        try {
            if (!living.isDeadOrDying()) {
                living.die(src);
            }
        } catch (Throwable ignored) {}

        try {
            if (!living.isRemoved()) {
                living.kill();
            }
        } catch (Throwable ignored) {}
    }

    // =========================================================
    // Phase 4: 物理削除
    // =========================================================

    public static void forceRemove(ServerLevel level, Entity target) {

        // チャンクソースから除去
        try {
            level.getChunkSource().removeEntity(target);
        } catch (Throwable ignored) {}

        // リフレクションでエンティティマネージャから抹消
        removeFromEntityManager(level, target);

        // 最終手段
        try { target.setRemoved(Entity.RemovalReason.KILLED); } catch (Throwable ignored) {}
        try { target.discard(); } catch (Throwable ignored) {}
        try { target.remove(Entity.RemovalReason.DISCARDED); } catch (Throwable ignored) {}

        // ボスバー除去
        removeBossBar(target);
    }

    private static void removeFromEntityManager(ServerLevel level, Entity target) {
        try {
            Field emField;
            try {
                emField = ServerLevel.class.getDeclaredField("entityManager");
            } catch (NoSuchFieldException e) {
                emField = ServerLevel.class.getDeclaredField("f_143244_");
            }
            emField.setAccessible(true);
            Object manager = emField.get(level);
            if (manager == null) return;

            Method removeMethod = manager.getClass().getMethod(
                    "remove",
                    EntityAccess.class
            );
            removeMethod.setAccessible(true);
            removeMethod.invoke(manager, target);
        } catch (Throwable ignored) {}
    }

    private static void removeBossBar(Entity target) {
        if (!(target instanceof LivingEntity living)) return;
        try {
            Field field;
            try {
                field = LivingEntity.class.getDeclaredField("bossEvent");
            } catch (NoSuchFieldException e) {
                field = LivingEntity.class.getDeclaredField("f_20895_");
            }
            field.setAccessible(true);
            Object bossEvent = field.get(living);
            if (bossEvent instanceof ServerBossEvent boss) {
                boss.setProgress(0.0F);
                boss.removeAllPlayers();
                boss.setVisible(false);
            }
        } catch (Throwable ignored) {}
    }

    // =========================================================
    // Phase 5: 削除パケット
    // =========================================================

    private static void broadcastRemove(ServerLevel level, Entity target) {

        ClientboundRemoveEntitiesPacket packet =
                new ClientboundRemoveEntitiesPacket(target.getId());

        for (ServerPlayer p : level.players()) {
            p.connection.send(packet);
        }
    }

    // =========================================================
    // 再スポーン阻止チェック
    // =========================================================

    public static boolean checkAndReErase(ServerLevel level, Entity entity) {
        if (entity == null) return false;
        UUID uuid = entity.getUUID();
        if (GMBEraseRegistry.isErased(level.getServer(), uuid)) {
            erase(level, entity, null);
            return true;
        }
        return false;
    }
}