package com.tyami.forlaism.world;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * 輪廻再転式量子融合炉（Rinne Revolving Quantum Fusion Reactor）のマルチブロック管理。
 *
 * 15 × 8 × 15 の巨大構造。
 * 中央の縦穴（Y1〜Y6）にアイテムを投入し、外側の搬入機に FE を流すと加工が走る。
 *
 * 起動: 搬入機にリンネディウムインゴットを持って Shift + 右クリック
 * 停止: 構造破壊時に自動解除
 */
public final class ForlaismReactorMultiblockManager {

    /** 起動済みマルチブロックの基準座標（= 構造の最小コーナー origin）を保持。 */
    private static final Set<BlockPos> ACTIVE =
            Collections.synchronizedSet(new HashSet<>());

    /** 構造のサイズ。 */
    public static final int SIZE_X = 15;
    public static final int SIZE_Y = 8;
    public static final int SIZE_Z = 15;

    // =========================================================
    // レシピ定義
    // =========================================================

    /** 1回の加工に必要な FE。 */
    public static final int FE_PER_CRAFT = 100_000;

    /** 入力アイテム。 */
    public static final net.minecraft.world.item.Item RECIPE_INPUT =
            net.minecraft.world.item.Items.DIRT;

    /** 入力個数。 */
    public static final int RECIPE_INPUT_COUNT = 100;

    /** 出力アイテム。 */
    public static final net.minecraft.world.item.Item RECIPE_OUTPUT =
            net.minecraft.world.item.Items.NETHER_STAR;

    /** 出力個数。 */
    public static final int RECIPE_OUTPUT_COUNT = 1;

    private ForlaismReactorMultiblockManager() {
    }




    // =========================================================
    // LAYERS
    // =========================================================

    public static final String[][] LAYERS = {
        // Y0
        {
            "     SSISS     ",
            "   CSGGMGGSC   ",
            "  CCSGGMGGSCC  ",
            " CCMGGGMGGGMCC ",
            " SSGMGGMGGMGSS ",
            "SGGGGMMKMMGGGGS",
            "SGGGGMEMEMGGGGS",
            "SMMMMKMOMKMMMMS",
            "SGGGGMEMEMGGGGS",
            "SGGGGMMKMMGGGGS",
            " SSGMGGMGGMGSS ",
            " CCMGGGMGGGMCC ",
            "  CCSGGMGGSCC  ",
            "   CSGGMGGSC   ",
            "     SSSSS     ",
        },
        // Y1
        {
            "               ",
            "               ",
            "               ",
            "               ",
            "               ",
            "     C   C     ",
            "      GGG      ",
            "      G G      ",
            "      GGG      ",
            "     C   C     ",
            "               ",
            "               ",
            "               ",
            "               ",
            "               ",
        },
        // Y2
        {
            "               ",
            "               ",
            "               ",
            "               ",
            "               ",
            "     C   C     ",
            "      GGG      ",
            "      G G      ",
            "      GGG      ",
            "     C   C     ",
            "               ",
            "               ",
            "               ",
            "               ",
            "               ",
        },
        // Y3
        {
            "               ",
            "               ",
            "               ",
            "               ",
            "               ",
            "     C   C     ",
            "      GGG      ",
            "      G G      ",
            "      GGG      ",
            "     C   C     ",
            "               ",
            "               ",
            "               ",
            "               ",
            "               ",
        },
        // Y4
        {
            "               ",
            "               ",
            "               ",
            "               ",
            "               ",
            "     C   C     ",
            "      GGG      ",
            "      G G      ",
            "      GGG      ",
            "     C   C     ",
            "               ",
            "               ",
            "               ",
            "               ",
            "               ",
        },
        // Y5
        {
            "               ",
            "               ",
            "               ",
            "               ",
            "               ",
            "     C   C     ",
            "      GGG      ",
            "      G G      ",
            "      GGG      ",
            "     C   C     ",
            "               ",
            "               ",
            "               ",
            "               ",
            "               ",
        },
        // Y6
        {
            "               ",
            "               ",
            "               ",
            "               ",
            "               ",
            "     C   C     ",
            "      GGG      ",
            "      G G      ",
            "      GGG      ",
            "     C   C     ",
            "               ",
            "               ",
            "               ",
            "               ",
            "               ",
        },
        // Y7
        {
            "               ",
            "               ",
            "               ",
            "               ",
            "               ",
            "     CCCCC     ",
            "     CTTTC     ",
            "     CT TC     ",
            "     CTTTC     ",
            "     CCCCC     ",
            "               ",
            "               ",
            "               ",
            "               ",
            "               ",
        },
    };

    // =========================================================
    // 構造チェック
    // =========================================================

    /**
     * origin = 構造の基準座標。LAYERS の (0, 0, 0) に対応。
     */
    public static boolean validateStructure(Level level, BlockPos origin) {

        for (int y = 0; y < LAYERS.length; y++) {
            String[] layer = LAYERS[y];
            for (int z = 0; z < SIZE_Z; z++) {
                String row = layer[z];
                for (int x = 0; x < SIZE_X; x++) {
                    char c = row.charAt(x);
                    BlockPos p = origin.offset(x, y, z);
                    if (!matches(level.getBlockState(p), c)) {
                        return false;
                    }
                }
            }
        }

        return true;
    }

    private static boolean matches(BlockState state, char c) {
        return switch (c) {
            case 'S' -> state.is(Blocks.SMOOTH_STONE);
            case 'M' -> state.is(com.tyami.forlaism.registry.Blocks.MADOROMU_BLOCK.get());
            case 'K' -> state.is(com.tyami.forlaism.registry.Blocks.FORLAISM_CRYSTAL_BLOCK.get());
            case 'T' -> state.is(com.tyami.forlaism.registry.Blocks.STEEL_BLOCK.get());
            case 'I' -> state.is(com.tyami.forlaism.registry.Blocks.FORLAISM_INSERTER.get());
            case ' ' -> state.isAir();
            case 'C' -> isBlockId(state, "mekanismgenerators:fission_reactor_casing");
            case 'G' -> isBlockId(state, "mekanism:structural_glass");
            case 'E' -> isBlockId(state, "mekanism:ultimate_energy_cube");
            case 'O' -> isBlockId(state, "mekanism:sps_port");
            default -> false;
        };
    }

    private static boolean isBlockId(BlockState state, String id) {
        ResourceLocation key = ForgeRegistries.BLOCKS.getKey(state.getBlock());
        return key != null && key.toString().equals(id);
    }

    // =========================================================
    // 内部座標
    // =========================================================

    /**
     * 中央空洞の入口（Y1 の中央マス）。
     * Y1〜Y6 の縦穴の一番下。
     */
    public static BlockPos getCenterHole(BlockPos origin) {
        return origin.offset(7, 1, 7);
    }

    /**
     * 中央空洞の AABB（アイテム取得用）。Y1〜Y6 を覆う。
     */
    public static AABB getCenterAABB(BlockPos origin) {
        BlockPos c = getCenterHole(origin);
        return new AABB(
                c.getX() + 0.1, c.getY() - 0.5, c.getZ() + 0.1,
                c.getX() + 0.9, c.getY() + 6.5, c.getZ() + 0.9
        );
    }

    /**
     * 搬入機（I）の位置。LAYERS[0] の row0, col7。
     */
    public static BlockPos getInserterPos(BlockPos origin) {
        return origin.offset(7, 0, 0);
    }

    /**
     * 搬入機の位置から構造の origin を逆算。
     */
    public static BlockPos originFromInserter(BlockPos inserterPos) {
        return inserterPos.offset(-7, 0, 0);
    }

    // =========================================================
    // 登録・解除
    // =========================================================

    public static void register(BlockPos origin) {
        ACTIVE.add(origin.immutable());
    }

    public static void unregister(BlockPos origin) {
        ACTIVE.remove(origin);
    }

    public static boolean isActive(BlockPos origin) {
        return ACTIVE.contains(origin);
    }

    public static Set<BlockPos> getActive() {
        return ACTIVE;
    }

    public static void clearAll() {
        ACTIVE.clear();
    }
}