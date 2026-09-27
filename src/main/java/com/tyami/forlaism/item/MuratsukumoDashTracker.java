package com.tyami.forlaism.item;

import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * ムラツクモの突進状態を毎tick管理する。
 *
 * 突進中は:
 *   - 追加加速
 *   - 周囲へダメージ
 *   - パーティクル
 * を onDashTick で実行する。
 */
@Mod.EventBusSubscriber(modid = "forlaism")
public final class MuratsukumoDashTracker {

    /** 突進残りtickのタグ。 */
    public static final String TAG_DASH_TICKS = "MuratsukumoDashTicks";

    private MuratsukumoDashTracker() {
    }

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {

        if (event.phase != TickEvent.Phase.END) return;

        Player player = event.player;

        if (player.level().isClientSide) return;

        var data = player.getPersistentData();
        int ticks = data.getInt(TAG_DASH_TICKS);

        if (ticks <= 0) return;

        // 突進処理
        MuratsukumoItem.onDashTick(player);

        ticks--;

        if (ticks <= 0) {
            data.remove(TAG_DASH_TICKS);
        } else {
            data.putInt(TAG_DASH_TICKS, ticks);
        }
    }
}