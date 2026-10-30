package com.tyami.forlaism.event;

import com.tyami.forlaism.Forlaism;
import com.tyami.forlaism.item.RapruslsathenelrterItem;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;

import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.UUID;

/**
 * ラプラスサジネーターの拘束（輝星 / 贄星）の解除と維持を管理する。
 *
 * 【軽量化】
 *   旧: 毎tick、全ディメンションの全LivingEntityを走査（激重）
 *   新: 拘束中のUUIDだけをSetで保持し、そのEntityだけを処理
 */
@Mod.EventBusSubscriber(modid = Forlaism.MOD_ID)
public final class RaprusBindingHandler {

    /** 拘束中のEntity UUIDセット。 */
    private static final Set<UUID> BOUND = new HashSet<>();

    private RaprusBindingHandler() {
    }

    // =========================================================
    // 外部から呼ぶAPI（拘束開始時）
    // =========================================================

    public static void registerBound(LivingEntity entity) {
        BOUND.add(entity.getUUID());
    }

    public static void unregisterBound(LivingEntity entity) {
        BOUND.remove(entity.getUUID());
    }

    public static void clearAll() {
        BOUND.clear();
    }

    // =========================================================
    // 毎tick処理
    // =========================================================

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        if (BOUND.isEmpty()) return;

        MinecraftServer server = event.getServer();
        if (server == null) return;

        long now = server.overworld().getGameTime();

        Iterator<UUID> it = BOUND.iterator();

        while (it.hasNext()) {
            UUID uuid = it.next();

            LivingEntity entity = findEntity(server, uuid);

            // 見つからない or 死んだ → 解除
            if (entity == null || !entity.isAlive()) {
                it.remove();
                continue;
            }

            CompoundTag data = entity.getPersistentData();

            // 拘束フラグが消えていたら（他の経路で解除された）→ Set から削除
            if (!data.getBoolean(RapruslsathenelrterItem.TAG_BOUND)) {
                it.remove();
                continue;
            }

            long until = data.getLong(RapruslsathenelrterItem.TAG_BOUND_UNTIL);

            // 解除時刻を過ぎた
            if (now >= until) {
                data.remove(RapruslsathenelrterItem.TAG_BOUND);
                data.remove(RapruslsathenelrterItem.TAG_BOUND_UNTIL);

                if (entity instanceof Mob mob) {
                    mob.setNoAi(false);
                }
                it.remove();
                continue;
            }

            // 拘束中の動作固定
            entity.setDeltaMovement(0, 0, 0);
            entity.hurtMarked = true;

            if (entity instanceof Mob mob) {
                mob.setNoAi(true);
                mob.setTarget(null);
            }

            // 見た目パーティクル（5tick毎）
            if (now % 5 == 0 && entity.level() instanceof ServerLevel sl) {
                sl.sendParticles(
                        net.minecraft.core.particles.ParticleTypes.END_ROD,
                        entity.getX(),
                        entity.getY() + entity.getBbHeight() * 0.5,
                        entity.getZ(),
                        3, 0.3, 0.3, 0.3, 0.02
                );
            }
        }
    }

    // =========================================================
    // UUIDからEntityを探す（全ディメンションを舐める）
    // =========================================================

    private static LivingEntity findEntity(MinecraftServer server, UUID uuid) {
        for (ServerLevel level : server.getAllLevels()) {
            Entity e = level.getEntity(uuid);
            if (e instanceof LivingEntity living) {
                return living;
            }
        }
        return null;
    }
}