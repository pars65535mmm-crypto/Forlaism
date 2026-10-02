package com.tyami.forlaism.event;

import com.tyami.forlaism.registry.Items;

import net.minecraft.world.entity.player.Player;

import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import top.theillusivec4.curios.api.CuriosApi;

/**
 * フェザーリングの落下ダメージ無効。
 *
 * 装備中は毎tick fallDistance を 0 にリセットする。
 * → どんな高さから落ちても落下ダメージが発生しない。
 */
@Mod.EventBusSubscriber(modid = "forlaism")
public final class FeatherRingHandler {

    private FeatherRingHandler() {
    }

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {

        if (event.phase != TickEvent.Phase.END) return;

        Player player = event.player;
        if (player.level().isClientSide) return;

        boolean hasRing = CuriosApi.getCuriosInventory(player)
                .map(handler -> handler.findFirstCurio(
                        stack -> stack.is(Items.FEATHER_RING.get())
                ).isPresent())
                .orElse(false);

        if (!hasRing) return;

        // 落下距離をリセット
        player.fallDistance = 0.0F;
    }
}