package com.tyami.forlaism.client.renderer;

import net.minecraft.core.BlockPos;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * クライアント側のチェレンコフ光演出状態。
 */
public final class CherenkovState {

    /** 演出1件。 */
    public static final class Instance {
        public final BlockPos center;
        public final int duration;
        public int tick;

        Instance(BlockPos center, int duration) {
            this.center = center;
            this.duration = duration;
            this.tick = 0;
        }

        /** 進行度 0.0 → 1.0。 */
        public float progress() {
            return tick / (float) duration;
        }

        /** 残り時間の割合。フェードアウトに使う。 */
        public float fadeAlpha() {
            float p = progress();
            // 最初0.15で立ち上がり、最後0.3で消える
            if (p < 0.15f) return p / 0.15f;
            if (p > 0.70f) return 1.0f - (p - 0.70f) / 0.30f;
            return 1.0f;
        }
    }

    private static final List<Instance> ACTIVE = new ArrayList<>();

    private CherenkovState() {
    }

    public static void start(BlockPos center, int duration) {
        ACTIVE.add(new Instance(center, duration));
    }

    public static List<Instance> getActive() {
        return ACTIVE;
    }

    public static void tick() {
        Iterator<Instance> it = ACTIVE.iterator();
        while (it.hasNext()) {
            Instance i = it.next();
            i.tick++;
            if (i.tick >= i.duration) {
                it.remove();
            }
        }
    }
}