package com.tyami.forlaism.world;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraftforge.server.ServerLifecycleHooks;

/**
 * ナイトメア判定のヘルパー。
 *
 * 呼び出しが楽になるように色々用意しておく。
 */
public final class NightmareHelper {

    private NightmareHelper() {
    }

    /** サーバー全体でナイトメア中か。 */
    public static boolean isActive() {
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        return server != null && NightmareSavedData.isNightmare(server);
    }

    /** 指定サーバーでナイトメア中か。 */
    public static boolean isActive(MinecraftServer server) {
        return NightmareSavedData.isNightmare(server);
    }

    /** 指定Levelでナイトメア中か。 */
    public static boolean isActive(Level level) {
        if (level == null || level.isClientSide) return false;
        MinecraftServer server = level.getServer();
        return server != null && NightmareSavedData.isNightmare(server);
    }

    /** 指定Playerの所属サーバーでナイトメア中か。 */
    public static boolean isActive(Player player) {
        if (player == null) return false;
        return isActive(player.level());
    }

    /** 指定ServerLevelでナイトメア中か。 */
    public static boolean isActive(ServerLevel level) {
        return isActive((Level) level);
    }
}