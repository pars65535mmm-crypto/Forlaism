package com.tyami.forlaism.event;

import com.tyami.forlaism.item.NoxLuxUhrwerkItem;

import net.minecraft.server.level.ServerPlayer;

import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.AttackEntityEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * ノクスルクスヴェルクの能力を毎tick・攻撃時に処理する。
 */
@Mod.EventBusSubscriber(modid = "forlaism")
public final class NoxLuxUhrwerkHandler {

    private NoxLuxUhrwerkHandler() {
    }

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        if (!(event.player instanceof ServerPlayer sp)) return;

        NoxLuxUhrwerkItem.tickPlayer(sp);
    }

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;

        var server = event.getServer();
        if (server == null) return;

        for (var level : server.getAllLevels()) {
            NoxLuxUhrwerkItem.tickStun(level);
        }
    }

    @SubscribeEvent
    public static void onAttack(AttackEntityEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        if (!(event.getTarget() instanceof net.minecraft.world.entity.LivingEntity target)) return;

        NoxLuxUhrwerkItem.onAttack(player, target);
    }
}