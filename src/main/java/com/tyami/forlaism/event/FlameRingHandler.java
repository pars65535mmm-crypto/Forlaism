package com.tyami.forlaism.event;

import com.tyami.forlaism.registry.Items;

import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;

import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import top.theillusivec4.curios.api.CuriosApi;

import java.util.UUID;

/**
 * フレイムリングの攻撃力補正。
 *
 * 装備中は ATTACK_DAMAGE に ×1.5 (MULTIPLY_TOTAL 0.5) を付与する。
 */
@Mod.EventBusSubscriber(modid = "forlaism")
public final class FlameRingHandler {

    private static final UUID ATTACK_UUID =
            UUID.fromString("d4a1c0de-3333-4222-8333-444444444403");

    private static final String ATTACK_NAME = "flame_ring_attack";

    /** +50%。 */
    private static final double ATTACK_MULTIPLIER = 0.50D;

    private FlameRingHandler() {
    }

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;

        Player player = event.player;
        if (player.level().isClientSide) return;

        boolean hasRing = CuriosApi.getCuriosInventory(player)
                .map(handler -> handler.findFirstCurio(
                        stack -> stack.is(Items.FLAME_RING.get())
                ).isPresent())
                .orElse(false);

        AttributeInstance attack = player.getAttribute(Attributes.ATTACK_DAMAGE);
        if (attack == null) return;

        AttributeModifier existing = attack.getModifier(ATTACK_UUID);

        if (!hasRing) {
            if (existing != null) {
                attack.removeModifier(ATTACK_UUID);
            }
            return;
        }

        if (existing == null) {
            attack.addTransientModifier(
                    new AttributeModifier(
                            ATTACK_UUID,
                            ATTACK_NAME,
                            ATTACK_MULTIPLIER,
                            AttributeModifier.Operation.MULTIPLY_TOTAL
                    )
            );
        }
    }
}