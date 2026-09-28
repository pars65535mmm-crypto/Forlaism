package com.tyami.forlaism.util;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
/**
 * Overnull。
 *
 * 「即死させる手段を全部やる」。
 *
 * 個別の防御（Halo / Freeze / Swrequimet / 微睡 / EntityMixin のキャンセル等）を
 * 個別に突破するのではなく、考えうる致死ルートを片っ端から全部叩き込む。
 *
 * どのルートか1つでも通れば死ぬ。
 *
 * ThreadLocal の EXECUTING フラグで、
 * 味方側の防御 Mixin（setHealth / die / hurt）を一時的に無効化する。
 */
public final class Overnull {

    private Overnull() {
    }

    // =========================================================
    // 実行中フラグ
    // =========================================================

    private static final ThreadLocal<Boolean> EXECUTING =
            ThreadLocal.withInitial(() -> false);

    public static boolean isExecuting() {
        return EXECUTING.get();
    }

    // =========================================================
    // メイン
    // =========================================================

    /**
     * 対象を Overnull する。
     *
     * @return 実行できたら true
     */
    public static boolean execute(LivingEntity target) {
        if (target == null) return false;
        if (target.level().isClientSide) return false;
        if (target.isDeadOrDying()) return false;

        // クリエイティブは除外
        if (target instanceof Player p && p.isCreative()) return false;

        EXECUTING.set(true);
        try {
            applyAllRoutes(target);
        } finally {
            EXECUTING.set(false);
        }

        // 演出
        if (target.level() instanceof ServerLevel sl) {
            playEffect(sl, target);
        }

        return true;
    }

    // =========================================================
    // 全ルート叩き込み
    // =========================================================

private static void applyAllRoutes(LivingEntity target) {
    // 1. 無敵時間リセット
    target.invulnerableTime = 0;
    target.hurtTime = 0;
    target.hurtDuration = 0;
    // 2. エフェクト剥がし
    target.removeAllEffects();
    // 3. setHealth(0)
    target.setHealth(0.0F);
    // 4. hurt()
    DamageSource source = target.damageSources().genericKill();
    target.hurt(source, Float.MAX_VALUE);
    // 5. die()
    if (!target.isDeadOrDying()) {
        target.die(source);
    }
    // 6. 保険 setHealth(0)
    if (target.getHealth() > 0.0F) {
        target.setHealth(0.0F);
    }
    // 7. kill()
    if (!target.isDeadOrDying()) {
        target.kill();
    }
    // 8. 保険
    if (!target.isDeadOrDying()) {
        target.setHealth(0.0F);
        target.die(source);
    }
net.minecraft.nbt.CompoundTag persistentData = target.getPersistentData();
    if (persistentData != null) {
        for (String key : new java.util.ArrayList<>(persistentData.getAllKeys())) {
            persistentData.remove(key);
        }
    }
target.hurt(target.damageSources().fellOutOfWorld(), Float.MAX_VALUE);
target.hurt(target.damageSources().fellOutOfWorld(), 1308);
target.setRemoved(Entity.RemovalReason.KILLED);
target.discard();
if (!target.isRemoved()) {
    target.discard();
}

target.setHealth(0.0F);
target.kill();
target.discard();
if (target.isAlive()) {
    target.hurt(source, Float.MAX_VALUE);
}
if (target.isAlive()) {
    target.setHealth(0.0F);
}
if (target.isAlive()) {
    target.die(source);
}
if (target.isAlive()) {
    target.kill();
}
if (target.isAlive()) {
    target.discard();
}
target.setHealth(0.0F);
target.kill();
target.discard();
target.hurt(target.damageSources().genericKill(), Float.MAX_VALUE);
target.hurt(target.damageSources().fellOutOfWorld(), Float.MAX_VALUE);
target.hurt(target.damageSources().outOfBorder(), Float.MAX_VALUE);
target.hurt(target.damageSources().outOfBorder(), Float.MAX_VALUE);
target.kill();
target.discard();
target.remove(Entity.RemovalReason.KILLED);
target.remove(Entity.RemovalReason.DISCARDED);
target.remove(Entity.RemovalReason.KILLED);
target.remove(Entity.RemovalReason.DISCARDED);
target.setPos(
    0,
    -2048,
    0
);
target.setNoGravity(true);
target.noPhysics = true;
target.setInvulnerable(false);
if (target instanceof Mob mob) {
    mob.setNoAi(true);
}
if (target instanceof Mob mob) {
    mob.setTarget(null);
}
for (EquipmentSlot slot : EquipmentSlot.values()) {
    target.setItemSlot(slot, ItemStack.EMPTY);
}
ServerLevel destination =
    target.level().getServer().getLevel(Level.NETHER);

if (destination != null) {
    target.changeDimension(destination);
}
target.changeDimension(
    target.level().getServer().getLevel(Level.NETHER)
);
target.setDeltaMovement(Vec3.ZERO);
target.stopRiding();
for (Entity passenger : target.getPassengers()) {
    passenger.stopRiding();
}
target.stopRiding();
if (target.level() instanceof ServerLevel server) {
    Entity entity = server.getEntity(target.getUUID());

    if (entity != null) {
        entity.remove(Entity.RemovalReason.KILLED);
    }
}
if (target.level() instanceof ServerLevel serverLevel) {
        // 対象がプレイヤーの場合は、サーバーから強制切断（キック）してログイン状態ごと消し去る
        if (target instanceof ServerPlayer serverPlayer) {
            serverPlayer.connection.disconnect(net.minecraft.network.chat.Component.literal("概念消滅により世界から追放されました。"));
        } else {
            // 通常のMobやボスの場合は、ServerLevelに実装されている正式な「公開メソッド」を使用します。
            // 内部のエンティティトラッカー、チャンクマップへの登録、レンダリング同期をすべて【イベントなし】で一括削除します。
            serverLevel.getChunkSource().removeEntity(target);
        }
    }
if (target.level() instanceof ServerLevel serverLevel) {
        // A. プレイヤーの場合は、ネットワークを切断してログイン状態ごと世界から追放
        if (target instanceof ServerPlayer serverPlayer) {
            serverPlayer.connection.disconnect(Component.literal("§5§l概念消滅により世界から追放されました。"));
        } else {
            // B. チャンク追跡システムから抹消（周りのプレイヤーの画面から即座に消す）
            serverLevel.getChunkSource().removeEntity(target);
            
            // C. 1.20.1最強の裏技: リフレクションによる「entityManager」への侵入
            // 低レイヤーの管理マップ（PersistentEntitySectionManager）から、対象の登録とUUIDインデックスを物理削除します。
            // これにより、MOD側が「同じUUIDでセルフ再スポーンする」処理を完全にクラッシュ（封殺）させます。
            try {
                // Mojang Mappings における ServerLevel の内部エンティティマネージャーを取得
                // 1.20.1では「entityManager」というプライベートフィールドで定義されています
                java.lang.reflect.Field entityManagerField = ServerLevel.class.getDeclaredField("entityManager");
                entityManagerField.setAccessible(true);
                Object manager = entityManagerField.get(serverLevel);
                
                if (manager != null) {
                    // PersistentEntitySectionManager.remove(EntityAccess) を取得して実行
                    java.lang.reflect.Method removeMethod = manager.getClass().getMethod("remove", net.minecraft.world.level.entity.EntityAccess.class);
                    removeMethod.setAccessible(true);
                    removeMethod.invoke(manager, target);
                    
                    // 内部のセクションストレージやインデックスマップからも完全にパージしたい場合、
                    // クラス内の「sectionStorage」や「visibleEntityIds」などのマップを直接操作することも可能。
                }
            } catch (Exception e) {
                // フォールバック: 通常のID削除窓口を叩く
                serverLevel.getEntities().get(target.getId());
            }
        }
    }
target.setRemoved(Entity.RemovalReason.DISCARDED);
target.level().getServer().getPlayerList();
target.noPhysics = true;
target.setNoGravity(true);
target.setDeltaMovement(Vec3.ZERO);
}

    // =========================================================
    // 演出
    // =========================================================

    private static void playEffect(ServerLevel sl, LivingEntity target) {

        double x = target.getX();
        double y = target.getY() + target.getBbHeight() * 0.5;
        double z = target.getZ();

        // 黒い柱
        sl.sendParticles(
                ParticleTypes.SCULK_SOUL,
                x, y, z,
                80, 0.4, 1.2, 0.4, 0.05
        );

        // 崩壊
        sl.sendParticles(
                ParticleTypes.SQUID_INK,
                x, y, z,
                40, 0.6, 1.0, 0.6, 0.02
        );

        // 中心の光
        sl.sendParticles(
                ParticleTypes.FLASH,
                x, y, z,
                1, 0, 0, 0, 0
        );

        sl.playSound(
                null, x, y, z,
                SoundEvents.WARDEN_SONIC_BOOM,
                SoundSource.HOSTILE,
                2.0F, 0.3F
        );

        sl.playSound(
                null, x, y, z,
                SoundEvents.SCULK_SHRIEKER_SHRIEK,
                SoundSource.HOSTILE,
                2.0F, 0.5F
        );

        // 近くのプレイヤーに通知
        sl.getPlayers(p -> p.distanceToSqr(x, y, z) < 128 * 128)
                .forEach(p -> p.displayClientMessage(
                        Component.literal("§5§lOvernull §f: " + target.getName().getString()),
                        true
                ));
    }
}