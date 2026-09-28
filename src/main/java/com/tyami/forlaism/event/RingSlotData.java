package com.tyami.forlaism.event;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;

/**
 * プレイヤーごとの「追加リングスロット数」を保持する。
 *
 * PlayerのpersistentDataに保存するので、
 * ワールド保存・ログイン/ログアウトを跨いで永続する。
 */
public final class RingSlotData {

    private static final String TAG_EXTRA_RING_SLOTS = "ForlaismExtraRingSlots";

    private RingSlotData() {
    }

    /** 追加スロット数を加算する。 */
    public static void addExtraSlots(ServerPlayer player, int amount) {
        CompoundTag data = player.getPersistentData();
        int current = data.getInt(TAG_EXTRA_RING_SLOTS);
        data.putInt(TAG_EXTRA_RING_SLOTS, current + amount);
    }

    /** 現在の追加スロット数を取得。 */
    public static int getExtraSlots(ServerPlayer player) {
        return player.getPersistentData().getInt(TAG_EXTRA_RING_SLOTS);
    }
}