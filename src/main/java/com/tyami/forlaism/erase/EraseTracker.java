package com.tyami.forlaism.erase;

/**
 * OverOverNull 実行中フラグ。
 *
 * 実行中は全ての防御Mixinを無効化する。
 * サーバースレッドごとに独立。
 */
public final class EraseTracker {

    private static final ThreadLocal<Boolean> EXECUTING =
            ThreadLocal.withInitial(() -> false);

    private EraseTracker() {
    }

    public static boolean isExecuting() {
        return EXECUTING.get();
    }

    public static void begin() {
        EXECUTING.set(true);
    }

    public static void end() {
        EXECUTING.set(false);
    }
}