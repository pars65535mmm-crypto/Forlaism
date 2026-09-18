package com.tyami.forlaism.quantum;

import net.minecraft.server.MinecraftServer;

/**
 * サーバー全体（全ディメンション共有）の量子転送ネットワーク。
 *
 * 実体は QuantumTransferData (SavedData) 側に持たせる。
 */
public final class QuantumTransferNetwork {

    public static final int MIN_NETWORK = 1;
    public static final int MAX_NETWORK = 255;

    private static MinecraftServer serverRef;

    private QuantumTransferNetwork() {
    }

    /** サーバー起動時に呼ぶ。 */
    public static void setServer(MinecraftServer server) {
        serverRef = server;
    }

    public static QuantumPool getPool(int network) {
        int n = clamp(network);
        if (serverRef == null) {
            // サーバー未起動時のフォールバック（開発用）
            return FALLBACK.computeIfAbsent(n, k -> new QuantumPool());
        }
        return QuantumTransferData.get(serverRef).getPool(n);
    }

    public static int clamp(int network) {
        if (network < MIN_NETWORK) return MAX_NETWORK;
        if (network > MAX_NETWORK) return MIN_NETWORK;
        return network;
    }

    public static int next(int network) {
        int n = network + 1;
        return n > MAX_NETWORK ? MIN_NETWORK : n;
    }

    public static int prev(int network) {
        int n = network - 1;
        return n < MIN_NETWORK ? MAX_NETWORK : n;
    }

    // サーバー未起動時のフォールバック
    private static final java.util.Map<Integer, QuantumPool> FALLBACK =
            new java.util.concurrent.ConcurrentHashMap<>();
}