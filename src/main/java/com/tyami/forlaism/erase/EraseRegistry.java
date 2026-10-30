package com.tyami.forlaism.erase;

import net.minecraft.server.MinecraftServer;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 抹消済みUUIDのグローバル管理。
 *
 * 【軽量化】
 *   旧: 毎回 EraseData.get(server) を叩いて SavedData を検索（激重）
 *   新: サーバーごとのメモリキャッシュを保持し、O(1) で判定
 */
public final class EraseRegistry {

    /** クライアント側キャッシュ。 */
    private static final Set<UUID> CLIENT_CACHE = new HashSet<>();

    /** サーバーごとのメモリキャッシュ（ワールド再起動までは保持）。 */
    private static final Set<UUID> SERVER_CACHE = Collections.newSetFromMap(new ConcurrentHashMap<>());

    /** 初期化済みフラグ。 */
    private static volatile boolean LOADED = false;

    private EraseRegistry() {
    }

    // =========================================================
    // サーバー側
    // =========================================================

    public static void markErased(MinecraftServer server, UUID uuid) {
        if (uuid != null) {
            SERVER_CACHE.add(uuid);
        }
        if (server != null && uuid != null) {
            EraseData.get(server).markErased(uuid);
        }
    }

    public static boolean isErased(MinecraftServer server, UUID uuid) {
        if (uuid == null) return false;

        // メモリキャッシュで即判定
        if (SERVER_CACHE.contains(uuid)) {
            return true;
        }

        // 起動後1回だけ SavedData から読み込み
        if (!LOADED && server != null) {
            LOADED = true;
            try {
                SERVER_CACHE.addAll(EraseData.get(server).snapshot());
            } catch (Throwable ignored) {
            }
        }

        return SERVER_CACHE.contains(uuid);
    }

    /** サーバー起動時に呼ぶ（明示的な初期化）。 */
    public static void loadFrom(MinecraftServer server) {
        if (server == null) return;
        try {
            SERVER_CACHE.clear();
            SERVER_CACHE.addAll(EraseData.get(server).snapshot());
            LOADED = true;
        } catch (Throwable ignored) {
        }
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