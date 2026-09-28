package com.tyami.forlaism.event;

import com.tyami.forlaism.registry.Items;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import top.theillusivec4.curios.api.CuriosApi;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * ロケットリングの空中歩行効果。
 *
 * 空中でShiftを押している間、0.4秒 (8tick) だけ
 * 「空中でも onGround 相当の挙動」を維持する。
 *
 * 実装:
 *   - 空中でShift押した瞬間に残tickを8にセット
 *   - 残tick中は:
 *       ・落下を打ち消す（y速度を0付近に）
 *       ・地上と同じ摩擦/加速が効くようにする
 *   - 残tickが0になったら通常の落下に戻る
 */
@Mod.EventBusSubscriber(modid = "forlaism")
public final class RocketRingHandler {

    /** 空中歩行の持続tick。0.4秒 = 8tick。 */
    private static final int AIR_WALK_TICKS = 8;

    /** 空中歩行中の水平方向の減衰係数（1tickあたり）。 */
    private static final double AIR_WALK_FRICTION = 0.91D;

    /** 空中歩行中の推進力（歩行入力があった時に加える）。 */
    private static final double AIR_WALK_ACCEL = 0.10D;

    /** プレイヤーごとの残り空中歩行tick。 */
    private static final Map<UUID, Integer> AIR_WALK = new HashMap<>();

    private RocketRingHandler() {
    }

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;

        Player player = event.player;
        if (player.level().isClientSide) return;

        UUID uuid = player.getUUID();
        int remaining = AIR_WALK.getOrDefault(uuid, 0);

        // =========================================================
        // リング装備チェック
        // =========================================================
        boolean hasRing = CuriosApi.getCuriosInventory(player)
                .map(handler -> handler.findFirstCurio(
                        stack -> stack.is(Items.ROCKET_RING.get())
                ).isPresent())
                .orElse(false);

        if (!hasRing) {
            AIR_WALK.remove(uuid);
            return;
        }

        // =========================================================
        // 新規発動判定
        //   空中 && Shift押し下げ && 残tickなし
        // =========================================================
        boolean inAir = !player.onGround()
                && !player.isFallFlying()
                && !player.isPassenger()
                && !player.getAbilities().flying;

        if (inAir && player.isShiftKeyDown() && remaining <= 0) {
            remaining = AIR_WALK_TICKS;

            // 発動演出
            if (player.level() instanceof ServerLevel sl) {
                sl.sendParticles(
                        ParticleTypes.CLOUD,
                        player.getX(), player.getY(), player.getZ(),
                        12,
                        0.4, 0.1, 0.4,
                        0.05
                );
                sl.sendParticles(
                        ParticleTypes.END_ROD,
                        player.getX(), player.getY(), player.getZ(),
                        4,
                        0.3, 0.1, 0.3,
                        0.02
                );
            }
        }

        // =========================================================
        // 空中歩行中の処理
        // =========================================================
        if (remaining > 0) {
            remaining--;

            Vec3 v = player.getDeltaMovement();

            // 落下をほぼ打ち消す
            double newY = Math.max(v.y, -0.05D);
            if (v.y < 0.0D) {
                newY = v.y * 0.3D;
            }

            // 水平方向の減衰（少しだけ残す）
            double newX = v.x * AIR_WALK_FRICTION;
            double newZ = v.z * AIR_WALK_FRICTION;

            // 移動入力があったら向いている方向へ推進
            Vec3 input = getInputDirection(player);
            if (input.lengthSqr() > 0.0D) {
                newX += input.x * AIR_WALK_ACCEL;
                newZ += input.z * AIR_WALK_ACCEL;
            }

            player.setDeltaMovement(newX, newY, newZ);
            player.hurtMarked = true;

            // 落下ダメージ無効化
            player.fallDistance = 0.0F;

            // 歩行中のパーティクル
            if (player.level() instanceof ServerLevel sl
                    && remaining % 2 == 0) {
                sl.sendParticles(
                        ParticleTypes.END_ROD,
                        player.getX(), player.getY() + 0.05, player.getZ(),
                        1,
                        0.15, 0.0, 0.15,
                        0.01
                );
            }

            if (remaining <= 0) {
                AIR_WALK.remove(uuid);
            } else {
                AIR_WALK.put(uuid, remaining);
            }
        }
    }

    /**
     * プレイヤーの移動入力を水平方向ベクトルで取得する。
     */
    private static Vec3 getInputDirection(Player player) {
        float forward = player.zza;
        float strafe = player.xxa;

        if (forward == 0.0F && strafe == 0.0F) {
            return Vec3.ZERO;
        }

        float yaw = player.getYRot();
        double rad = Math.toRadians(yaw);

        // Minecraftの前方向: (-sin(yaw), 0, cos(yaw))
        double fx = -Math.sin(rad);
        double fz = Math.cos(rad);

        // 右方向: (cos(yaw), 0, sin(yaw))
        double rx = Math.cos(rad);
        double rz = Math.sin(rad);

        double x = fx * forward + rx * strafe;
        double z = fz * forward + rz * strafe;

        double len = Math.sqrt(x * x + z * z);
        if (len < 1e-6) return Vec3.ZERO;

        return new Vec3(x / len, 0.0D, z / len);
    }
}