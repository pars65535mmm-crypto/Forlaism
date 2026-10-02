package com.tyami.forlaism.annihilation;

import net.minecraft.server.MinecraftServer;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * サーバー側 + クライアント側の消去UUID管理。
 */
public final class GMBEraseRegistry {

    /** クライアント側キャッシュ（パケットで同期される想定だが今はローカルでも更新）。 */
    private static final Set<UUID> CLIENT_CACHE = new HashSet<>();

    private GMBEraseRegistry() {
    }

    // =========================================================
    // サーバー側
    // =========================================================

    public static void markErased(MinecraftServer server, UUID uuid) {
        if (server != null && uuid != null) {
            GMBEraseData.get(server).markErased(uuid);
        }
    }

    public static boolean isErased(MinecraftServer server, UUID uuid) {
        if (server == null || uuid == null) return false;
        return GMBEraseData.get(server).isErased(uuid);
    }

    // =========================================================
    // クライアント側
    // =========================================================

    public static void markErasedClient(UUID uuid) {
        if (uuid != null) CLIENT_CACHE.add(uuid);
    }

    public static boolean isErasedClient(UUID uuid) {
        return uuid != null && CLIENT_CACHE.contains(uuid);
    }

    public static void clearClient() {
        CLIENT_CACHE.clear();
    }
}