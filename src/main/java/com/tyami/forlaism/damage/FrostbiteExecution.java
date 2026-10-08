package com.tyami.forlaism.damage;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 凍傷の進行管理。
 *
 * 凍傷中は毎tickチェックして、一定間隔でダメージ。
 * HPが20%以下になったら「凍死」させる。
 */
public final class FrostbiteExecution {

    /** 凍傷の1tickあたりダメージ間隔（20tick = 1秒）。 */
    private static final int DAMAGE_INTERVAL = 20;

    /** 凍傷1回あたりのダメージ。 */
    private static final float DAMAGE_PER_TICK = 3.0F;

    /** 凍死閾値（HP割合）。 */
    private static final float FROST_DEATH_THRESHOLD = 0.00F;

    /** 対象UUID → 残り凍傷tick。 */
    private static final Map<UUID, Integer> FROSTBITE_MAP = new ConcurrentHashMap<>();

    /** 対象UUID → 最後にダメージを与えたtick。 */
    private static final Map<UUID, Integer> LAST_DAMAGE_TICK = new ConcurrentHashMap<>();

    private FrostbiteExecution() {
    }

    // =========================================================
    // 付与
    // =========================================================

    /**
     * 凍傷を付与（既存なら延長）。
     */
    public static void apply(LivingEntity target, int durationTicks) {
        if (target.level().isClientSide) return;
        if (target instanceof Player p && p.isCreative()) return;

        UUID id = target.getUUID();
        int current = FROSTBITE_MAP.getOrDefault(id, 0);
        FROSTBITE_MAP.put(id, Math.max(current, durationTicks));
    }

    public static boolean isFrostbitten(LivingEntity target) {
        return FROSTBITE_MAP.containsKey(target.getUUID());
    }

    // =========================================================
    // 毎tick処理（サーバー側で全Mobを舐める）
    // =========================================================

    /**
     * ForgeのServerTickEventから呼ばれる想定。
     */
    public static void tick(net.minecraft.server.MinecraftServer server) {

        if (FROSTBITE_MAP.isEmpty()) return;

        int now = server.getTickCount();

        var it = FROSTBITE_MAP.entrySet().iterator();
        while (it.hasNext()) {
            var entry = it.next();
            UUID id = entry.getKey();
            int remaining = entry.getValue() - 1;

            // 対象を検索
            LivingEntity target = findEntity(server, id);
            if (target == null || !target.isAlive()) {
                it.remove();
                LAST_DAMAGE_TICK.remove(id);
                continue;
            }

            // 残り時間更新
            if (remaining <= 0) {
                it.remove();
                LAST_DAMAGE_TICK.remove(id);
                continue;
            }
            entry.setValue(remaining);

            // =========================================================
            // 毎秒ダメージ
            // =========================================================
            int lastTick = LAST_DAMAGE_TICK.getOrDefault(id, 0);
            if (now - lastTick >= DAMAGE_INTERVAL) {
                LAST_DAMAGE_TICK.put(id, now);

                if (target.level() instanceof ServerLevel sl) {
                    // 凍傷ダメージ
                    target.invulnerableTime = 0;
                    target.hurt(FrostbiteDamageSource.of(sl, null), DAMAGE_PER_TICK);

                    // 雪の結晶パーティクル
                    sl.sendParticles(
                            ParticleTypes.SNOWFLAKE,
                            target.getX(),
                            target.getY() + target.getBbHeight() * 0.5,
                            target.getZ(),
                            12, 0.4, 0.4, 0.4, 0.02
                    );

                    sl.playSound(null,
                            target.getX(), target.getY(), target.getZ(),
                            SoundEvents.PLAYER_HURT_FREEZE,
                            SoundSource.PLAYERS, 0.8F, 1.2F
                    );

                    // =========================================================
                    // 凍死判定
                    // =========================================================
                    float hpRatio = target.getHealth() / target.getMaxHealth();
                    if (hpRatio <= FROST_DEATH_THRESHOLD) {
                        triggerFrostDeath(sl, target);
                        it.remove();
                        LAST_DAMAGE_TICK.remove(id);
                    }
                }
            }
        }
    }

    // =========================================================
    // 凍死
    // =========================================================

    private static void triggerFrostDeath(ServerLevel sl, LivingEntity target) {

        // 演出
        sl.sendParticles(
                ParticleTypes.SNOWFLAKE,
                target.getX(),
                target.getY() + target.getBbHeight() * 0.5,
                target.getZ(),
                100, 0.8, 0.8, 0.8, 0.3
        );
        sl.sendParticles(
                ParticleTypes.FLASH,
                target.getX(),
                target.getY() + target.getBbHeight() * 0.5,
                target.getZ(),
                1, 0, 0, 0, 0
        );
        sl.playSound(null,
                target.getX(), target.getY(), target.getZ(),
                SoundEvents.GLASS_BREAK,
                SoundSource.PLAYERS, 2.0F, 0.5F
        );

        // HP 0 にして死亡
        target.setHealth(0.0F);
        if (!target.isDeadOrDying()) {
            target.die(FrostbiteDamageSource.of(sl, null));
        }
    }

    // =========================================================
    // ユーティリティ
    // =========================================================

    private static LivingEntity findEntity(net.minecraft.server.MinecraftServer server, UUID id) {
        for (ServerLevel level : server.getAllLevels()) {
            var e = level.getEntity(id);
            if (e instanceof LivingEntity living) return living;
        }
        return null;
    }
}