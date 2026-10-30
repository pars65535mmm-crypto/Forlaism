package com.tyami.forlaism.event;

import com.tyami.forlaism.world.FakedreamDimension;

import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * FakeDream の塔の外側で Y=500 より上に行けないようにする。
 *
 * 挙動:
 *   - 塔の領域（±11）の「外」にいる
 *   - かつ Y > 500
 *   - かつ上昇中（Y 速度 > 0）
 *   → 上昇値を反転（Y 速度を -|v.y| に）
 *
 * これにより:
 *   - エリトラで外から来た人は、上昇が急に下向きになって落ちる
 *   - ブロック積んで登ってる人は、ジャンプの上昇分が反転して登れない
 *   - 塔の中（螺旋階段含む）を登ってる人は、この判定を受けない
 */
@Mod.EventBusSubscriber(modid = "forlaism")
public final class TowerGuardHandler {

    /** 塔の中心座標。 */
    private static final double TOWER_X = 1300.0;
    private static final double TOWER_Z = 230.0;

    /** 塔の領域半径（±11）。 */
    private static final double TOWER_RADIUS = 11.0;

    /** 押し返しを開始する Y 座標。 */
    private static final double GUARD_Y = 500.0;

    /** メッセージを出す間隔（tick）。 */
    private static final int MESSAGE_INTERVAL = 40;

    private TowerGuardHandler() {
    }

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;

        Player player = event.player;
        if (player.level().isClientSide) return;

        // FakeDream 以外は無視
        if (!player.level().dimension().equals(FakedreamDimension.FAKEDREAM_LEVEL)) {
            return;
        }

        // Y=500 未満なら何もしない
        if (player.getY() < GUARD_Y) {
            return;
        }

        // 塔の領域内なら何もしない（登ってる人は自由）
        double dx = Math.abs(player.getX() - TOWER_X);
        double dz = Math.abs(player.getZ() - TOWER_Z);
        if (dx <= TOWER_RADIUS && dz <= TOWER_RADIUS) {
            return;
        }

        // ここから「領域外 かつ Y > 500」

        Vec3 v = player.getDeltaMovement();

        // 上昇中のみ反転
        if (v.y > 0.0D) {
            player.setDeltaMovement(v.x, -v.y, v.z);
            player.hurtMarked = true;
        }

        // 念のため：何らかの手段で上に行こうとしている場合、
        // Y を 500 付近で頭打ちにする「ソフトキャップ」
        if (player.getY() > GUARD_Y + 5.0D) {
            player.teleportTo(
                    player.getX(),
                    GUARD_Y + 5.0D,
                    player.getZ()
            );
        }

        // メッセージ（一定間隔で）
        if (player.tickCount % MESSAGE_INTERVAL == 0) {
            player.displayClientMessage(
                    Component.literal("§7ここから先は、塔を登って進め。")
                            .withStyle(net.minecraft.ChatFormatting.GRAY),
                    true
            );
        }
    }
}