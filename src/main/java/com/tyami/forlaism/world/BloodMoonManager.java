package com.tyami.forlaism.world;

import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * ブラッドムーンの状態管理。
 *
 * ワールドごとに「発動中の開始tick」を持つ。
 */
public final class BloodMoonManager {

    private static final Map<ResourceKey<Level>, Long> ACTIVE = new ConcurrentHashMap<>();

    private BloodMoonManager() {
    }

    public static void start(ServerLevel level) {
        ACTIVE.put(level.dimension(), level.getGameTime());
    }

    public static void stop(ServerLevel level) {
        ACTIVE.remove(level.dimension());
    }

    public static void stopAll() {
        ACTIVE.clear();
    }

    public static boolean isActive(ServerLevel level) {
        return ACTIVE.containsKey(level.dimension());
    }

    public static boolean isActive(Level level) {
        if (!(level instanceof ServerLevel sl)) return false;
        return isActive(sl);
    }

    public static long getStartTick(ServerLevel level) {
        return ACTIVE.getOrDefault(level.dimension(), -1L);
    }

    /** 13000〜23000 を夜とみなす。 */
    public static boolean isNight(ServerLevel level) {
        long time = level.getDayTime() % 24000L;
        return time >= 13000L && time < 23000L;
    }

    // =========================================================
    // クライアント側フラグ（パケットで更新）
    // =========================================================

    private static volatile boolean clientActive = false;

    public static void setClientActive(boolean active) {
        clientActive = active;
    }

    public static boolean isClientActive() {
        return clientActive;
    }
}