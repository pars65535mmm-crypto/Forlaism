package com.tyami.forlaism.event;

import com.tyami.forlaism.registry.Items;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import top.theillusivec4.curios.api.CuriosApi;

/**
 * ジャンプリングの効果。
 *
 * - ジャンプした瞬間、向いている方向へ追加加速する。
 * - ダッシュの有無は関係なし。
 *
 * 検知は LivingEvent.LivingJumpEvent を使う。
 * （自前の onGround 遷移監視より正確で、ブロック密着の誤検知もない）
 */
@Mod.EventBusSubscriber(modid = "forlaism")
public final class JumpRingHandler {

    /** 追加加速の強さ（水平方向）。 */
    private static final double JUMP_BOOST = 0.55D;

    /** 追加の上方向成分。 */
    private static final double JUMP_BOOST_UP = 0.05D;

    private JumpRingHandler() {
    }

    @SubscribeEvent
    public static void onLivingJump(LivingEvent.LivingJumpEvent event) {

        // プレイヤーのみ対象
        if (!(event.getEntity() instanceof Player player)) {
            return;
        }

        // サーバー側でのみ処理
        if (player.level().isClientSide) {
            return;
        }

        // リング装備チェック
boolean hasRing = CuriosApi.getCuriosInventory(player)
        .map(handler -> handler.findFirstCurio(
                stack -> stack.is(Items.JUMP_RING.get())
                        || stack.is(Items.ROCKET_RING.get())
        ).isPresent())
        .orElse(false);

        if (!hasRing) {
            return;
        }

        // =========================================================
        // 向いている方向へ押し出し
        // =========================================================
        Vec3 look = player.getLookAngle();
        Vec3 v = player.getDeltaMovement();

        player.setDeltaMovement(
                v.x + look.x * JUMP_BOOST,
                v.y + JUMP_BOOST_UP,
                v.z + look.z * JUMP_BOOST
        );
        player.hurtMarked = true;

        // 軽めの演出
        if (player.level() instanceof ServerLevel sl) {
            sl.sendParticles(
                    ParticleTypes.END_ROD,
                    player.getX(), player.getY() + 0.1, player.getZ(),
                    6,
                    0.2, 0.05, 0.2,
                    0.03
            );
        }
    }
}