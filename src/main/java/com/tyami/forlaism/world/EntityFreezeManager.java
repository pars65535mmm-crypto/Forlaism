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
 * entity の時間停止（tick skip）を管理する。
 *
 * 単純に tick() をスキップすると、entity の内部時間 (tickCount 等) が
 * 全く進まないため、解除時に「停止していた分の時間」を後で進めてあげる。
 *
 * Mixin (EntityFreezeMixin) は freeze 中は tick() 本体を実行せず、
 * tickCount++ だけは行うようにしている。
 * これにより:
 *   - AI は動かない (tick ロジックが動かない)
 *   - 見た目のアニメーション (tickCount 依存) は進む
 *   - 再開後も tickCount が破綻しない
 */
@Mod.EventBusSubscriber(modid = "forlaism")
public final class EntityFreezeManager {

    /** entity UUID -> 残り凍結 tick 数。 */
    private static final Map<UUID, Integer> FROZEN = new ConcurrentHashMap<>();

    private EntityFreezeManager() {
    }

    // =========================================================
    // API
    // =========================================================

    /**
     * entity を指定 tick 数だけ凍結する。
     */
    public static void freeze(Entity entity, int durationTicks) {
        if (entity == null) return;
        if (entity.level().isClientSide) return;
        if (durationTicks <= 0) return;

        UUID id = entity.getUUID();

        // 既に凍結中なら延長
        int existing = FROZEN.getOrDefault(id, 0);
        FROZEN.put(id, existing + durationTicks);
    }

    /**
     * 現在凍結中か。
     */
    public static boolean isFrozen(Entity entity) {
        if (entity == null) return false;
        return FROZEN.containsKey(entity.getUUID());
    }

    /**
     * 残り凍結 tick 数。
     */
    public static int getRemaining(Entity entity) {
        if (entity == null) return 0;
        return FROZEN.getOrDefault(entity.getUUID(), 0);
    }

    // =========================================================
    // Tick
    // =========================================================

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        if (FROZEN.isEmpty()) return;

        var server = event.getServer();
        if (server == null) return;

        // ConcurrentHashMap の entrySet().removeIf() で
        // entry.setValue() すると UnsupportedOperationException が出るため、
        // Iterator を使って手動で書き換える。
        Iterator<Map.Entry<UUID, Integer>> it = FROZEN.entrySet().iterator();

        while (it.hasNext()) {
            Map.Entry<UUID, Integer> entry = it.next();

            UUID id = entry.getKey();
            int remaining = entry.getValue() - 1;

            if (remaining <= 0) {
                it.remove();
                continue;
            }

            // ★ Iterator 経由の setValue でもダメなケースがあるので、
            //   必ず ConcurrentHashMap.put() で更新する。
            FROZEN.put(id, remaining);

            // 凍結中の演出
            Entity entity = findByUUID(server, id);
            if (entity instanceof LivingEntity living
                    && entity.level() instanceof ServerLevel sl) {

                if (sl.getGameTime() % 5 == 0) {
                    sl.sendParticles(
                            ParticleTypes.SNOWFLAKE,
                            living.getX(),
                            living.getY() + living.getBbHeight() * 0.5,
                            living.getZ(),
                            3,
                            0.3, 0.3, 0.3,
                            0.01
                    );
                    sl.sendParticles(
                            ParticleTypes.END_ROD,
                            living.getX(),
                            living.getY() + living.getBbHeight() * 0.5,
                            living.getZ(),
                            1,
                            0.2, 0.2, 0.2,
                            0.005
                    );
                }
            }
        }
    }

    private static Entity findByUUID(net.minecraft.server.MinecraftServer server, UUID id) {
        for (ServerLevel level : server.getAllLevels()) {
            Entity e = level.getEntity(id);
            if (e != null) return e;
        }
        return null;
    }
}