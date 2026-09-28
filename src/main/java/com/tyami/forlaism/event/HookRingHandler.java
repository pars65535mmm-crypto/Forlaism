package com.tyami.forlaism.event;

import com.tyami.forlaism.registry.Items;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import top.theillusivec4.curios.api.CuriosApi;

/**
 * フックリング。
 *
 * Shiftを押している間、向いている方向へ3マスレイキャスト。
 * ブロックに当たったらそのブロックへプレイヤーを引き寄せる。
 *
 * 速度は MAX_SPEED でキャップされる。
 */
@Mod.EventBusSubscriber(modid = "forlaism")
public final class HookRingHandler {

    /** 射程（ブロック単位）。 */
    private static final double RANGE = 3.0D;

    /** 引き寄せの強さ（1tickあたりの加速度）。 */
    private static final double PULL_STRENGTH = 0.3D;

    /** 最大速度（水平方向）。バニラのダッシュ ≈ 0.13 / エリトラ ≈ 0.6 くらい。 */
    private static final double MAX_SPEED = 0.43D;

    /** 上方向のブースト（壁に張り付いた時の落下防止）。 */
    private static final double VERTICAL_LIFT = 0.3D;

    /** 到達判定の距離（ブロック中心からの距離）。 */
    private static final double REACH_DISTANCE = 1.2D;

    private HookRingHandler() {
    }

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;

        Player player = event.player;
        if (player.level().isClientSide) return;

        // リング装備チェック
        boolean hasRing = CuriosApi.getCuriosInventory(player)
                .map(handler -> handler.findFirstCurio(
                        stack -> stack.is(Items.HOOK_RING.get())
                ).isPresent())
                .orElse(false);

        if (!hasRing) return;

        // Shift押してないなら何もしない
        if (!player.isShiftKeyDown()) return;

        // レイキャスト
        Vec3 eye = player.getEyePosition();
        Vec3 look = player.getLookAngle();
        Vec3 end = eye.add(look.scale(RANGE));

        BlockHitResult hit = player.level().clip(new ClipContext(
                eye,
                end,
                ClipContext.Block.COLLIDER,
                ClipContext.Fluid.NONE,
                player
        ));

        if (hit.getType() != HitResult.Type.BLOCK) return;

        // 当たったブロック位置
        BlockPos targetPos = hit.getBlockPos();
        Vec3 targetCenter = new Vec3(
                targetPos.getX() + 0.5,
                targetPos.getY() + 0.5,
                targetPos.getZ() + 0.5
        );

        // プレイヤーの現在位置と目標位置の差分
        Vec3 playerPos = new Vec3(player.getX(), player.getY(), player.getZ());
        Vec3 diff = targetCenter.subtract(playerPos);
        double dist = diff.length();

        // 既に到達していたら吸引しない
        if (dist <= REACH_DISTANCE) return;

        // 正規化した方向
        Vec3 dir = diff.normalize();

        // =========================================================
        // 引き寄せ（横方向）
        // =========================================================
        Vec3 v = player.getDeltaMovement();

        double newX = v.x + dir.x * PULL_STRENGTH;
        double newZ = v.z + dir.z * PULL_STRENGTH;
        double newY = v.y + dir.y * PULL_STRENGTH;


                // 上下は「壁に張り付く」感を出すため、わずかに浮かせる
        newY = Math.max(v.y, -0.05D);
        if (dist > 1.5D) {
            newY += VERTICAL_LIFT;
        }

        // =========================================================
        // 最大速度キャップ（水平方向）
        // =========================================================
        double totalSpeed = Math.sqrt(newX * newX + newY * newY + newZ * newZ);
if (totalSpeed > MAX_SPEED) {
    double scale = MAX_SPEED / totalSpeed;
    newX *= scale;
    newY *= scale; // Y軸にもスケールを適用
    newZ *= scale;
}



        player.setDeltaMovement(newX, newY, newZ);
        player.hurtMarked = true;
        player.fallDistance = 0.0F;

        // 演出
        if (player.level() instanceof ServerLevel sl) {
            spawnChainParticles(sl, eye, targetCenter);

            if (player.tickCount % 2 == 0) {
                sl.sendParticles(
                        ParticleTypes.CRIT,
                        targetCenter.x, targetCenter.y, targetCenter.z,
                        2,
                        0.15, 0.15, 0.15,
                        0.0
                );
            }
        }
    }

    private static void spawnChainParticles(ServerLevel sl, Vec3 from, Vec3 to) {
        int steps = 6;
        for (int i = 1; i < steps; i++) {
            double t = (double) i / steps;
            double x = from.x + (to.x - from.x) * t;
            double y = from.y + (to.y - from.y) * t;
            double z = from.z + (to.z - from.z) * t;

            sl.sendParticles(
                    ParticleTypes.END_ROD,
                    x, y, z,
                    1,
                    0.02, 0.02, 0.02,
                    0.0
            );
        }
    }
}