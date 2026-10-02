package com.tyami.forlaism.event;

import com.tyami.forlaism.registry.Items;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;

import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import top.theillusivec4.curios.api.CuriosApi;

import java.util.ArrayList;
import java.util.List;

/**
 * アイスリングの凍結効果。
 *
 * プレイヤーの足元の水面（水源 + 流水）を氷に変える。
 * フロストウォーカーIV相当の範囲（4ブロック四方）をカバー。
 *
 * 特徴:
 *   - 水源と流水の両方を凍らせる
 *   - 一度凍らせた場所は記録して、プレイヤーが離れたら溶かす
 *   - ネザー・エンドでは動かない（バニラ準拠）
 */
@Mod.EventBusSubscriber(modid = "forlaism")
public final class IceRingHandler {

    /** 凍結範囲（フロストウォーカーIV相当: 4ブロック四方）。 */
    private static final int FREEZE_RADIUS = 2;

    /** サーバーtickの間隔（2tickごとに処理）。 */
    private static final int TICK_INTERVAL = 2;

    /** プレイヤーが作った氷を記録する（離れたら溶かす用）。 */
    private static final java.util.Map<java.util.UUID, List<BlockPos>> PLACED_ICE =
            new java.util.HashMap<>();

    private IceRingHandler() {
    }

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {

        if (event.phase != TickEvent.Phase.END) return;

        Player player = event.player;
        if (player.level().isClientSide) return;
        if (!(player.level() instanceof ServerLevel sl)) return;

        // 2tickごとに処理（軽量化）
        if (sl.getGameTime() % TICK_INTERVAL != 0) return;

        // ネザー・エンドでは無効（バニラのフロストウォーカーと同じ）
        if (sl.dimension() == Level.NETHER || sl.dimension() == Level.END) {
            return;
        }

        boolean hasRing = CuriosApi.getCuriosInventory(player)
                .map(handler -> handler.findFirstCurio(
                        stack -> stack.is(Items.ICE_RING.get())
                ).isPresent())
                .orElse(false);

        if (!hasRing) {
            // リングを外したら、置いた氷を溶かす
            meltAll(sl, player.getUUID());
            return;
        }

        // 足元の水を凍らせる
        freezeAround(sl, player);

        // 離れた場所の氷を溶かす
        meltDistant(sl, player);
    }

    /**
     * プレイヤーの足元周辺の水を氷に変える。
     */
    private static void freezeAround(ServerLevel level, Player player) {

        // 足元のブロック位置
        BlockPos feet = player.blockPosition();

        // 歩く方向に合わせて範囲を計算
        // （進行方向 + 左右に広め）
        for (int dx = -FREEZE_RADIUS; dx <= FREEZE_RADIUS; dx++) {
            for (int dz = -FREEZE_RADIUS; dz <= FREEZE_RADIUS; dz++) {

                BlockPos checkPos = feet.offset(dx, 0, dz);
                BlockPos belowPos = checkPos.below();

                // =====================================================
                // 1. 足元が水（プレイヤーが水に浸かっている）
                // =====================================================
                FluidState feetFluid = level.getFluidState(checkPos);
                if (isWater(feetFluid) && feetFluid.isSource()) {
                    freeze(level, checkPos, player);
                }

                // =====================================================
                // 2. 足元の1つ下が水（歩いてる地面が水）
                // =====================================================
                FluidState belowFluid = level.getFluidState(belowPos);
                if (isWater(belowFluid)) {
                    freeze(level, belowPos, player);
                }

                // =====================================================
                // 3. 水面が「着地可能な高さ」にある場合
                // =====================================================
                // プレイヤーが水の上を歩けるように、足元の水を凍らせる
                // （水ブロックの上に立っている場合）
            }
        }
    }

    /**
     * 指定位置の水を氷に変える。
     */
    private static void freeze(ServerLevel level, BlockPos pos, Player player) {

        BlockState state = level.getBlockState(pos);
        FluidState fluid = level.getFluidState(pos);

        if (!isWater(fluid)) return;

        // 水ブロックを氷に置換
        level.setBlock(pos, Blocks.FROSTED_ICE.defaultBlockState(), 3);

        // 設置場所を記録
        PLACED_ICE.computeIfAbsent(player.getUUID(), k -> new ArrayList<>())
                .add(pos.immutable());
    }

    /**
     * 水かどうか判定（水源 + 流水）。
     */
    private static boolean isWater(FluidState fluid) {
        return fluid.getType() == Fluids.WATER
                || fluid.getType() == Fluids.FLOWING_WATER;
    }

    /**
     * プレイヤーが離れた場所の氷を溶かす。
     */
    private static void meltDistant(ServerLevel level, Player player) {

        List<BlockPos> placed = PLACED_ICE.get(player.getUUID());
        if (placed == null || placed.isEmpty()) return;

        BlockPos playerPos = player.blockPosition();

        placed.removeIf(pos -> {
            // プレイヤーから8ブロック以上離れたら溶かす
            if (pos.distSqr(playerPos) > 8 * 8) {
                // 氷なら水に戻す
                if (level.getBlockState(pos).is(Blocks.FROSTED_ICE)) {
                    level.setBlock(pos, Blocks.WATER.defaultBlockState(), 3);
                }
                return true;
            }
            return false;
        });
    }

    /**
     * 全記録を溶かす（リングを外した時など）。
     */
    private static void meltAll(ServerLevel level, java.util.UUID uuid) {

        List<BlockPos> placed = PLACED_ICE.remove(uuid);
        if (placed == null) return;

        for (BlockPos pos : placed) {
            if (level.getBlockState(pos).is(Blocks.FROSTED_ICE)) {
                level.setBlock(pos, Blocks.WATER.defaultBlockState(), 3);
            }
        }
    }
}