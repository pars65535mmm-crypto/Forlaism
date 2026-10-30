package com.tyami.forlaism.world;

import net.minecraft.server.MinecraftServer;

/**
 * /tick コマンド相当の薄いラッパー。
 * 実体は WorldTimeFreezeManager 側。
 */
public final class ClockTickManager {

    private ClockTickManager() {}

    public static void freeze(MinecraftServer server) {
        if (!WorldTimeFreezeManager.isFrozen()) {
            WorldTimeFreezeManager.setFrozen(server, true, null);
        }
    }

    public static void unfreeze(MinecraftServer server) {
        if (WorldTimeFreezeManager.isFrozen()) {
            WorldTimeFreezeManager.setFrozen(server, false, null);
        }
    }

    public static boolean isFrozen(MinecraftServer server) {
        return WorldTimeFreezeManager.isFrozen();
    }

    public static void setRate(MinecraftServer server, float rate) {
        WorldTimeFreezeManager.setTickRate(server, rate);
    }

    public static float getRate(MinecraftServer server) {
        return WorldTimeFreezeManager.getTickRate();
    }

    public static void step(MinecraftServer server, int ticks) {
        WorldTimeFreezeManager.requestStep(server, ticks);
    }

    public static void sprint(MinecraftServer server, int ticks) {
        WorldTimeFreezeManager.requestSprint(server, ticks);
    }

    public static void sprintStop(MinecraftServer server) {
        WorldTimeFreezeManager.stopSprint(server);
    }

    public static boolean isSprinting(MinecraftServer server) {
        return WorldTimeFreezeManager.isSprinting();
    }
}