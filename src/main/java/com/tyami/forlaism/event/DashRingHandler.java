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
 * ダッシュリングの移動速度補正。
 *
 * Curios の ring スロットに装備されているか毎tick確認し、
 * 歩行: ×1.3 / ダッシュ: ×1.3 × 1.2 のModifierを付与する。
 */
@Mod.EventBusSubscriber(modid = "forlaism")
public final class DashRingHandler {

    private static final UUID SPEED_UUID =
            UUID.fromString("d4a1c0de-1111-4222-8333-444444444401");

    private static final String SPEED_NAME = "dash_ring_speed";

    /** 歩行時の倍率。 */
    private static final double WALK_MULTIPLIER = 0.30D;

    /** ダッシュ時の追加倍率。 */
    private static final double SPRINT_MULTIPLIER = 0.56D; // 1.3 * 1.2 - 1 = 0.56

    private DashRingHandler() {
    }

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;

        Player player = event.player;
        if (player.level().isClientSide) return;

        boolean hasRing = CuriosApi.getCuriosInventory(player)
        .map(handler -> handler.findFirstCurio(
                stack -> stack.is(Items.DASH_RING.get())
                        || stack.is(Items.ROCKET_RING.get())
        ).isPresent())
        .orElse(false);

        

        AttributeInstance speed = player.getAttribute(Attributes.MOVEMENT_SPEED);
        if (speed == null) return;

        AttributeModifier existing = speed.getModifier(SPEED_UUID);

        if (!hasRing) {
            if (existing != null) {
                speed.removeModifier(SPEED_UUID);
            }
            return;
        }

        double target = player.isSprinting()
                ? SPRINT_MULTIPLIER
                : WALK_MULTIPLIER;

        if (existing != null) {
            if (existing.getAmount() == target) return;
            speed.removeModifier(SPEED_UUID);
        }

        speed.addTransientModifier(
                new AttributeModifier(
                        SPEED_UUID,
                        SPEED_NAME,
                        target,
                        AttributeModifier.Operation.MULTIPLY_TOTAL
                )
        );
    }
}