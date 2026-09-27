package com.tyami.forlaism.event;

import net.minecraft.server.level.ServerPlayer;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * 核の自爆時に「リスポーンでリンネディウムを付与する」予約を保持する。
 */
public final class PendingRinnediumData {

    /** プレイヤーUUID → 付与個数。 */
    private static final Map<UUID, Integer> PENDING = new HashMap<>();

    private PendingRinnediumData() {
    }

    public static void schedule(ServerPlayer player, int count) {
        PENDING.put(player.getUUID(), count);
    }

    /** 予約を取り出して削除。なければ0。 */
    public static int consume(ServerPlayer player) {
        Integer count = PENDING.remove(player.getUUID());
        return count == null ? 0 : count;
    }

    public static boolean has(ServerPlayer player) {
        return PENDING.containsKey(player.getUUID());
    }
}