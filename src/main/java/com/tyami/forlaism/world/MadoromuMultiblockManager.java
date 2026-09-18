package com.tyami.forlaism.world;

import com.tyami.forlaism.registry.Blocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * 微睡む九十九の夢マルチブロックの管理。
 *
 * 構造:
 *   F = フォラリス搬入機（基準ブロック）
 *   M = 微睡む九十九の塊
 *
 *   Fの内向き法線 n を基準に、相対座標で構造を判定する。
 */
public final class MadoromuMultiblockManager {

    /** ワールドごとにアクティブなマルチブロックの基準座標を保持。 */
    private static final Set<BlockPos> ACTIVE = Collections.synchronizedSet(new HashSet<>());

    private MadoromuMultiblockManager() {
    }

    // =========================================================
    // 構造チェック
    // =========================================================

    /**
     * Fの位置から構造をチェックする。
     *
     * @param level ワールド
     * @param inserterPos Fの位置
     * @param n Fが向いている内向き法線（壁の内側方向）
     * @return 構造が成立していれば true
     */
    public static boolean validateStructure(Level level, BlockPos inserterPos, Direction n) {
        // F から見た「前方向（内向き）」= n
        // 「右方向」= n を Y軸回りに -90°回転
        Direction right = n.getClockWise();
        Direction up = Direction.UP;

        // =====================================================
        // Y1層 (Fの1つ下、5x5 全部 M)
        // =====================================================
        BlockPos y1Origin = inserterPos.relative(up, -1);
        if (!checkLayer(level, y1Origin, n, right, true)) return false;

        // =====================================================
        // Y2層 (Fと同じ高さ、外周のみ M、F以外の内部は空気)
        // =====================================================
        if (!checkLayer(level, inserterPos, n, right, false)) return false;

        // =====================================================
        // Y3〜Y10層 (Fの1つ上から8層、外周のみ M)
        // =====================================================
        for (int dy = 1; dy <= 8; dy++) {
            BlockPos layerPos = inserterPos.relative(up, dy);
            if (!checkLayer(level, layerPos, n, right, false)) return false;
        }

        return true;
    }

    /**
     * 1層分のチェック。
     *
     * @param center Fの位置（= 外周の1マス）を中心とする基準位置
     * @param n 内向き法線
     * @param right 右方向
     * @param solidAll true なら5x5 全部 M、false なら外周のみ M & 内部空気
     */
    private static boolean checkLayer(Level level, BlockPos center, Direction n, Direction right, boolean solidAll) {

        // 5x5 の中心は、F から見て「n方向に2マス進んだ位置」
        // Fは外周の中央なので、中心までは n方向に 2マス
        BlockPos layerCenter = center.relative(n, 2);

        for (int dx = -2; dx <= 2; dx++) {
            for (int dz = -2; dz <= 2; dz++) {

                BlockPos target = layerCenter
                        .relative(right, dx)
                        .relative(n, -dz); // dz は n の逆方向に展開（z=0が手前=F側）

                // 外周かどうか
                boolean isEdge = (Math.abs(dx) == 2 || Math.abs(dz) == 2);

                if (solidAll) {
                    // Y1層: 全部 M
                    if (!isMadoromuBlock(level, target)) return false;
                } else {
                    if (isEdge) {
                        // 外周: F または M
                        if (target.equals(center)) {
                            // F 自身の位置 → スキップ（後でFチェック）
                            continue;
                        }
                        if (!isMadoromuBlock(level, target)) return false;
                    } else {
                        // 内部: 空気
                        if (!level.getBlockState(target).isAir()) return false;
                    }
                }
            }
        }

        return true;
    }

    private static boolean isMadoromuBlock(Level level, BlockPos pos) {
        return level.getBlockState(pos).is(Blocks.MADOROMU_BLOCK.get());
    }

    private static boolean isInserter(Level level, BlockPos pos) {
        return level.getBlockState(pos).is(Blocks.FORLAISM_INSERTER.get());
    }

    // =========================================================
    // Fの位置から内向き法線を推定
    // =========================================================

    /**
     * Fの位置から、そのFが「どの壁面に埋まっているか」を判定して
     * 内向き法線を返す。
     *
     * Fの6面のうち、内部方向（空気側）を法線とする。
     */
public static Direction detectInwardNormal(Level level, BlockPos inserterPos) {

    Direction[] horizontals = {
            Direction.NORTH, Direction.SOUTH, Direction.EAST, Direction.WEST
    };

    for (Direction d : horizontals) {
        // 1マス先が空気
        if (!level.getBlockState(inserterPos.relative(d)).isAir()) continue;

        // 2マス先も空気 → これが内側（Y2空洞）
        // 2マス先がM → これが外側（外周壁の向こう）
        if (level.getBlockState(inserterPos.relative(d, 2)).isAir()) {
            // さらに、2マス先の「真上」がM（Y3層の外周）なら確実に内側
            BlockPos twoAhead = inserterPos.relative(d, 2);
            if (level.getBlockState(twoAhead.above()).is(Blocks.MADOROMU_BLOCK.get())) {
                return d;
            }
        }
    }

    // フォールバック：2マス先が空気な最初の方向
    for (Direction d : horizontals) {
        if (level.getBlockState(inserterPos.relative(d)).isAir()
                && level.getBlockState(inserterPos.relative(d, 2)).isAir()) {
            return d;
        }
    }

    return null;
}

    // =========================================================
    // 登録・解除
    // =========================================================

    public static void register(BlockPos inserterPos) {
        ACTIVE.add(inserterPos.immutable());
    }

    public static void unregister(BlockPos inserterPos) {
        ACTIVE.remove(inserterPos);
    }

    public static boolean isActive(BlockPos inserterPos) {
        return ACTIVE.contains(inserterPos);
    }

    public static Set<BlockPos> getActive() {
        return ACTIVE;
    }

    /** サーバー停止時などに全解除。 */
    public static void clearAll() {
        ACTIVE.clear();
    }



// MadoromuMultiblockManager.java に追加

/**
 * Fの位置と内向き法線から、内部空洞の中心座標を返す。
 *
 * Y2層の3×3空洞の中心 = Fから内向き2マス、Y+1
 */
public static BlockPos getInnerCenter(BlockPos inserterPos, Direction inward) {
    return inserterPos.relative(inward, 2).above(1);
}

/**
 * アイテム判定範囲（AABB）を返す。
 *
 * Y2層の3×3内部をカバー。
 */
public static net.minecraft.world.phys.AABB getInnerAABB(BlockPos inserterPos, Direction inward) {
    BlockPos center = getInnerCenter(inserterPos, inward);

    // 3×3×3 の判定エリア（少し余裕を持たせる）
    double r = 1.5;
    return new net.minecraft.world.phys.AABB(
            center.getX() + 0.5 - r, center.getY() - 0.5, center.getZ() + 0.5 - r,
            center.getX() + 0.5 + r, center.getY() + 2.0, center.getZ() + 0.5 + r
    );
}
}