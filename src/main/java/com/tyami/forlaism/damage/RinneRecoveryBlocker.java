package com.tyami.forlaism.damage;

import net.minecraft.world.entity.ai.attributes.AttributeInstance;

import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.WeakHashMap;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 輪廻ダメージを受けた Entity の
 * 「最大HP固定」と「回復阻害」を管理する。
 *
 * - MAX_HEALTH は WeakHashMap で AttributeInstance に紐づけて固定
 * - 回復阻害は UUID の Set で管理（永続）
 *
 * Entity が消滅すれば AttributeInstance が GC され、
 * WeakHashMap のエントリも自動で消える。
 */
public final class RinneRecoveryBlocker {

    /**
     * MAX_HEALTH固定用。
     */
    private static final Map<AttributeInstance, Double> LOCKED_MAX_HEALTH =
            Collections.synchronizedMap(new WeakHashMap<>());

    /**
     * 回復阻害フラグ（永続）。
     */
    private static final Set<UUID> RECOVERY_BLOCKED =
            ConcurrentHashMap.newKeySet();

    private RinneRecoveryBlocker() {
    }

    // =========================================================
    // MAX_HEALTH 固定
    // =========================================================

    /**
     * MAX_HEALTH を固定する。
     */
    public static void lockMaxHealth(AttributeInstance instance, double value) {
        if (instance == null) return;

        LOCKED_MAX_HEALTH.put(instance, value);

        // 即座に固定値へ
        instance.setBaseValue(value);
    }

    /**
     * 固定を解除する（内部用：再ロック時に使用）。
     */
    public static void unlockMaxHealth(AttributeInstance instance) {
        if (instance == null) return;
        LOCKED_MAX_HEALTH.remove(instance);
    }

    /**
     * この AttributeInstance が固定済みか。
     */
    public static boolean isMaxHealthLocked(AttributeInstance instance) {
        if (instance == null) return false;
        return LOCKED_MAX_HEALTH.containsKey(instance);
    }

    /**
     * 固定されている値を取得（未固定なら null）。
     */
    public static Double getLockedValue(AttributeInstance instance) {
        if (instance == null) return null;
        return LOCKED_MAX_HEALTH.get(instance);
    }

    // =========================================================
    // 回復阻害
    // =========================================================

    public static void blockRecovery(UUID uuid) {
        if (uuid == null) return;
        RECOVERY_BLOCKED.add(uuid);
    }

    public static boolean isRecoveryBlocked(UUID uuid) {
        if (uuid == null) return false;
        return RECOVERY_BLOCKED.contains(uuid);
    }

    public static void clearRecovery(UUID uuid) {
        if (uuid == null) return;
        RECOVERY_BLOCKED.remove(uuid);
    }
}