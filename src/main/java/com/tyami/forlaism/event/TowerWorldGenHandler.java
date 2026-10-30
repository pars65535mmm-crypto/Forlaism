package com.tyami.forlaism.event;

import com.tyami.forlaism.world.FakedreamDimension;
import com.tyami.forlaism.world.TowerBuilder;

import net.minecraft.server.level.ServerLevel;

import net.minecraftforge.event.level.LevelEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * FakeDream のワールドロード時に、塔を1回だけ建てる。
 *
 * 既に塔が建っているかどうかは、塔の中心付近のブロックで判定する。
 */
@Mod.EventBusSubscriber(modid = "forlaism")
public final class TowerWorldGenHandler {

    private TowerWorldGenHandler() {
    }

    @SubscribeEvent
    public static void onLevelLoad(LevelEvent.Load event) {
        if (!(event.getLevel() instanceof ServerLevel level)) return;

        // FakeDream 以外は無視
        if (!level.dimension().equals(FakedreamDimension.FAKEDREAM_LEVEL)) {
            return;
        }

        // 既に塔が建っているか確認
        if (isTowerAlreadyBuilt(level)) {
            return;
        }

        TowerBuilder.build(level);

        System.out.println("[Forlaism] FakeDream tower built at x="
                + TowerBuilder.TOWER_CENTER_X
                + ", z=" + TowerBuilder.TOWER_CENTER_Z);
    }

    /**
     * 塔が既に建っているか。
     *
     * 塔の最下段 Y=120 の中心ブロック（steel_block）が置かれているかで判定。
     */
    private static boolean isTowerAlreadyBuilt(ServerLevel level) {
        net.minecraft.core.BlockPos checkPos = new net.minecraft.core.BlockPos(
                TowerBuilder.TOWER_CENTER_X,
                TowerBuilder.TOWER_BASE_Y,
                TowerBuilder.TOWER_CENTER_Z
        );

        var state = level.getBlockState(checkPos);
        // STA の中心 (5, 5) は 'E' だが、
        // ワールド生成時点で中身が入っているかどうかはブロックの種類で判定できない。
        // そこで、塔の範囲内のどれか1つでも「forlaism:steel_block」があれば建て済みとみなす。
        for (int dx = -5; dx <= 5; dx++) {
            for (int dz = -5; dz <= 5; dz++) {
                var s = level.getBlockState(new net.minecraft.core.BlockPos(
                        TowerBuilder.TOWER_CENTER_X + dx,
                        TowerBuilder.TOWER_BASE_Y + 5,
                        TowerBuilder.TOWER_CENTER_Z + dz
                ));
                if (s.is(com.tyami.forlaism.registry.Blocks.STEEL_BLOCK.get())) {
                    return true;
                }
            }
        }

        return false;
    }
}