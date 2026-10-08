package com.tyami.forlaism.event;

import com.tyami.forlaism.world.PossibilityGlobalData;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;

import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * ハードコア死亡時に「可能性」フラグを立てる。
 *
 * 条件:
 *   1. ハードコアワールド
 *   2. ログインから10分以内
 *   3. 何も持っていない（インベントリ + 装備 + オフハンド 全部空）
 *   4. 死亡
 */
@Mod.EventBusSubscriber(modid = "forlaism")
public final class PossibilityHardcoreHandler {

    /** 10分 = 600秒 = 600_000 ミリ秒。 */
    private static final long TIME_LIMIT_MS = 10L * 60L * 1000L;

    /** プレイヤーUUID -> 初回ログイン時刻（ミリ秒）。 */
    private static final Map<UUID, Long> LOGIN_TIME = new HashMap<>();

    private PossibilityHardcoreHandler() {
    }

    // =========================================================
    // ログイン時刻記録
    // =========================================================

    @SubscribeEvent
    public static void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;

        // ハードコアワールド以外は記録しない
        if (!player.server.isHardcore()) return;

        LOGIN_TIME.putIfAbsent(player.getUUID(), System.currentTimeMillis());
    }

    @SubscribeEvent
    public static void onPlayerLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;

        // ログアウトしても保持（同じワールドに戻ってきたとき用）
        // → ただし、シングルプレイで新規ワールド作成→ログアウト→再ログインの場合、
        //    同じUUIDでもワールドが違うので、ここでは消さない。
        //    （実用上、UUIDはプレイヤーごとに固定なので問題なし）
    }

    // =========================================================
    // 死亡検知
    // =========================================================

    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onPlayerDeath(LivingDeathEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        if (!player.server.isHardcore()) return;

        // 既にフラグが立ってたら何もしない（重複防止）
        if (PossibilityGlobalData.isPending()) return;

        // ---- 時間チェック ----
        Long loginTime = LOGIN_TIME.get(player.getUUID());
        if (loginTime == null) return;

        long elapsed = System.currentTimeMillis() - loginTime;
        if (elapsed > TIME_LIMIT_MS) return;

        // ---- 「何も持っていない」チェック ----
        if (!isCompletelyEmpty(player)) return;

        // ---- 条件達成！ ----
        PossibilityGlobalData.markPending();

        // メッセージ（死亡演出と一緒に表示される）
        player.displayClientMessage(
                Component.literal("§5ただ...可能性だけは引き継がれる"),
                false
        );
    }

    // =========================================================
    // 判定
    // =========================================================

    private static boolean isCompletelyEmpty(ServerPlayer player) {
        // インベントリ 36スロット
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            ItemStack stack = player.getInventory().getItem(i);
            if (!stack.isEmpty()) return false;
        }

        // 装備 4スロット
        for (EquipmentSlot slot : new EquipmentSlot[]{
                EquipmentSlot.HEAD, EquipmentSlot.CHEST,
                EquipmentSlot.LEGS, EquipmentSlot.FEET
        }) {
            if (!player.getItemBySlot(slot).isEmpty()) return false;
        }

        // オフハンド
        if (!player.getOffhandItem().isEmpty()) return false;

        return true;
    }
}