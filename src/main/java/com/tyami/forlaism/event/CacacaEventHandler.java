package com.tyami.forlaism.event;

import com.tyami.forlaism.item.CacacaItem;

import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * カッカオカッカオを持っている間のジャンプ無限処理。
 */
@Mod.EventBusSubscriber(modid = "forlaism")
public final class CacacaEventHandler {

    private CacacaEventHandler() {
    }

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;

        Player player = event.player;
        if (player.level().isClientSide) return;

        CacacaItem.tickJumpReset(player);
    }
}