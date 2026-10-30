package com.tyami.forlaism.util;

/**
 * ServerLevelTickControlMixin の再入防止用フラグ。
 *
 * Mixin本体は mixin パッケージ、このガードは util パッケージ。
 */
public final class ServerLevelTickControlGuard {

    private static final ThreadLocal<Boolean> ACTIVE =
            ThreadLocal.withInitial(() -> false);

    private ServerLevelTickControlGuard() {
    }

    public static boolean isActive() {
        return ACTIVE.get();
    }

    public static void BEGIN() {
        ACTIVE.set(true);
    }

    public static void END() {
        ACTIVE.set(false);
    }
}