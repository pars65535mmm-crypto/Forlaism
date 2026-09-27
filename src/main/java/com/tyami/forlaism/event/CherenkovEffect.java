package com.tyami.forlaism.event;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * チェレンコフ光演出。
 *
 * 加工成功時に呼ばれ、0.8秒間、青白い光の粒が
 * 中央の縦穴を螺旋状に駆け上る。
 */
public final class CherenkovEffect {

    /** 演出の持続時間（tick）。0.8秒 = 16tick。 */
    private static final int DURATION = 16;

    /** 縦穴の高さ（Y1〜Y6 の6マス）。 */
    private static final int HOLE_HEIGHT = 6;

    private static final List<Entry> ACTIVE = new ArrayList<>();

    private CherenkovEffect() {
    }

    public static void play(ServerLevel level, BlockPos center) {
        ACTIVE.add(new Entry(level, center.immutable(), 0));
    }

    /**
     * 毎tick呼ぶ。ForlaismReactorMultiblockTicker から。
     */
    public static void tick(ServerLevel level) {
        if (ACTIVE.isEmpty()) return;

        Iterator<Entry> it = ACTIVE.iterator();
        while (it.hasNext()) {
            Entry e = it.next();

            if (e.level != level) continue;

            e.tick++;

            spawnFrame(level, e.center, e.tick);

            if (e.tick >= DURATION) {
                it.remove();
            }
        }
    }

    private static void spawnFrame(ServerLevel level, BlockPos center, int tick) {

        double cx = center.getX() + 0.5;
        double cz = center.getZ() + 0.5;
        double baseY = center.getY() + 0.5;

        // 進行度 0.0 → 1.0
        float progress = tick / (float) DURATION;

        // 光の輪の半径（最初大きく、だんだん縮む）
        double radius = 1.2 - progress * 0.9;

        // =====================================================
        // 1. 螺旋状に駆け上がる光の粒
        // =====================================================
        int spiralCount = 3;
        for (int i = 0; i < spiralCount; i++) {
            double angle = (tick * 0.6 + i * (Math.PI * 2.0 / spiralCount));

            // 高さは progress に応じて上に
            double y = baseY + progress * HOLE_HEIGHT;

            double x = cx + Math.cos(angle) * radius;
            double z = cz + Math.sin(angle) * radius;

            // チェレンコフ青（END_ROD は白いので、SOUL_FIRE_FLAME で青を強調）
            level.sendParticles(
                    ParticleTypes.SOUL_FIRE_FLAME,
                    x, y, z,
                    1, 0, 0, 0, 0
            );

            // 電気の火花をたまに
            if (tick % 2 == 0) {
                level.sendParticles(
                        ParticleTypes.ELECTRIC_SPARK,
                        x, y, z,
                        1, 0, 0, 0, 0
                );
            }
        }

        // =====================================================
        // 2. 中心から広がる青白い光の輪
        // =====================================================
        int ringPoints = 8;
        double ringRadius = 1.5 + progress * 2.0; // 広がる
        double ringY = baseY + 0.5;

        for (int i = 0; i < ringPoints; i++) {
            double angle = (i / (double) ringPoints) * Math.PI * 2.0;
            double x = cx + Math.cos(angle) * ringRadius;
            double z = cz + Math.sin(angle) * ringRadius;

            level.sendParticles(
                    ParticleTypes.END_ROD,
                    x, ringY, z,
                    1, 0, 0, 0, 0
            );
        }

        // =====================================================
        // 3. 縦穴を貫く光の柱
        // =====================================================
        if (tick % 3 == 0) {
            int columnHeight = (int) (progress * HOLE_HEIGHT);
            for (int y = 0; y <= columnHeight; y++) {
                level.sendParticles(
                        ParticleTypes.END_ROD,
                        cx, baseY + y, cz,
                        1, 0.05, 0, 0.05, 0
                );
            }
        }

        // =====================================================
        // 4. 最終段階: ドラゴンブレスで締め
        // =====================================================
        if (tick == DURATION - 1) {
            level.sendParticles(
                    ParticleTypes.DRAGON_BREATH,
                    cx, baseY + HOLE_HEIGHT * 0.5, cz,
                    60, 1.0, HOLE_HEIGHT * 0.5, 1.0, 0.1
            );

            level.sendParticles(
                    ParticleTypes.FLASH,
                    cx, baseY + HOLE_HEIGHT * 0.5, cz,
                    1, 0, 0, 0, 0
            );
        }
    }

    // =========================================================

    private static final class Entry {
        final ServerLevel level;
        final BlockPos center;
        int tick;

        Entry(ServerLevel level, BlockPos center, int tick) {
            this.level = level;
            this.center = center;
            this.tick = tick;
        }
    }
}