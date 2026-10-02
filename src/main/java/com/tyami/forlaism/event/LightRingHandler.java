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
 * ライトリングの暗視付与。
 *
 * 装備中は毎tick 暗視を短時間付与する。
 * → 効果が消えそうになったら自動再付与される。
 */
@Mod.EventBusSubscriber(modid = "forlaism")
public final class LightRingHandler {

    /** 効果時間 (tick)。60秒 = 1200tick。 */
    private static final int DURATION = 1200;

    private LightRingHandler() {
    }

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {

        if (event.phase != TickEvent.Phase.END) return;

        Player player = event.player;
        if (player.level().isClientSide) return;

        boolean hasRing = CuriosApi.getCuriosInventory(player)
                .map(handler -> handler.findFirstCurio(
                        stack -> stack.is(Items.LIGHT_RING.get())
                ).isPresent())
                .orElse(false);

        if (!hasRing) return;

        // 残り時間が少なくなったら再付与
        MobEffectInstance current = player.getEffect(MobEffects.NIGHT_VISION);
        if (current == null || current.getDuration() < 200) {
            player.addEffect(new MobEffectInstance(
                    MobEffects.NIGHT_VISION,
                    DURATION,
                    0,
                    false,
                    false,
                    false
            ));
        }
    }
}