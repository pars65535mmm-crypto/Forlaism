package com.tyami.forlaism.world;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;

import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.Iterator;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * クロノスダガーによる時間減速（1/10）を管理する。
 *
 * Mixin (ChronosSlowMixin) が tick() のたびに本クラスへ問い合わせ、
 * 「減速中かつ、このtickを通すべきでない」なら tick 本体をスキップする。
 *
 * 1/10 の実装：
 *   tickCount を 10 で割った余りが 0 の時だけ通過させる。
 *   → 実質 10 tick に 1 回しか動かない。
 */
@Mod.EventBusSubscriber(modid = "forlaism")
public final class ChronosSlowManager {

    /** 減速倍率の分母。10 なら 1/10 速。 */
    public static final int SLOW_DIVISOR = 10;

    /** 効果時間（tick）。10秒 = 200 tick。 */
    public static final int DURATION_TICKS = 200;

    /** 対象UUID → 残り tick。 */
    private static final Map<UUID, Integer> SLOWED = new ConcurrentHashMap<>();

    private ChronosSlowManager() {
    }

    // =========================================================
    // API
    // =========================================================

    /**
     * 周囲の敵を減速させる。
     */
    public static void applyArea(ServerLevel level, double cx, double cy, double cz, double radius) {
        double r2 = radius * radius;

        var targets = level.getEntitiesOfClass(
                LivingEntity.class,
                new net.minecraft.world.phys.AABB(
                        cx - radius, cy - radius, cz - radius,
                        cx + radius, cy + radius, cz + radius
                ),
                e -> e.isAlive() && !e.isSpectator()
        );

        for (LivingEntity t : targets) {
            if (t.distanceToSqr(cx, cy, cz) > r2) continue;
            SLOWED.put(t.getUUID(), DURATION_TICKS);
            spawnApplyEffect(level, t);
        }
    }

    /** 減速中か。 */
    public static boolean isSlowed(Entity entity) {
        if (entity == null) return false;
        return SLOWED.containsKey(entity.getUUID());
    }

    /** 残り時間。 */
    public static int getRemaining(Entity entity) {
        if (entity == null) return 0;
        return SLOWED.getOrDefault(entity.getUUID(), 0);
    }

    /**
     * このtickを通過させるべきか。
     *
     * true を返したら tick を通す、false なら tick をスキップ。
     */
    public static boolean shouldPassTick(Entity entity) {
        if (!isSlowed(entity)) return true;
        // 10 tick に 1 回だけ通過
        return entity.tickCount % SLOW_DIVISOR == 0;
    }

    // =========================================================
    // Tick
    // =========================================================

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        if (SLOWED.isEmpty()) return;

        var server = event.getServer();
        if (server == null) return;

        Iterator<Map.Entry<UUID, Integer>> it = SLOWED.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<UUID, Integer> e = it.next();
            int next = e.getValue() - 1;
            if (next <= 0) {
                it.remove();
            } else {
                SLOWED.put(e.getKey(), next);
            }
        }
    }

    // =========================================================
    // 演出
    // =========================================================

    private static void spawnApplyEffect(ServerLevel level, LivingEntity target) {
        level.sendParticles(
                ParticleTypes.END_ROD,
                target.getX(),
                target.getY() + target.getBbHeight() * 0.5,
                target.getZ(),
                12,
                0.4, 0.6, 0.4,
                0.02
        );
        level.sendParticles(
                ParticleTypes.SOUL_FIRE_FLAME,
                target.getX(),
                target.getY() + target.getBbHeight() * 0.5,
                target.getZ(),
                6,
                0.3, 0.4, 0.3,
                0.01
        );
    }
}