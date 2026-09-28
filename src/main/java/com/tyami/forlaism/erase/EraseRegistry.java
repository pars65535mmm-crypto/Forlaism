package com.tyami.forlaism.erase;

import net.minecraft.server.MinecraftServer;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * 抹消済みUUIDのグローバル管理。
 *
 * サーバー側は EraseData (永続) と同期。
 * クライアント側はメモリのみ（受信パケットで更新）。
 */
public final class EraseRegistry {

    /** クライアント側キャッシュ。 */
    private static final Set<UUID> CLIENT_CACHE = new HashSet<>();

    private EraseRegistry() {
    }

    // =========================================================
    // サーバー側
    // =========================================================

    public static void markErased(MinecraftServer server, UUID uuid) {
        if (server != null) {
            EraseData.get(server).markErased(uuid);
        }
    }

    public static boolean isErased(MinecraftServer server, UUID uuid) {
        if (server == null || uuid == null) return false;
        return EraseData.get(server).isErased(uuid);
    }

    // =========================================================
    // クライアント側
    // =========================================================

    public static void markErasedClient(UUID uuid) {
        if (uuid != null) {
            CLIENT_CACHE.add(uuid);
        }
    }

    public static boolean isErasedClient(UUID uuid) {
        return uuid != null && CLIENT_CACHE.contains(uuid);
    }

    public static void clearClient() {
        CLIENT_CACHE.clear();
    }
}