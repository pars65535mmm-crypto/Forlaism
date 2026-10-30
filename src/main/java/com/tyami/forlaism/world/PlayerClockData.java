package com.tyami.forlaism.world;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;

/**
 * プレイヤーごとのクロック設定を永続化。
 *
 * PlayerのpersistentDataに保存するので、
 * ワールド保存・ログイン/ログアウトを跨いで残る。
 */
public final class PlayerClockData {

    private static final String TAG_AUTO_TOTEM = "ForlaismClockAutoTotem";
    private static final String TAG_AUTO_AIM = "ForlaismClockAutoAim";
    private static final String TAG_AIM_TARGET = "ForlaismClockAimTarget";
    private static final String TAG_AIM_HOLD = "ForlaismClockAimHold";

    private PlayerClockData() {
    }

    // =========================================================
    // オートトーテム
    // =========================================================

    public static boolean isAutoTotem(ServerPlayer player) {
        return player.getPersistentData().getBoolean(TAG_AUTO_TOTEM);
    }

    public static void setAutoTotem(ServerPlayer player, boolean value) {
        player.getPersistentData().putBoolean(TAG_AUTO_TOTEM, value);
    }

    public static void toggleAutoTotem(ServerPlayer player) {
        setAutoTotem(player, !isAutoTotem(player));
    }

    // =========================================================
    // オートエイム
    // =========================================================

    public static boolean isAutoAim(ServerPlayer player) {
        return player.getPersistentData().getBoolean(TAG_AUTO_AIM);
    }

    public static void setAutoAim(ServerPlayer player, boolean value) {
        player.getPersistentData().putBoolean(TAG_AUTO_AIM, value);
    }

    public static void toggleAutoAim(ServerPlayer player) {
        setAutoAim(player, !isAutoAim(player));
    }

    // =========================================================
    // エイムロック対象
    // =========================================================

    public static void setAimTarget(ServerPlayer player, java.util.UUID targetId) {
        if (targetId == null) {
            player.getPersistentData().remove(TAG_AIM_TARGET);
        } else {
            player.getPersistentData().putUUID(TAG_AIM_TARGET, targetId);
        }
    }

    public static java.util.UUID getAimTarget(ServerPlayer player) {
        CompoundTag tag = player.getPersistentData();
        if (!tag.hasUUID(TAG_AIM_TARGET)) return null;
        return tag.getUUID(TAG_AIM_TARGET);
    }

    public static boolean hasAimTarget(ServerPlayer player) {
        return player.getPersistentData().hasUUID(TAG_AIM_TARGET);
    }

    // =========================================================
    // Shiftホールドカウンタ
    // =========================================================

    public static int getAimHold(ServerPlayer player) {
        return player.getPersistentData().getInt(TAG_AIM_HOLD);
    }

    public static void setAimHold(ServerPlayer player, int ticks) {
        player.getPersistentData().putInt(TAG_AIM_HOLD, ticks);
    }

    public static void resetAimHold(ServerPlayer player) {
        player.getPersistentData().putInt(TAG_AIM_HOLD, 0);
    }
}