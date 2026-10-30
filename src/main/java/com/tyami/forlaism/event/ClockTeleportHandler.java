package com.tyami.forlaism.event;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/**
 * TP処理。
 *
 * 埋まってたらY=300から真下にレイキャストして地表へ。
 * 埋まってなければそのままTP。
 */
public final class ClockTeleportHandler {

    private ClockTeleportHandler() {
    }

    public static void teleportTo(ServerPlayer player, double x, double y, double z) {
        ServerLevel sl = player.serverLevel();

        // 埋まってるかチェック（足元と頭の2ブロック分）
        BlockPos feet = BlockPos.containing(x, y, z);
        BlockPos head = feet.above();

        BlockState feetState = sl.getBlockState(feet);
        BlockState headState = sl.getBlockState(head);

        boolean blocked = !feetState.isAir() || !headState.isAir();

        double targetX = x;
        double targetY = y;
        double targetZ = z;

        if (blocked) {
            // Y=300から真下へレイキャスト
            Vec3 start = new Vec3(x, 300, z);
            Vec3 end = new Vec3(x, sl.getMinBuildHeight(), z);

            BlockHitResult hit = sl.clip(new ClipContext(
                    start, end,
                    ClipContext.Block.COLLIDER,
                    ClipContext.Fluid.NONE,
                    player
            ));

            if (hit.getType() == HitResult.Type.BLOCK) {
                BlockPos hitPos = hit.getBlockPos();
                targetY = hitPos.getY() + 1.0;
            } else {
                targetY = 100.0;
            }
        }

        // TP
        player.teleportTo(
                sl,
                targetX, targetY, targetZ,
                player.getYRot(), player.getXRot()
        );

        // 演出
        sl.sendParticles(ParticleTypes.PORTAL,
                player.getX(), player.getY() + 1.0, player.getZ(),
                50, 0.5, 1.0, 0.5, 0.3);
        sl.sendParticles(ParticleTypes.REVERSE_PORTAL,
                targetX, targetY + 1.0, targetZ,
                50, 0.5, 1.0, 0.5, 0.3);
        sl.playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.ENDERMAN_TELEPORT, SoundSource.PLAYERS, 1.0F, 1.2F);

        player.displayClientMessage(
                net.minecraft.network.chat.Component.literal(
                        "§dTP: §f" + (int) targetX + ", " + (int) targetY + ", " + (int) targetZ),
                true
        );
    }

    /**
     * 視線先へTP。
     */
    public static void teleportLookedAt(ServerPlayer player, double range) {
        Vec3 eye = player.getEyePosition();
        Vec3 look = player.getLookAngle().normalize();
        Vec3 end = eye.add(look.scale(range));

        BlockHitResult hit = player.serverLevel().clip(new ClipContext(
                eye, end,
                ClipContext.Block.COLLIDER,
                ClipContext.Fluid.NONE,
                player
        ));

        Vec3 dest = hit.getType() == HitResult.Type.BLOCK
                ? hit.getLocation().add(look.scale(-0.5))
                : end;

        teleportTo(player, dest.x, dest.y, dest.z);
    }
}