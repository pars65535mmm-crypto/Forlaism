package com.tyami.forlaism.event;

import com.tyami.forlaism.registry.Items;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.entity.BarrelBlockEntity;
import net.minecraft.world.level.block.entity.ShulkerBoxBlockEntity;
import net.minecraft.world.level.block.entity.BaseContainerBlockEntity;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import top.theillusivec4.curios.api.CuriosApi;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * トレジャーリングの探知処理。
 *
 * プレイヤー周辺のロード済みチャンクを走査し、
 * 最寄りのコンテナ（チェスト・樽・シュルカーボックス等）の
 * 方向と距離をアクションバーに表示する。
 *
 * 表示形式:
 *   チェスト: 右前 (32m)
 */
@Mod.EventBusSubscriber(modid = "forlaism")
public final class TreasureRingHandler {

    /** 探知範囲（ブロック）。 */
    private static final int SEARCH_RADIUS = 64;

    /** 表示更新間隔（tick）。 */
    private static final int UPDATE_INTERVAL = 10;

    /** プレイヤーごとの最終表示tick。 */
    private static final Map<UUID, Long> LAST_UPDATE = new HashMap<>();

    private TreasureRingHandler() {
    }

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {

        if (event.phase != TickEvent.Phase.END) return;

        Player player = event.player;
        if (player.level().isClientSide) return;
        if (!(player instanceof ServerPlayer sp)) return;
        if (!(player.level() instanceof ServerLevel sl)) return;

        // 更新間隔チェック
        long now = sl.getGameTime();
        Long last = LAST_UPDATE.get(player.getUUID());
        if (last != null && now - last < UPDATE_INTERVAL) return;
        LAST_UPDATE.put(player.getUUID(), now);

        // リング装備チェック
        boolean hasRing = CuriosApi.getCuriosInventory(player)
                .map(handler -> handler.findFirstCurio(
                        stack -> stack.is(Items.TREASURE_RING.get())
                ).isPresent())
                .orElse(false);

        if (!hasRing) return;

        // 最寄りのコンテナを探す
        BlockPos nearest = findNearestContainer(sl, player.blockPosition());

        if (nearest == null) {
            // 見つからなかったら何も表示しない（前の表示を消す）
            player.displayClientMessage(Component.literal(""), true);
            return;
        }

        // 方向と距離を計算
        Vec3 diff = Vec3.atCenterOf(nearest).subtract(player.position());
        double distance = diff.length();
        String direction = getDirectionName(player, diff);

        // ブロック名を取得
        BlockState state = sl.getBlockState(nearest);
        String blockName = state.getBlock().getName().getString();

        // アクションバーに表示
        player.displayClientMessage(
                Component.literal("§6🧭 §f" + blockName + ": §e" + direction
                        + " §7(" + (int) distance + "m)"),
                true
        );
    }

    /**
     * 最寄りのコンテナを探す。
     *
     * プレイヤー周辺のロード済みチャンクを走査。
     */
    private static BlockPos findNearestContainer(ServerLevel level, BlockPos center) {

        BlockPos nearest = null;
        double nearestDistSqr = Double.MAX_VALUE;

        int chunkRadius = SEARCH_RADIUS / 16 + 1;
        int centerChunkX = center.getX() >> 4;
        int centerChunkZ = center.getZ() >> 4;

        for (int cx = centerChunkX - chunkRadius; cx <= centerChunkX + chunkRadius; cx++) {
            for (int cz = centerChunkZ - chunkRadius; cz <= centerChunkZ + chunkRadius; cz++) {

                // チャンクがロードされてなければスキップ
                if (!level.getChunkSource().hasChunk(cx, cz)) continue;

                LevelChunk chunk = level.getChunk(cx, cz);

                for (BlockEntity be : chunk.getBlockEntities().values()) {
                    if (!isContainer(be)) continue;

                    BlockPos pos = be.getBlockPos();
                    double distSqr = pos.distSqr(center);

                    if (distSqr > (double) SEARCH_RADIUS * SEARCH_RADIUS) continue;
                    if (distSqr >= nearestDistSqr) continue;

                    nearestDistSqr = distSqr;
                    nearest = pos;
                }
            }
        }

        return nearest;
    }

    /**
     * コンテナかどうか判定。
     */
    private static boolean isContainer(BlockEntity be) {
        // チェスト・トラップチェスト
        if (be instanceof ChestBlockEntity) return true;
        // 樽
        if (be instanceof BarrelBlockEntity) return true;
        // シュルカーボックス
        if (be instanceof ShulkerBoxBlockEntity) return true;
        // その他のコンテナ系（ホッパー、ディスペンサー等は除外したい場合は個別判定）
        // BaseContainerBlockEntity を継承してるものは一応対象
        if (be instanceof BaseContainerBlockEntity) return true;

        return false;
    }

    /**
     * プレイヤーから見た方向名を返す。
     *
     * 前・右前・右・右後・後・左後・左・左前 の8方向。
     */
    private static String getDirectionName(Player player, Vec3 diff) {

        // プレイヤーの視線（水平成分のみ）
        float yaw = player.getYRot();
        double yawRad = Math.toRadians(yaw);

        // 前方向ベクトル (x, z)
        double forwardX = -Math.sin(yawRad);
        double forwardZ = Math.cos(yawRad);

        // 右方向ベクトル (x, z)
        double rightX = Math.cos(yawRad);
        double rightZ = Math.sin(yawRad);

        // 対象の水平方向ベクトル
        double targetX = diff.x;
        double targetZ = diff.z;

        // 前方向・右方向への射影
        double fwd = forwardX * targetX + forwardZ * targetZ;
        double rgt = rightX * targetX + rightZ * targetZ;

        // 8方向に分割
        double angle = Math.toDegrees(Math.atan2(rgt, fwd));

        // -180 ～ 180 を -22.5 ～ 337.5 に正規化
        if (angle < -22.5) angle += 360;

        if (angle >= -22.5 && angle < 22.5) return "前";
        if (angle >= 22.5 && angle < 67.5) return "右前";
        if (angle >= 67.5 && angle < 112.5) return "右";
        if (angle >= 112.5 && angle < 157.5) return "右後";
        if (angle >= 157.5 && angle < 202.5) return "後";
        if (angle >= 202.5 && angle < 247.5) return "左後";
        if (angle >= 247.5 && angle < 292.5) return "左";
        if (angle >= 292.5 && angle < 337.5) return "左前";

        return "前";
    }
}