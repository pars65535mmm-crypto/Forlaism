package com.tyami.forlaism.util;

import net.minecraft.world.entity.LivingEntity;

/**
 * Sovereignによる強制処理の対象を一時的に保持する。
 *
 * Mixinは「今Sovereignが誰を処理しているか」を
 * このクラスから確認する。
 */
public final class SovereignExecution {

    private static final ThreadLocal<LivingEntity> TARGET =
            new ThreadLocal<>();

    private SovereignExecution() {
    }

    public static void setTarget(LivingEntity target) {
        TARGET.set(target);
    }

    public static LivingEntity getTarget() {
        return TARGET.get();
    }

    public static boolean isTarget(LivingEntity entity) {
        return TARGET.get() == entity;
    }

    public static void clear() {
        TARGET.remove();
    }
}