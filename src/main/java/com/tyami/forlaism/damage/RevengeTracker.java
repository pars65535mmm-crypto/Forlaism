package com.tyami.forlaism.damage;

import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.util.ObfuscationReflectionHelper;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * リベンジダメージの蓄積カウンター・不正Mob対策・執行マネージャー。
 *
 * 【特徴】
 *  - 内部蓄積カウンター（0 ~ 100）: ダメージを与えるごとに加算され、100に達すると確定死亡
 *  - 輪廻ダメージ完全内包: 最大HP削り・ロック、回復阻害、無敵時間無視、防具耐性無視
 *  - 不正対策: die(), discard(), remove() の無効化や、HP常時1000固定Mobに対抗し、
 *    確実に生命活動を停止・除去する
 */
@Mod.EventBusSubscriber(modid = "forlaism")
public final class RevengeTracker {

    /** 各Entityのリベンジダメージ蓄積値（0.0 ~ 100.0） */
    private static final Map<UUID, Float> REVENGE_GAUGE = new ConcurrentHashMap<>();

    /** 回復阻害対象のUUID */
    private static final Set<UUID> REVENGE_BLOCKED_RECOVERY = ConcurrentHashMap.newKeySet();

    /** 処刑済み・死確定MobのUUID */
    private static final Set<UUID> EXECUTED_ENTITIES = ConcurrentHashMap.newKeySet();

    // リフレクションによる内部変数アクセス
    private static EntityDataAccessor<Float> DATA_HEALTH;
    private static Field FIELD_DEAD;
    private static Field FIELD_DEATH_TIME;
    private static Method METHOD_ON_DEATH;
    private static Method METHOD_DROP_ALL_DEATH_LOOT;
    private static boolean REFLECTION_INIT = false;

    private RevengeTracker() {
    }

    private static synchronized void ensureReflectionInit() {
        if (REFLECTION_INIT) return;
        try {
            Field idField;
            try {
                idField = LivingEntity.class.getDeclaredField("DATA_HEALTH_ID");
            } catch (Throwable t) {
                idField = ObfuscationReflectionHelper.findField(LivingEntity.class, "f_20883_");
            }
            idField.setAccessible(true);
            DATA_HEALTH = (EntityDataAccessor<Float>) idField.get(null);

            try {
                FIELD_DEAD = ObfuscationReflectionHelper.findField(LivingEntity.class, "f_20919_");
                FIELD_DEATH_TIME = ObfuscationReflectionHelper.findField(LivingEntity.class, "f_20923_");
            } catch (Throwable t) {
                FIELD_DEAD = LivingEntity.class.getDeclaredField("dead");
                FIELD_DEATH_TIME = LivingEntity.class.getDeclaredField("deathTime");
            }
            FIELD_DEAD.setAccessible(true);
            FIELD_DEATH_TIME.setAccessible(true);

            METHOD_ON_DEATH = ObfuscationReflectionHelper.findMethod(LivingEntity.class, "m_6667_", DamageSource.class);
            METHOD_ON_DEATH.setAccessible(true);

            METHOD_DROP_ALL_DEATH_LOOT = ObfuscationReflectionHelper.findMethod(LivingEntity.class, "m_6668_", DamageSource.class);
            METHOD_DROP_ALL_DEATH_LOOT.setAccessible(true);

            REFLECTION_INIT = true;
        } catch (Throwable t) {
            System.err.println("[Forlaism] RevengeTracker reflection init failed: " + t);
            REFLECTION_INIT = true;
        }
    }

    /**
     * リベンジダメージの実処理。
     */
    public static void applyRevengeDamage(LivingEntity target, DamageSource source, float amount) {
        if (target == null || target.level().isClientSide) return;
        UUID uuid = target.getUUID();

        // 1. 最大HP削り・固定（輪廻同等）
        AttributeInstance maxHp = target.getAttribute(Attributes.MAX_HEALTH);
        if (maxHp != null) {
            if (!RinneRecoveryBlocker.isMaxHealthLocked(maxHp)) {
                double currentMax = maxHp.getBaseValue();
                double nextMax = Math.max(0.0D, currentMax - amount);
                RinneRecoveryBlocker.lockMaxHealth(maxHp, nextMax);
            } else {
                Double locked = RinneRecoveryBlocker.getLockedValue(maxHp);
                if (locked != null) {
                    double nextMax = Math.max(0.0D, locked - amount);
                    RinneRecoveryBlocker.unlockMaxHealth(maxHp);
                    RinneRecoveryBlocker.lockMaxHealth(maxHp, nextMax);
                }
            }
        }

        // 2. 現在HPの直接削減
        float newHealth = Math.max(0.0F, target.getHealth() - amount);
        target.setHealth(newHealth);

        // 3. 回復阻害を永続付与
        RinneRecoveryBlocker.blockRecovery(uuid);
        REVENGE_BLOCKED_RECOVERY.add(uuid);

        // 4. 被弾演出・無敵時間完全リセット
        target.invulnerableTime = 0;
        target.hurtTime = 10;
        target.hurtDuration = 10;

        if (source.getEntity() instanceof Player p) {
            target.setLastHurtByPlayer(p);
        }

        // 5. 独自リベンジ蓄積カウンターの加算
        // ダメージ量に応じつつ、最低でも1打撃あたり10加算される
        float addGauge = Math.max(10.0F, amount);
        float currentGauge = REVENGE_GAUGE.getOrDefault(uuid, 0.0F) + addGauge;
        REVENGE_GAUGE.put(uuid, currentGauge);

        // 6. 死亡判定: HPが0以下、または独自変数が100以上に達した時
        if (newHealth <= 0.0F || currentGauge >= 100.0F) {
            executeRevengeDeath(target, source);
        }
    }

    /**
     * 不正Mob（die/discard/remove空実装やHP常時固定）をも貫通する確定処刑処理。
     */
    public static void executeRevengeDeath(LivingEntity target, DamageSource source) {
        if (target == null || target.level().isClientSide) return;
        UUID uuid = target.getUUID();
        EXECUTED_ENTITIES.add(uuid);
        REVENGE_GAUGE.remove(uuid);

        ensureReflectionInit();

        // 1. 同期データ層のHPを0Fに固定
        try {
            if (DATA_HEALTH != null) {
                target.getEntityData().set(DATA_HEALTH, 0.0F);
            }
        } catch (Throwable ignored) {}

        target.setHealth(0.0F);

        // 2. dead フラグをセット
        try {
            if (FIELD_DEAD != null) FIELD_DEAD.setBoolean(target, true);
            if (FIELD_DEATH_TIME != null) FIELD_DEATH_TIME.setInt(target, 0);
        } catch (Throwable ignored) {}

        // 3. 死亡イベント演出パケット
        target.level().broadcastEntityEvent(target, (byte) 3);

        // 4. die() メソッドの実行
        try {
            if (METHOD_ON_DEATH != null) {
                METHOD_ON_DEATH.invoke(target, source);
            } else {
                target.die(source);
            }
        } catch (Throwable ignored) {}

        try {
            if (METHOD_DROP_ALL_DEATH_LOOT != null) {
                METHOD_DROP_ALL_DEATH_LOOT.invoke(target, source);
            }
        } catch (Throwable ignored) {}

        // 5. discard() および remove() の強制呼び出し
        try { target.discard(); } catch (Throwable ignored) {}
        try { target.remove(Entity.RemovalReason.KILLED); } catch (Throwable ignored) {}

        // 6. それでも消えない不正Mobに対して、ワールドから物理的除去
        if (target.level() instanceof ServerLevel serverLevel) {
            serverLevel.getServer().execute(() -> {
                try {
                    target.remove(Entity.RemovalReason.KILLED);
                } catch (Throwable ignored) {}
            });
        }
    }

    public static boolean isRecoveryBlocked(UUID uuid) {
        return REVENGE_BLOCKED_RECOVERY.contains(uuid);
    }

    public static float getRevengeGauge(UUID uuid) {
        return REVENGE_GAUGE.getOrDefault(uuid, 0.0F);
    }

    // =========================================================
    // 不正Mob監視（HPを毎tick強制回復するMob等の封殺）
    // =========================================================

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onLivingTick(LivingEvent.LivingTickEvent event) {
        LivingEntity entity = event.getEntity();
        if (entity.level().isClientSide) return;
        UUID uuid = entity.getUUID();

        // 既に処刑済みなのに生き残っているチートMobを即刻排除
        if (EXECUTED_ENTITIES.contains(uuid)) {
            entity.setHealth(0.0F);
            entity.discard();
            entity.remove(Entity.RemovalReason.KILLED);
            return;
        }

        // 回復阻害対象がHPを増やそうとした場合、最大HPおよび現在HPを0F側へ抑え込む
        if (REVENGE_BLOCKED_RECOVERY.contains(uuid)) {
            AttributeInstance maxHp = entity.getAttribute(Attributes.MAX_HEALTH);
            if (maxHp != null) {
                Double locked = RinneRecoveryBlocker.getLockedValue(maxHp);
                if (locked != null && entity.getHealth() > locked.floatValue()) {
                    entity.setHealth(Math.max(0.0F, locked.floatValue()));
                }
            }
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onEntityJoin(EntityJoinLevelEvent event) {
        if (event.getLevel().isClientSide) return;
        UUID uuid = event.getEntity().getUUID();
        if (EXECUTED_ENTITIES.contains(uuid)) {
            event.setCanceled(true);
            event.getEntity().discard();
        }
    }
}
