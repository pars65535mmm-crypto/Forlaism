package com.tyami.forlaism.event;

import com.tyami.forlaism.registry.Items;

import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;

import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import top.theillusivec4.curios.api.CuriosApi;

/**
 * マリンリングのコンジットパワー付与。
 *
 * ウォーター/アクア の効果は個別ハンドラが MARINE_RING も見てるので、
 * ここはコンジットパワーだけ担当。
 */
@Mod.EventBusSubscriber(modid = "forlaism")
public final class MarineRingHandler {

    private static final int DURATION = 600;

    private MarineRingHandler() {
    }

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;

        Player player = event.player;
        if (player.level().isClientSide) return;

        boolean hasRing = CuriosApi.getCuriosInventory(player)
                .map(handler -> handler.findFirstCurio(
                        stack -> stack.is(Items.MARINE_RING.get())
                ).isPresent())
                .orElse(false);

        if (!hasRing) return;

        MobEffectInstance current = player.getEffect(MobEffects.CONDUIT_POWER);
        if (current == null || current.getDuration() < 200) {
            player.addEffect(new MobEffectInstance(
                    MobEffects.CONDUIT_POWER,
                    DURATION,
                    0,
                    false,
                    false,
                    false
            ));
        }
    }
}