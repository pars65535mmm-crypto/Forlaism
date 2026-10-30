package com.tyami.forlaism.world;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;

/**
 * FakeDream の塔を構築する。
 *
 * 中心 (1300, 230)、地面 Y=120 を起点に、
 *   STA (7) + NOW (20 × 24) + LAST (15) = 502
 * を設置。頂上は Y=622。
 */
public final class TowerBuilder {

    public static final int TOWER_CENTER_X = 1300;
    public static final int TOWER_CENTER_Z = 230;

    /** 塔の最下段の Y 座標。 */
    public static final int TOWER_BASE_Y = 120;

    /** 中間の重ね数。 */
    public static final int NOW_COUNT = 24;

    private TowerBuilder() {
    }

    /**
     * 塔を構築する。
     */
    public static void build(ServerLevel level) {
        // LAYERS のサイズは 11 × N × 11
        // LAYERS[y][z] = 1行(11文字) = X方向
        // LAYERS[y][z][x]

        // 中心 (1300, 230) が LAYERS の (5, 5) に来るようにオフセット
        // LAYERS は 0..10 のインデックスなので、中心は 5
        int originX = TOWER_CENTER_X - 5;
        int originZ = TOWER_CENTER_Z - 5;

        int y = TOWER_BASE_Y;

        // STA
        y = placeLayers(level, TowerLayers.STA, originX, y, originZ, true);

        // NOW × 24
        for (int i = 0; i < NOW_COUNT; i++) {
            y = placeLayers(level, TowerLayers.NOW, originX, y, originZ, false);
        }

        // LAST
        placeLayers(level, TowerLayers.LAST, originX, y, originZ, false);
    }

    /**
     * LAYERS を設置する。
     *
     * @param isSta   true なら STA パレット、false なら NOW パレット
     *                （LAST は別途 lastSymbol を呼ぶ）
     */
    private static int placeLayers(
            ServerLevel level,
            String[][] layers,
            int originX,
            int originY,
            int originZ,
            boolean isSta
    ) {
        for (int y = 0; y < layers.length; y++) {
            String[] layer = layers[y];
            for (int z = 0; z < layer.length; z++) {
                String row = layer[z];
                for (int x = 0; x < row.length(); x++) {
                    char c = row.charAt(x);
                    if (c == ' ') continue;

                    BlockState state = resolveSymbol(c, layers, isSta);
                    if (state == null) continue;

                    BlockPos pos = new BlockPos(
                            originX + x,
                            originY + y,
                            originZ + z
                    );

                    level.setBlock(pos, state, 3);
                }
            }
        }

        return originY + layers.length;
    }

    /**
     * シンボルを BlockState に解決する。
     *
     * どの LAYERS かによってパレットが違うので、
     * 配列の参照で判定する。
     */
    private static BlockState resolveSymbol(char c, String[][] layers, boolean isSta) {
        if (layers == TowerLayers.STA || isSta) {
            return TowerLayers.staSymbol(c);
        }
        if (layers == TowerLayers.NOW) {
            return TowerLayers.nowSymbol(c);
        }
        if (layers == TowerLayers.LAST) {
            return TowerLayers.lastSymbol(c);
        }
        return null;
    }
}