package com.tyami.forlaism.event;

import com.tyami.forlaism.registry.Items;

import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

import net.minecraftforge.common.ForgeMod;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import top.theillusivec4.curios.api.CuriosApi;

import java.util.UUID;

/**
 * アクアリングの水中効果。
 *
 * - 泳ぎ速度 ×1.5 (ForgeMod.SWIM_SPEED)
 * - 水中の摩擦抵抗を軽減
 * - 水中での採掘速度低下を無効化
 */
@Mod.EventBusSubscriber(modid = "forlaism")
public final class AquaRingHandler {

    private static final UUID SWIM_UUID =
            UUID.fromString("d4a1c0de-4444-4222-8333-444444444404");

    private static final String SWIM_NAME = "aqua_ring_swim";

    /** +50%。 */
    private static final double SWIM_MULTIPLIER = 0.50D;

    /** 水中で減速しきい値（これを超えたら速度維持を試みる）。 */
    private static final double WATER_FRICTION_THRESHOLD = 0.05D;

    private AquaRingHandler() {
    }

    private static boolean hasRing(Player player) {
        return CuriosApi.getCuriosInventory(player)
                .map(handler -> handler.findFirstCurio(
                        stack -> stack.is(Items.AQUA_RING.get())
                                || stack.is(Items.MARINE_RING.get())
                ).isPresent())
                .orElse(false);
    }

    // =========================================================
    // 泳ぎ速度 + 水の抵抗軽減
    // =========================================================
    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;

        Player player = event.player;
        if (player.level().isClientSide) return;

        boolean ring = hasRing(player);

        // 泳ぎ速度属性
        AttributeInstance swim = player.getAttribute(ForgeMod.SWIM_SPEED.get());
        if (swim != null) {
            AttributeModifier existing = swim.getModifier(SWIM_UUID);

            if (!ring) {
                if (existing != null) {
                    swim.removeModifier(SWIM_UUID);
                }
            } else {
                if (existing == null) {
                    swim.addTransientModifier(
                            new AttributeModifier(
                                    SWIM_UUID,
                                    SWIM_NAME,
                                    SWIM_MULTIPLIER,
                                    AttributeModifier.Operation.MULTIPLY_TOTAL
                            )
                    );
                }
            }
        }

        // 水の抵抗軽減
        if (!ring) return;

        if (player.isInWater()) {
            Vec3 v = player.getDeltaMovement();

            // 水平方向の速度が小さすぎる場合は何もしない（ゼロ除算対策）
            double horizSpeed = Math.sqrt(v.x * v.x + v.z * v.z);

            if (horizSpeed > 0.001D) {
                // バニラは水中で速度を 0.8倍 とかにする。その減衰を軽減する
                // → 前tickの速度の95%を維持するように上書き
                double keepRatio = 0.98D;

                player.setDeltaMovement(
                        v.x * keepRatio,
                        v.y * 0.95D,  // 縦も少し維持
                        v.z * keepRatio
                );
                player.hurtMarked = true;
            }
        }
    }

    // =========================================================
    // 水中での採掘速度低下を無効化
    // =========================================================
    @SubscribeEvent
    public static void onBreakSpeed(PlayerEvent.BreakSpeed event) {
        Player player = event.getEntity();
        if (player.level().isClientSide) return;

        if (!hasRing(player)) return;

        // 水没ペナルティを受けている場合のみ補正
        // → 元の速度に 5倍 (バニラは水中で1/5) を掛ける
        if (player.isInWater() && !player.onGround()) {
            event.setNewSpeed(event.getOriginalSpeed() * 5.0F);
        }
    }
}