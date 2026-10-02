package com.tyami.forlaism.event;

import com.tyami.forlaism.Forlaism;
import com.tyami.forlaism.item.EndWardenSummonerItem;
import com.tyami.forlaism.item.EndWardenSummonerReverseItem;
import com.tyami.forlaism.registry.Items;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * 召喚符をクラフトした瞬間に EndWarden を召喚する。
 */
@Mod.EventBusSubscriber(modid = Forlaism.MOD_ID)
public final class EndWardenSummonHandler {

    private EndWardenSummonHandler() {
    }

    @SubscribeEvent
    public static void onItemCrafted(PlayerEvent.ItemCraftedEvent event) {

        ItemStack crafted = event.getCrafting();
        if (crafted.isEmpty()) return;

        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        if (!(player.level() instanceof ServerLevel level)) return;

        // ---- 通常版 ----
        if (crafted.is(Items.END_WARDEN_SUMMONER.get())) {
            EndWardenSummonerItem.summonEndWarden(level, player);
            removeFromInventory(level, player, Items.END_WARDEN_SUMMONER.get());
            return;
        }

        // ---- リバース版 ----
        if (crafted.is(Items.END_WARDEN_SUMMONER_REVERSE.get())) {
            EndWardenSummonerReverseItem.summonEndWardenReverse(level, player);
            removeFromInventory(level, player, Items.END_WARDEN_SUMMONER_REVERSE.get());
            return;
        }
    }

    /**
     * 召喚符をプレイヤーのインベントリから消す（次tick実行）。
     */
    private static void removeFromInventory(ServerLevel level, ServerPlayer player, net.minecraft.world.item.Item item) {
        level.getServer().execute(() -> {
            var inv = player.getInventory();
            for (int i = 0; i < inv.getContainerSize(); i++) {
                ItemStack s = inv.getItem(i);
                if (s.is(item)) {
                    inv.setItem(i, ItemStack.EMPTY);
                }
            }
        });
    }
}