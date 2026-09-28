package com.tyami.forlaism.event;

import net.minecraft.server.level.ServerPlayer;

import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import top.theillusivec4.curios.api.CuriosApi;

/**
 * リングオブリングの効果をログイン時に復元する。
 *
 * Curiosのスロット数はセッションごとにリセットされるため、
 * プレイヤーの永続データに保存した「追加スロット数」を
 * ログインのたびに再適用する。
 */
@Mod.EventBusSubscriber(modid = "forlaism")
public final class RingOfRingsHandler {

    private RingOfRingsHandler() {
    }

    @SubscribeEvent
    public static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {

        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }

        int extra = RingSlotData.getExtraSlots(player);
        if (extra <= 0) {
            return;
        }

        // ログイン時点で既に足りてるかチェック
        CuriosApi.getCuriosInventory(player).ifPresent(handler -> {

            var slot = handler.getStacksHandler("ring").orElse(null);
            if (slot == null) return;

            int current = slot.getSlots();
            // デフォルト2 + extra を目標にする
            int target = 2 + extra;

            if (current < target) {
                handler.growSlotType("ring", target - current);
            }
        });
    }
}