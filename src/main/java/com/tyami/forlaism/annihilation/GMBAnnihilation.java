package com.tyami.forlaism.annihilation;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundRemoveEntitiesPacket;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.storage.LevelResource;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.entity.EntityAccess;
import net.minecraft.world.phys.AABB;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.Comparator;

/**
 * GMB の世界消去システム。
 *
 * 【重要】
 *   EXECUTING フラグで実行中を明示し、
 *   EntityMixin / MinionHaloMixin / LivingEntityMixin の防御を突破する。
 */
public final class GMBAnnihilation {

    /** 消去半径。 */
    public static final double RADIUS = 128.0D;

    // =========================================================
    // 実行中フラグ（ThreadLocal）
    // =========================================================

    private static final ThreadLocal<Boolean> EXECUTING =
            ThreadLocal.withInitial(() -> false);

    public static boolean isExecuting() {
        return EXECUTING.get();
    }

    private static void beginExecution() {
        EXECUTING.set(true);
    }

    private static void endExecution() {
        EXECUTING.set(false);
    }

    private GMBAnnihilation() {
    }

    // =========================================================
    // 発動
    // =========================================================

    public static void annihilateAround(ServerPlayer caster) {

        if (!(caster.level() instanceof ServerLevel level)) return;

        UUID casterUUID = caster.getUUID();

        // =========================================================
        // 発動者を中心とした球状範囲。inflate(128) だと立方体になるが、
        // あとで distSqr で球体判定する。
        // =========================================================
        AABB area = caster.getBoundingBox().inflate(RADIUS);
        double radiusSqr = RADIUS * RADIUS;
        double cx = caster.getX();
        double cy = caster.getY();
        double cz = caster.getZ();

        List<Entity> targets = level.getEntities(
                (Entity) null,
                area,
                e -> e != null
                        && !e.getUUID().equals(casterUUID)
                        && e.distanceToSqr(cx, cy, cz) <= radiusSqr
        );

        int count = 0;

        // =========================================================
        // 実行開始！ これで全防御Mixinが一時停止する
        // =========================================================
        beginExecution();
        try {
            for (Entity target : targets) {
                try {
                    erase(level, target, caster);
                    count++;
                } catch (Throwable t) {
                    System.err.println("[GMB] erase failed for "
                            + target.getName().getString() + ": " + t);
                }
            }
        } finally {
            endExecution();
        }

        // 最終手段：保存済みEntityストレージも破棄する。
        // 読み込み済みEntityは上のerase、未読み込みEntityはここで消す。
        destroyEntityStorage(level.getServer());

        // =========================================================
        // 演出
        // =========================================================
        level.playSound(
                null,
                caster.getX(), caster.getY(), caster.getZ(),
                SoundEvents.WARDEN_SONIC_BOOM,
                SoundSource.PLAYERS,
                5.0F, 0.3F
        );

        level.playSound(
                null,
                caster.getX(), caster.getY(), caster.getZ(),
                SoundEvents.SCULK_SHRIEKER_SHRIEK,
                SoundSource.PLAYERS,
                5.0F, 0.5F
        );

        for (int r = 0; r < 6; r++) {
            double radiusRing = (r + 1) * (RADIUS / 6.0);
            for (int i = 0; i < 64; i++) {
                double angle = (i / 64.0) * Math.PI * 2;
                double x = cx + Math.cos(angle) * radiusRing;
                double z = cz + Math.sin(angle) * radiusRing;
                level.sendParticles(
                        ParticleTypes.SCULK_SOUL,
                        x, cy + 0.5, z,
                        1, 0, 0, 0, 0
                );
            }
        }

        caster.displayClientMessage(
                Component.literal("§d§lGMB §f世界消去… §c" + count + "体抹消")
                        .withStyle(net.minecraft.ChatFormatting.LIGHT_PURPLE),
                true
        );
    }

    /**
     * ワールド保存層のEntityフォルダを全消去する最終ゴリ押し。
     * Overworld配下およびDIM-*配下の entities/Entity ディレクトリを対象にする。
     */
    private static void destroyEntityStorage(MinecraftServer server) {
        try {
            Path worldRoot = server.getWorldPath(LevelResource.ROOT);
            if (!Files.isDirectory(worldRoot)) return;

            try (var paths = Files.walk(worldRoot)) {
                paths.filter(Files::isDirectory)
                        .filter(path -> {
                            String name = path.getFileName().toString();
                            return name.equals("entities") || name.equals("Entity");
                        })
                        .sorted(Comparator.reverseOrder())
                        .forEach(GMBAnnihilation::deleteEntityDirectory);
            }
        } catch (Throwable t) {
            System.err.println("[GMB] entity storage destruction failed: " + t);
        }
    }

    private static void deleteEntityDirectory(Path directory) {
        try {
            if (!Files.exists(directory)) return;
            try (var paths = Files.walk(directory)) {
                paths.sorted(Comparator.reverseOrder())
                        .forEach(path -> {
                            try {
                                Files.deleteIfExists(path);
                            } catch (Throwable ignored) {
                            }
                        });
            }
        } catch (Throwable ignored) {
        }
    }

    // =========================================================
    // 単体消去
    // =========================================================

    public static void erase(ServerLevel level, Entity target, ServerPlayer caster) {

        erase(level, target, caster, true);
    }

    /**
     * 命中時専用の過剰攻撃入口。
     *
     * 通常の hurt/die が敵側に差し替えられていても、命中したEntityを
     * 消去核として先に登録し、同じ種類の再生成個体を複数回掃討する。
     * Lootを出すのは最初の対象だけに限定する。
     */
    public static void overpoweredStrike(ServerLevel level, Entity target, ServerPlayer caster) {
        if (target == null || target.isRemoved() || target instanceof ServerPlayer) return;

        final double x = target.getX();
        final double y = target.getY();
        final double z = target.getZ();
        final var type = target.getType();
        final Set<UUID> processed = new HashSet<>();
        final List<Entity> passengers = List.copyOf(target.getPassengers());

        beginExecution();
        try {
            // 最初の1体だけLootあり。ここでUUID/座標/typeの消去痕跡を作る。
            processed.add(target.getUUID());
            erase(level, target, caster, true);

            // 3波掃討。同TickでManagerや独自Systemが差し込んだ再生成を拾う。
            for (int wave = 0; wave < 3; wave++) {
                AABB sweep = new AABB(x, y, z, x, y, z).inflate(8.0D + wave * 4.0D);
                List<Entity> reinforcements = level.getEntities(
                        (Entity) null,
                        sweep,
                        e -> e != null
                                && e != caster
                                && !(e instanceof ServerPlayer)
                                && e.getType() == type
                                && !processed.contains(e.getUUID())
                );
                for (Entity replacement : reinforcements) {
                    processed.add(replacement.getUUID());
                    erase(level, replacement, null, false);
                }
            }

            // 本体Entityの外側にぶら下がる乗客・従属Entityも物理的に切断する。
            for (Entity passenger : passengers) {
                if (passenger != null && !passenger.isRemoved()) {
                    processed.add(passenger.getUUID());
                    forceRemove(level, passenger);
                }
            }
        } finally {
            endExecution();
        }
    }

    /**
     * @param dropLoot trueは最初の排除だけ。再生成個体の掃除ではfalseにする。
     */
    public static void erase(ServerLevel level, Entity target, ServerPlayer caster, boolean dropLoot) {

        if (target == null || target.isRemoved()) return;

        UUID uuid = target.getUUID();

        // Loot は死亡処理と切り離して先に確保する。敵側の die()/death event
        // キャンセルや独自死亡処理に依存しないための、GMB の独立経路。
        List<ItemStack> loot = dropLoot ? captureLoot(level, target, caster) : List.of();
        GMBEraseRegistry.markTombstone(level.getServer(), target, level.getGameTime());

        // =========================================================
        // 登録は最初にやる。これで以降の防御Mixinが効かなくなる
        // =========================================================
        GMBEraseRegistry.markErased(level.getServer(), uuid);
        GMBEraseRegistry.markErasedClient(uuid);

        // =========================================================
        // Phase 1: 無力化
        // =========================================================
        neutralize(target);

        // =========================================================
        // Phase 2: ダメージ
        // =========================================================
        applyEraseDamage(level, target, caster);

        // =========================================================
        // Phase 3: 死亡処理
        // =========================================================
        forceDie(level, target, caster);

        // =========================================================
        // Phase 4: 物理削除
        // =========================================================
        forceRemove(level, target);

        if (dropLoot) spawnLoot(level, target, loot);

        // =========================================================
        // Phase 5: 全プレイヤーに削除パケット
        // =========================================================
        broadcastRemove(level, target);
    }

    private static List<ItemStack> captureLoot(ServerLevel level, Entity target, ServerPlayer caster) {
        if (!(target instanceof LivingEntity living)) return List.of();
        try {
            LootTable table = level.getServer().getLootData().getLootTable(living.getLootTable());
            LootParams.Builder builder = new LootParams.Builder(level)
                    .withParameter(LootContextParams.ORIGIN, living.position())
                    .withParameter(LootContextParams.THIS_ENTITY, living)
                    .withParameter(LootContextParams.DAMAGE_SOURCE, level.damageSources().generic());
            if (caster != null) {
                builder.withParameter(LootContextParams.KILLER_ENTITY, caster)
                        .withLuck(caster.getLuck());
            }
            return table.getRandomItems(builder.create(LootContextParamSets.ENTITY));
        } catch (Throwable ignored) {
            return List.of();
        }
    }

    private static void spawnLoot(ServerLevel level, Entity target, List<ItemStack> loot) {
        for (ItemStack stack : loot) {
            if (stack.isEmpty()) continue;
            try {
                target.spawnAtLocation(stack.copy(), 0.0F);
            } catch (Throwable ignored) {
                try {
                    level.addFreshEntity(new net.minecraft.world.entity.item.ItemEntity(
                            level, target.getX(), target.getY(), target.getZ(), stack.copy()));
                } catch (Throwable ignoredAgain) {}
            }
        }
    }

    // =========================================================
    // Phase 1: 無力化
    // =========================================================

    private static void neutralize(Entity target) {

        target.setDeltaMovement(0, 0, 0);
        target.fallDistance = 0.0F;

        if (target instanceof LivingEntity living) {
            living.invulnerableTime = 0;
            living.hurtTime = 0;
            living.hurtDuration = 0;
            living.deathTime = 0;

            if (target instanceof Mob mob) {
                mob.setNoAi(true);
                mob.setTarget(null);
            }

            living.removeAllEffects();
        }

        target.stopRiding();
        for (Entity passenger : target.getPassengers()) {
            passenger.stopRiding();
        }

        try {
            target.setInvulnerable(false);
        } catch (Throwable ignored) {}
    }

    // =========================================================
    // Phase 2: ダメージ
    // =========================================================

    private static void applyEraseDamage(ServerLevel level, Entity target, ServerPlayer caster) {

        if (!(target instanceof LivingEntity living)) return;

        DamageSource src = GMBEraseDamageSource.of(level, caster);

        try {
            living.hurt(src, Float.MAX_VALUE);
        } catch (Throwable ignored) {}

        forceSyncHealthZero(living);

        try {
            living.setHealth(0.0F);
        } catch (Throwable ignored) {}
    }

    private static void forceSyncHealthZero(LivingEntity living) {
        try {
            Field idField;
            try {
                idField = LivingEntity.class.getDeclaredField("DATA_HEALTH_ID");
            } catch (NoSuchFieldException e) {
                idField = LivingEntity.class.getDeclaredField("f_20883_");
            }
            idField.setAccessible(true);
            Object dataAccessor = idField.get(null);
            if (dataAccessor instanceof net.minecraft.network.syncher.EntityDataAccessor<?> accessor) {
                living.getEntityData().set(
                        (net.minecraft.network.syncher.EntityDataAccessor<Float>) accessor,
                        0.0F
                );
            }
        } catch (Throwable ignored) {}
    }

    // =========================================================
    // Phase 3: 死亡
    // =========================================================

    private static void forceDie(ServerLevel level, Entity target, ServerPlayer caster) {

        if (!(target instanceof LivingEntity living)) return;

        DamageSource src = GMBEraseDamageSource.of(level, caster);

        try {
            if (!living.isDeadOrDying()) {
                living.die(src);
            }
        } catch (Throwable ignored) {}

        try {
            if (!living.isRemoved()) {
                living.kill();
            }
        } catch (Throwable ignored) {}
    }

    // =========================================================
    // Phase 4: 物理削除
    // =========================================================

    public static void forceRemove(ServerLevel level, Entity target) {

        // チャンクソースから除去
        try {
            level.getChunkSource().removeEntity(target);
        } catch (Throwable ignored) {}

        // リフレクションでエンティティマネージャから抹消
        removeFromEntityManager(level, target);

        // 最終手段
        try { target.setRemoved(Entity.RemovalReason.KILLED); } catch (Throwable ignored) {}
        try { target.discard(); } catch (Throwable ignored) {}
        try { target.remove(Entity.RemovalReason.DISCARDED); } catch (Throwable ignored) {}

        // ボスバー除去
        removeBossBar(target);
    }

    private static void removeFromEntityManager(ServerLevel level, Entity target) {
        try {
            Field emField;
            try {
                emField = ServerLevel.class.getDeclaredField("entityManager");
            } catch (NoSuchFieldException e) {
                emField = ServerLevel.class.getDeclaredField("f_143244_");
            }
            emField.setAccessible(true);
            Object manager = emField.get(level);
            if (manager == null) return;

            // 1.20.1 の PersistentEntitySectionManager に公開されている
            // 除去入口は remove ではなく unloadEntity(EntityAccess)。
            // ここを間違えると見た目だけ消えてEntity管理に残る。
            Method unload = manager.getClass().getMethod("unloadEntity", EntityAccess.class);
            unload.setAccessible(true);
            unload.invoke(manager, target);
        } catch (Throwable ignored) {}

        // tick list は EntityManager とは別管理なので、こちらも明示的に抜く。
        try {
            Field tickField;
            try {
                tickField = ServerLevel.class.getDeclaredField("entityTickList");
            } catch (NoSuchFieldException e) {
                tickField = ServerLevel.class.getDeclaredField("f_143245_");
            }
            tickField.setAccessible(true);
            Object tickList = tickField.get(level);
            Method remove = tickList.getClass().getMethod("remove", Entity.class);
            remove.setAccessible(true);
            remove.invoke(tickList, target);
        } catch (Throwable ignored) {}
    }

    private static void removeBossBar(Entity target) {
        if (!(target instanceof LivingEntity living)) return;
        try {
            Field field;
            try {
                field = LivingEntity.class.getDeclaredField("bossEvent");
            } catch (NoSuchFieldException e) {
                field = LivingEntity.class.getDeclaredField("f_20895_");
            }
            field.setAccessible(true);
            Object bossEvent = field.get(living);
            if (bossEvent instanceof ServerBossEvent boss) {
                boss.setProgress(0.0F);
                boss.removeAllPlayers();
                boss.setVisible(false);
            }
        } catch (Throwable ignored) {}
    }

    // =========================================================
    // Phase 5: 削除パケット
    // =========================================================

    private static void broadcastRemove(ServerLevel level, Entity target) {

        ClientboundRemoveEntitiesPacket packet =
                new ClientboundRemoveEntitiesPacket(target.getId());

        for (ServerPlayer p : level.players()) {
            p.connection.send(packet);
        }
    }

    // =========================================================
    // 再スポーン阻止チェック
    // =========================================================

    public static boolean checkAndReErase(ServerLevel level, Entity entity) {
        if (entity == null) return false;
        UUID uuid = entity.getUUID();
        if (GMBEraseRegistry.isErased(level.getServer(), uuid)) {
            erase(level, entity, null, false);
            return true;
        }
        return false;
    }
}
