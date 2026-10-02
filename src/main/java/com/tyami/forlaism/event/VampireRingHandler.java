package com.tyami.forlaism.event;

import com.tyami.forlaism.registry.Items;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

import net.minecraftforge.event.entity.player.AttackEntityEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import top.theillusivec4.curios.api.CuriosApi;

/**
 * バンパイアリングの攻撃時回復。
 *
 * 攻撃した時に0.2HP回復する。
 * 最大HPを超えないようにクランプ。
 */
@Mod.EventBusSubscriber(modid = "forlaism")
public final class VampireRingHandler {

    /** 1回の攻撃で回復するHP量。 */
    private static final float HEAL_AMOUNT = 0.2F;

    private VampireRingHandler() {
    }

    @SubscribeEvent
    public static void onAttack(AttackEntityEvent event) {

        Player player = event.getEntity();
        if (player.level().isClientSide) return;

        // 対象がLivingEntity以外なら何もしない
        if (!(event.getTarget() instanceof LivingEntity target)) return;

        // 自分自身への攻撃は除外
        if (target == player) return;

        // リング装備チェック
        boolean hasRing = CuriosApi.getCuriosInventory(player)
                .map(handler -> handler.findFirstCurio(
                        stack -> stack.is(Items.VAMPIRE_RING.get())
                ).isPresent())
                .orElse(false);

        if (!hasRing) return;

        // 満タンなら何もしない
        if (player.getHealth() >= player.getMaxHealth()) return;

        // 回復（最大HPを超えない）
        float newHealth = Math.min(
                player.getMaxHealth(),
                player.getHealth() + HEAL_AMOUNT
        );
        player.setHealth(newHealth);

        // 回復演出
        if (player.level() instanceof ServerLevel sl) {
            sl.sendParticles(
                    ParticleTypes.HEART,
                    player.getX(),
                    player.getY() + 1.5,
                    player.getZ(),
                    1,
                    0.2, 0.2, 0.2,
                    0.0
            );
        }
    }
}