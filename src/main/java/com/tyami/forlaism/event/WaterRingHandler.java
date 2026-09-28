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
 * ウォーターリングの水中呼吸付与。
 */
@Mod.EventBusSubscriber(modid = "forlaism")
public final class WaterRingHandler {

    private static final int DURATION = 1200;

    private WaterRingHandler() {
    }

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;

        Player player = event.player;
        if (player.level().isClientSide) return;

        boolean hasRing = CuriosApi.getCuriosInventory(player)
                .map(handler -> handler.findFirstCurio(
                        stack -> stack.is(Items.WATER_RING.get())
                                || stack.is(Items.MARINE_RING.get())
                ).isPresent())
                .orElse(false);

        if (!hasRing) return;

        MobEffectInstance current = player.getEffect(MobEffects.WATER_BREATHING);
        if (current == null || current.getDuration() < 200) {
            player.addEffect(new MobEffectInstance(
                    MobEffects.WATER_BREATHING,
                    DURATION,
                    0,
                    false,
                    false,
                    false
            ));
        }
    }
}