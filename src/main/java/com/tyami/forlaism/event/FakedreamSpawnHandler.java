package com.tyami.forlaism.event;

import com.tyami.forlaism.world.FakedreamDimension;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;

import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.Optional;

/**
 * Fakedreamディメンションの初期化処理。
 *
 * (0, 120, 0) に「普通の桜の大木」を1本だけ生成する。
 *
 * ・バニラのチェリーツリー feature をそのまま使う
 * ・バイオームの features からは削除してあるので、自然生成はされない
 * ・既に生成済みかどうかは「桜の原木があるか」で判定
 */
@Mod.EventBusSubscriber(modid = "forlaism")
public final class FakedreamSpawnHandler {

    /** 大木の根本座標。 */
    public static final int TREE_X = 0;
    public static final int TREE_Y = 120;
    public static final int TREE_Z = 0;

    /** 大木判定のスキャン範囲（Y方向）。 */
    private static final int SCAN_RANGE = 60;

    /** 使う feature（バニラの巨大桜ツリー）。 */
    private static final ResourceLocation TREE_FEATURE_ID =
            new ResourceLocation("minecraft", "trees_cherry");

    private FakedreamSpawnHandler() {
    }

    // =========================================================
    // 入場時
    // =========================================================

    @SubscribeEvent
    public static void onPlayerChangedDimension(PlayerEvent.PlayerChangedDimensionEvent event) {

        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }

        // Fakedreamに入った時だけ
        if (!event.getTo().equals(FakedreamDimension.FAKEDREAM_LEVEL)) {
            return;
        }

        ServerLevel level = player.server.getLevel(FakedreamDimension.FAKEDREAM_LEVEL);
        if (level == null) return;

        // 既に大木があれば何もしない
        if (hasGreatTree(level)) {
            return;
        }

        // 1本生成
        generateGreatTree(level);
    }

    // =========================================================
    // 存在チェック
    // =========================================================

    /**
     * (0, TREE_Y, 0) 付近に桜の原木があるか判定する。
     */
    private static boolean hasGreatTree(ServerLevel level) {

        BlockPos center = new BlockPos(TREE_X, TREE_Y, TREE_Z);

        // チャンクをロード
        level.getChunkAt(center);

        for (int y = TREE_Y - 10; y <= TREE_Y + SCAN_RANGE; y++) {
            BlockPos pos = new BlockPos(TREE_X, y, TREE_Z);
            if (level.getBlockState(pos).is(Blocks.CHERRY_LOG)) {
                return true;
            }
        }

        // 周囲2マスもチェック（2×2の幹対策）
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                for (int y = TREE_Y - 10; y <= TREE_Y + SCAN_RANGE; y++) {
                    BlockPos pos = new BlockPos(TREE_X + dx, y, TREE_Z + dz);
                    if (level.getBlockState(pos).is(Blocks.CHERRY_LOG)) {
                        return true;
                    }
                }
            }
        }

        return false;
    }

    // =========================================================
    // 生成
    // =========================================================

    /**
     * バニラのチェリーツリー feature を (0, ?, 0) に1回だけ配置する。
     */
    private static void generateGreatTree(ServerLevel level) {

        BlockPos origin = new BlockPos(TREE_X, TREE_Y, TREE_Z);
        level.getChunkAt(origin);

        // 地表の高さを取得
        BlockPos surfacePos = level.getHeightmapPos(
                Heightmap.Types.WORLD_SURFACE_WG,
                origin
        );

        System.out.println("[Fakedream] Generating cherry tree at surface Y="
                + surfacePos.getY());

        // =========================================================
        // バニラの configured feature を取得
        // =========================================================
        var configuredRegistry = level.registryAccess()
                .registryOrThrow(Registries.CONFIGURED_FEATURE);

        Optional<ConfiguredFeature<?, ?>> treeFeature =
                configuredRegistry.getOptional(TREE_FEATURE_ID);

        if (treeFeature.isEmpty()) {
            System.out.println("[Fakedream] ERROR: " + TREE_FEATURE_ID + " not found!");
            return;
        }

        // =========================================================
        // placed feature を組み立てずに、直接 configured を place する
        // =========================================================
        RandomSource random = level.getRandom();

        // バニラの cherry は 1本の木を生成する configuredfeature なので、
        // そのまま place でOK
        boolean placed = treeFeature.get().place(
                level,
                level.getChunkSource().getGenerator(),
                random,
                surfacePos
        );

        System.out.println("[Fakedream] Cherry tree placed: " + placed);

        // =========================================================
        // 1本じゃ物足りない場合：周囲に少しだけ追加で桜を生やす
        // =========================================================
        // (コメントアウト中)
        // for (int i = 0; i < 4; i++) {
        //     BlockPos extra = surfacePos.offset(
        //             random.nextInt(17) - 8,
        //             0,
        //             random.nextInt(17) - 8
        //     );
        //     extra = level.getHeightmapPos(
        //             Heightmap.Types.WORLD_SURFACE_WG,
        //             extra
        //     );
        //     treeFeature.get().place(
        //             level,
        //             level.getChunkSource().getGenerator(),
        //             random,
        //             extra
        //     );
        // }
    }
}