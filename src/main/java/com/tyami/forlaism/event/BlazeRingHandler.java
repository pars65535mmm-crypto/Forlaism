package com.tyami.forlaism.event;

import com.tyami.forlaism.registry.Items;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

import net.minecraftforge.event.entity.player.AttackEntityEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import top.theillusivec4.curios.api.CuriosApi;

/**
 * ブレイズリングの攻撃効果。
 *
 * 攻撃時に相手を5秒燃やし、追加3ダメージ。
 */
@Mod.EventBusSubscriber(modid = "forlaism")
public final class BlazeRingHandler {

    /** 燃焼時間（秒）。 */
    private static final int FIRE_SECONDS = 5;

    /** 追加ダメージ。 */
    private static final float BONUS_DAMAGE = 3.0F;

    private BlazeRingHandler() {
    }

    @SubscribeEvent
    public static void onAttack(AttackEntityEvent event) {

        Player player = event.getEntity();
        if (player.level().isClientSide) return;

        if (!(event.getTarget() instanceof LivingEntity target)) return;

boolean hasRing = CuriosApi.getCuriosInventory(player)
        .map(handler -> handler.findFirstCurio(
                stack -> stack.is(Items.BLAZE_RING.get())
                        || stack.is(Items.FLAME_RING.get())
        ).isPresent())
        .orElse(false);

        if (!hasRing) return;

        // 燃やす
        target.setSecondsOnFire(FIRE_SECONDS);

        // 追加ダメージ
        target.invulnerableTime = 0;
        target.hurt(
                target.damageSources().playerAttack(player),
                BONUS_DAMAGE
        );
    }
}