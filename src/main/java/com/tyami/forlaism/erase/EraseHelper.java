package com.tyami.forlaism.erase;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.entity.EntityAccess;
import net.minecraft.world.phys.AABB;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.List;
import java.util.UUID;

/**
 * OverOverNull 本体。
 *
 * 一切の演出なし。静かに、確実に、存在を消す。
 */
public final class EraseHelper {

    private EraseHelper() {
    }

    /** 効果半径。 */
    public static final double RADIUS = 30.0D;

    // =========================================================
    // メイン: 範囲抹消
    // =========================================================

    /**
     * 発動者以外の半径30m以内の全Entityを抹消する。
     */
    public static void eraseAround(ServerPlayer caster) {

        if (!(caster.level() instanceof ServerLevel level)) return;

        UUID casterUUID = caster.getUUID();
        AABB area = caster.getBoundingBox().inflate(RADIUS);

        // 半径30m以内の全Entity（プレイヤー・Mob・動物・弾・パーツ全部）
        List<Entity> targets = level.getEntities(
                (Entity) null,
                area,
                e -> e != null
                        && e.isAlive()
                        && !e.getUUID().equals(casterUUID)
        );

        for (Entity target : targets) {
            erase(level, target, caster);
        }
    }

    // =========================================================
    // 単体抹消
    // =========================================================

    /**
     * 1体を完全抹消する。
     */
    public static void erase(ServerLevel level, Entity target, ServerPlayer caster) {

        if (target == null) return;
        if (target.isRemoved()) return;

        UUID uuid = target.getUUID();

        // 既に抹消済みならスキップ
        if (EraseRegistry.isErased(level.getServer(), uuid)) {
            forceRemove(level, target);
            return;
        }

        // 抹消フラグを立てる
        EraseTracker.begin();

        try {
            // =========================================================
            // Phase 1: 事前無力化
            // =========================================================
            neutralize(target);

            // =========================================================
            // Phase 2: ダメージ / HP同期データ
            // =========================================================
            applyEraseDamage(level, target, caster);

            // =========================================================
            // Phase 3: 死亡処理
            // =========================================================
            forceDie(level, target, caster);

            // =========================================================
            // Phase 4: エンティティ管理から除去
            // =========================================================
            forceRemove(level, target);

            // =========================================================
            // Phase 5: UUIDを抹消リストに登録（永続）
            // =========================================================
            EraseRegistry.markErased(level.getServer(), uuid);

            // =========================================================
            // Phase 6: クライアントへ通知
            // =========================================================
            broadcastErase(level, uuid);

        } finally {
            EraseTracker.end();
        }
    }

    // =========================================================
    // Phase 1: 事前無力化
    // =========================================================

    private static void neutralize(Entity target) {

        // 移動停止
        target.setDeltaMovement(0, 0, 0);
        target.fallDistance = 0.0F;

        // 無敵時間リセット
        if (target instanceof LivingEntity living) {
            living.invulnerableTime = 0;
            living.hurtTime = 0;
            living.hurtDuration = 0;
            living.deathTime = 0;

            // AI停止
            if (target instanceof Mob mob) {
                mob.setNoAi(true);
                mob.setTarget(null);
            }

            // エフェクト除去
            living.removeAllEffects();
        }

        // 乗り物から降ろす
        target.stopRiding();
        for (Entity passenger : target.getPassengers()) {
            passenger.stopRiding();
        }

        // 不滅・無敵フラグを折る
        try {
            target.setInvulnerable(false);
        } catch (Throwable ignored) {
        }
    }

    // =========================================================
    // Phase 2: ダメージ
    // =========================================================

    private static void applyEraseDamage(ServerLevel level, Entity target, ServerPlayer caster) {

        if (!(target instanceof LivingEntity living)) return;

        DamageSource src = EraseDamageSource.of(level, caster);

        // hurt() を叩く（EraseTracker経由で全防御が無効化される）
        try {
            living.hurt(src, Float.MAX_VALUE);
        } catch (Throwable ignored) {
        }

        // HP同期データを直接0に
        forceSyncHealthZero(living);

        // setHealth(0)も叩く
        try {
            living.setHealth(0.0F);
        } catch (Throwable ignored) {
        }
    }

    /**
     * DATA_HEALTH_ID を直接 0 に書き換える。
     */
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
        } catch (Throwable ignored) {
        }
    }

    // =========================================================
    // Phase 3: 死亡処理
    // =========================================================

    private static void forceDie(ServerLevel level, Entity target, ServerPlayer caster) {

        if (!(target instanceof LivingEntity living)) return;

        DamageSource src = EraseDamageSource.of(level, caster);

        try {
            if (!living.isDeadOrDying()) {
                living.die(src);
            }
        } catch (Throwable ignored) {
        }

        try {
            if (!living.isRemoved()) {
                living.kill();
            }
        } catch (Throwable ignored) {
        }
    }

    // =========================================================
    // Phase 4: エンティティ管理から除去
    // =========================================================

    /**
     * Minecraftのエンティティ管理システムから物理的に抹消する。
     */
    public static void forceRemove(ServerLevel level, Entity target) {

        UUID uuid = target.getUUID();

        // チャンクソースから除去
        try {
            level.getChunkSource().removeEntity(target);
        } catch (Throwable ignored) {
        }

        // レベルから除去
        try {
            level.getEntities().get(target.getId());  // 存在確認
        } catch (Throwable ignored) {
        }

        // リフレクションで PersistentEntitySectionManager から抹消
        removeFromEntityManager(level, target);

        // 最終手段
        try {
            target.setRemoved(Entity.RemovalReason.KILLED);
        } catch (Throwable ignored) {
        }

        try {
            target.discard();
        } catch (Throwable ignored) {
        }

        // ボスバー除去
        removeBossBar(target);
    }

    /**
     * PersistentEntitySectionManager から抹消する。
     */
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

            // remove(EntityAccess) を叩く
            Method removeMethod = manager.getClass().getMethod(
                    "remove",
                    EntityAccess.class
            );
            removeMethod.setAccessible(true);
            removeMethod.invoke(manager, target);

        } catch (Throwable ignored) {
        }
    }

    /**
     * ボスバーを除去する。
     */
    private static void removeBossBar(Entity target) {

        if (!(target instanceof LivingEntity living)) return;

        try {
            // LivingEntityの private field "bossEvent"
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
        } catch (Throwable ignored) {
        }
    }

    // =========================================================
    // Phase 6: クライアントへ通知
    // =========================================================

    private static void broadcastErase(ServerLevel level, UUID uuid) {

        // 独自パケットは今回使わない（演出なしのため）
        // 代わりに、通常のエンティティ削除通知を全員に送る
        // → EntityLookupから消えているので、次tickでクライアントも消える

        // 念のため、近くのプレイヤーには個別に通知
        for (ServerPlayer p : level.players()) {
            p.connection.send(new net.minecraft.network.protocol.game.ClientboundRemoveEntitiesPacket(
                    // 該当IDは既に消えているが、念のため
                    -1
            ));
        }
    }

    // =========================================================
    // チェック
    // =========================================================

    /**
     * 既に抹消済みのEntityが復活していたら再度抹消。
     */
    public static boolean checkAndReErase(ServerLevel level, Entity entity) {
        if (entity == null) return false;
        UUID uuid = entity.getUUID();
        if (EraseRegistry.isErased(level.getServer(), uuid)) {
            erase(level, entity, null);
            return true;
        }
        return false;
    }
}