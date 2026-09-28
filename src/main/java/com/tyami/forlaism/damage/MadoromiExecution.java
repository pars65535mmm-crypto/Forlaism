package com.tyami.forlaism.damage;

import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.fml.util.ObfuscationReflectionHelper;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

public final class MadoromiExecution {

    private MadoromiExecution() {
    }

    // HPの同期データキー（これ自体をリフレクションで引っこ抜く）
    private static EntityDataAccessor<Float> DATA_HEALTH;
    
    private static Field FIELD_DEAD;
    private static Field FIELD_DEATH_TIME;
    private static Method METHOD_ON_DEATH;
    private static Method METHOD_DROP_ALL_DEATH_LOOT;

    private static boolean INITIALIZED = false;
    private static boolean INIT_FAILED = false;

    private static synchronized void ensureInitialized() {
        if (INITIALIZED || INIT_FAILED) return;

        try {
            // 1. HPを管理している大元の同期キー(DATA_HEALTH_ID)を取得
            // MCP名: DATA_HEALTH_ID  /  SRG名: f_20883_
            Field idField;
            try {
                idField = LivingEntity.class.getDeclaredField("DATA_HEALTH_ID");
            } catch (Throwable t) {
                idField = ObfuscationReflectionHelper.findField(LivingEntity.class, "f_20883_");
            }
            idField.setAccessible(true);
            DATA_HEALTH = (EntityDataAccessor<Float>) idField.get(null);

            // 2. 死亡フラグ・死亡時間のフィールド取得
            try {
                FIELD_DEAD = ObfuscationReflectionHelper.findField(LivingEntity.class, "f_20919_"); // dead
                FIELD_DEATH_TIME = ObfuscationReflectionHelper.findField(LivingEntity.class, "f_20923_"); // deathTime
            } catch (Throwable t) {
                FIELD_DEAD = LivingEntity.class.getDeclaredField("dead");
                FIELD_DEATH_TIME = LivingEntity.class.getDeclaredField("deathTime");
            }
            FIELD_DEAD.setAccessible(true);
            FIELD_DEATH_TIME.setAccessible(true);

            // 3. ロジック用の内部メソッド取得
            METHOD_ON_DEATH = ObfuscationReflectionHelper.findMethod(LivingEntity.class, "m_6667_", DamageSource.class); // die
            METHOD_ON_DEATH.setAccessible(true);

            METHOD_DROP_ALL_DEATH_LOOT = ObfuscationReflectionHelper.findMethod(LivingEntity.class, "m_6668_", DamageSource.class); // dropAllDeathLoot
            METHOD_DROP_ALL_DEATH_LOOT.setAccessible(true);

            INITIALIZED = true;
        } catch (Throwable t) {
            INIT_FAILED = true;
            System.err.println("[Forlaism] MadoromiExecution init failed: " + t);
        }
    }

    public static boolean execute(LivingEntity target, DamageSource source) {
        if (target == null) return false;
        if (target.level().isClientSide) return false;
        if (target.isDeadOrDying()) return false;
        if (target instanceof Player p && p.isCreative()) return false;

        ensureInitialized();
        if (INIT_FAILED) return false;

        MadoromiDamageSource.beginExecution();
        try {
            // ★超重要: ネットワーク同期データ層のHPをリフレクションで直接0Fにする
            // これにより、イベント（HurtEvent等）を一切挟まず、光輪の防御を完全に貫通してHPが0になります。
            target.getEntityData().set(DATA_HEALTH, 0.0F);

            // 2. dead フラグを立ててシステム的に死亡状態にする
            try { FIELD_DEAD.setBoolean(target, true); } catch (Throwable ignored) {}
            try { FIELD_DEATH_TIME.setInt(target, 0); } catch (Throwable ignored) {}

            // 3. 演出パケットを送信（クライアント側に「こいつは死んだ」と強制認識させる）
            target.level().broadcastEntityEvent(target, (byte) 3); 

            // 4. 後処理（アイテムドロップと内部死亡処理の実行）
            try { METHOD_ON_DEATH.invoke(target, source); } catch (Throwable ignored) {}
            try { METHOD_DROP_ALL_DEATH_LOOT.invoke(target, source); } catch (Throwable ignored) {}

            return true;
        } catch (Throwable t) {
            return false;
        } finally {
            MadoromiDamageSource.endExecution();
        }
    }
}
