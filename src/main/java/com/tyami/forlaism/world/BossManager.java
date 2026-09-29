package com.tyami.forlaism.world;

import com.tyami.forlaism.Forlaism;
import com.tyami.forlaism.entity.BossCoreEntity;
import com.tyami.forlaism.network.BossSyncPacket;
import com.tyami.forlaism.registry.ModEntityTypes;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.network.PacketDistributor;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * サーバー側ボスマネージャ。
 */
@Mod.EventBusSubscriber(modid = Forlaism.MOD_ID)
public final class BossManager {

    public static final double HIT_RADIUS = 1.0D;
    public static final double HIT_HEIGHT = 1.8D;
    public static final int INVULNERABLE_TICKS = 10;
    public static final int SYNC_INTERVAL = 5;

    private static final java.util.concurrent.ConcurrentHashMap<UUID, UUID> CORE_IDS
            = new java.util.concurrent.ConcurrentHashMap<>();

    private BossManager() {
    }

    // =========================================================
    // スポーン
    // =========================================================

    public static BossInstance spawnBoss(
            ServerLevel level,
            String displayName,
            String skinId,
            double x, double y, double z
    ) {
        BossData data = BossData.get(level.getServer());
        BossInstance inst = new BossInstance(UUID.randomUUID(), displayName, skinId, x, y, z);
        data.bosses.put(inst.id, inst);
        data.setDirty();

        inst.bossBar = inst.createBossBar();
        for (ServerPlayer p : level.players()) {
            inst.bossBar.addPlayer(p);
        }

        spawnCore(level, inst);
        broadcastSync(level.getServer());

        return inst;
    }

    private static void spawnCore(ServerLevel level, BossInstance inst) {
        BossCoreEntity core = ModEntityTypes.BOSS_CORE.get().create(level);
        if (core == null) return;

        core.setBossId(inst.id);
        core.setBossDisplayName(inst.displayName);
        core.setSkinId(inst.skinId);
        core.moveTo(inst.x, inst.y, inst.z, inst.yaw, 0.0F);

        level.addFreshEntity(core);
        inst.coreEntityId = core.getUUID();
        CORE_IDS.put(inst.id, core.getUUID());
    }

    // =========================================================
    // 破棄
    // =========================================================

    public static void destroyBoss(MinecraftServer server, UUID bossId) {
        BossData data = BossData.get(server);
        BossInstance inst = data.bosses.remove(bossId);
        if (inst == null) return;

        if (inst.bossBar != null) {
            inst.bossBar.removeAllPlayers();
        }

        UUID coreId = CORE_IDS.remove(bossId);
        if (coreId != null) {
            for (ServerLevel lv : server.getAllLevels()) {
                Entity e = lv.getEntity(coreId);
                if (e != null) {
                    e.remove(Entity.RemovalReason.KILLED);
                }
            }
        }

        data.setDirty();
        broadcastSync(server);
    }

    public static List<BossInstance> getAll(MinecraftServer server) {
        return new ArrayList<>(BossData.get(server).bosses.values());
    }

    public static BossInstance get(MinecraftServer server, UUID id) {
        return BossData.get(server).bosses.get(id);
    }

    /**
     * サーバーを介さず BossInstance を取得する。
     *
     * BossDeathDetector から呼ばれる。
     * サーバーが未起動なら null。
     */
    public static BossInstance getById(UUID id) {
        MinecraftServer server = net.minecraftforge.server.ServerLifecycleHooks.getCurrentServer();
        if (server == null) return null;
        return get(server, id);
    }

    // =========================================================
    // ダメージ
    // =========================================================

    public static void onCoreHurt(
            ServerLevel level,
            UUID bossId,
            DamageSource source,
            float amount,
            BossCoreEntity core
    ) {
        BossData data = BossData.get(level.getServer());
        BossInstance inst = data.bosses.get(bossId);
        if (inst == null) return;

        if (inst.invulnerableTicks > 0) return;
        inst.invulnerableTicks = INVULNERABLE_TICKS;

        inst.hp = Math.max(0.0D, inst.hp - amount);
        data.setDirty();

        inst.recentDamage += amount;
        inst.totalDamage += amount;

        if (source.getEntity() instanceof net.minecraft.world.entity.player.Player p) {
            inst.damageByPlayer.merge(p.getUUID(), (double) amount, Double::sum);
        }

        data.setDirty();

        if (inst.hp <= 0.0D) {
            destroyBoss(level.getServer(), bossId);
        } else {
            updateBossBar(inst);
        }
    }

    private static void updateBossBar(BossInstance inst) {
        if (inst.bossBar == null) return;
        if (inst.maxHp <= 0.0D) {
            inst.bossBar.setProgress(0.0F);
            return;
        }
        float progress = (float) Math.max(0.0D, Math.min(1.0D, inst.hp / inst.maxHp));
        inst.bossBar.setProgress(progress);
    }

    // =========================================================
    // tick
    // =========================================================

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;

        MinecraftServer server = event.getServer();
        if (server == null) return;

        BossData data = BossData.get(server);

        for (BossInstance inst : data.bosses.values()) {
            tickBoss(server, inst);
        }

        if (server.getTickCount() % SYNC_INTERVAL == 0) {
            broadcastSync(server);
        }
    }

    private static void tickBoss(MinecraftServer server, BossInstance inst) {

        if (inst.bossBar != null) {
            for (ServerPlayer p : server.getPlayerList().getPlayers()) {
                if (!inst.bossBar.getPlayers().contains(p)) {
                    inst.bossBar.addPlayer(p);
                }
            }
        }

        if (inst.invulnerableTicks > 0) inst.invulnerableTicks--;

        // =========================================================
        // 即死記録の寿命を減らす（DPSと同じ仕組み）
        // =========================================================
        if (inst.deathRecord != null && inst.deathRecord.displayTicks > 0) {
            inst.deathRecord.displayTicks--;

            if (inst.deathRecord.displayTicks <= 0) {
                // 表示時間終了 → リセット（ただし履歴の最後のtypeは保持してもいい）
                // ここでは完全リセットせず、displayTicks 0 で「非表示」状態にする
            }
        }

        // DPS
        inst.dpsTickCounter++;
        if (inst.dpsTickCounter >= 20) {
            inst.dps = inst.recentDamage;
            inst.recentDamage = 0.0D;
            inst.dpsTickCounter = 0;
            updateBossBarText(inst);
        }

        updateBossBar(inst);

        ServerLevel level = findLevel(server, inst);
        if (level == null) return;

        BossCoreEntity core = findCore(level, inst);

        if (core == null) {
            spawnCore(level, inst);
            return;
        }

        core.setPos(inst.x, inst.y, inst.z);
        core.setYRot(inst.yaw);
        core.setDeltaMovement(0, 0, 0);
        core.fallDistance = 0.0F;

        checkPlayerAttack(level, inst, core);
    }

    private static ServerLevel findLevel(MinecraftServer server, BossInstance inst) {
        return server.overworld();
    }

    private static BossCoreEntity findCore(ServerLevel level, BossInstance inst) {
        UUID coreId = CORE_IDS.get(inst.id);
        if (coreId == null) return null;
        Entity e = level.getEntity(coreId);
        if (e instanceof BossCoreEntity core && !core.isRemoved()) {
            return core;
        }
        return null;
    }

    // =========================================================
    // 攻撃判定
    // =========================================================

    private static void checkPlayerAttack(ServerLevel level, BossInstance inst, BossCoreEntity core) {
        AABB area = new AABB(
                inst.x - 4.0D, inst.y - 1.0D, inst.z - 4.0D,
                inst.x + 4.0D, inst.y + 3.0D, inst.z + 4.0D
        );

        for (ServerPlayer p : level.getEntitiesOfClass(ServerPlayer.class, area)) {
            if (p.attackAnim <= 0.0F && p.attackAnim > 0.6F) continue;

            float prevAnim = p.oAttackAnim;
            float curAnim = p.attackAnim;
            boolean justSwung = prevAnim <= 0.0F && curAnim > 0.0F;
            if (!justSwung) continue;

            Vec3 eye = p.getEyePosition();
            Vec3 look = p.getLookAngle().normalize();

            if (!rayIntersectsCylinder(eye, look, inst.x, inst.y, inst.z, HIT_RADIUS, HIT_HEIGHT)) {
                continue;
            }

            if (p.distanceToSqr(inst.x, inst.y, inst.z) > 16.0D) continue;

            float dmg = (float) p.getAttributeValue(net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_DAMAGE);

            onCoreHurt(level, inst.id, level.damageSources().playerAttack(p), dmg, core);
        }
    }

    private static boolean rayIntersectsCylinder(
            Vec3 origin, Vec3 dir,
            double cx, double by, double cz,
            double r, double h
    ) {
        double ox = origin.x - cx;
        double oz = origin.z - cz;

        double a = dir.x * dir.x + dir.z * dir.z;
        double b = 2.0D * (ox * dir.x + oz * dir.z);
        double c = ox * ox + oz * oz - r * r;

        double disc = b * b - 4.0D * a * c;
        if (disc < 0.0D) return false;

        double sq = Math.sqrt(disc);
        double t1 = (-b - sq) / (2.0D * a);
        double t2 = (-b + sq) / (2.0D * a);

        double t = -1.0D;
        if (t1 >= 0.0D) t = t1;
        else if (t2 >= 0.0D) t = t2;
        if (t < 0.0D) return false;

        double y = origin.y + dir.y * t;
        return y >= by && y <= by + h;
    }

    // =========================================================
    // 同期
    // =========================================================

    public static void broadcastSync(MinecraftServer server) {
        List<BossInstance> all = getAll(server);
        BossSyncPacket packet = BossSyncPacket.of(all);

        for (ServerPlayer p : server.getPlayerList().getPlayers()) {
            BossSyncPacket.sendTo(p, packet);
        }
    }

    @SubscribeEvent
    public static void onPlayerLogin(net.minecraftforge.event.entity.player.PlayerEvent.PlayerLoggedInEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer p)) return;

        MinecraftServer server = p.getServer();
        if (server == null) return;

        for (BossInstance inst : getAll(server)) {
            if (inst.bossBar != null) {
                inst.bossBar.addPlayer(p);
            }
        }

        BossSyncPacket.sendTo(p, BossSyncPacket.of(getAll(server)));
    }

    @SubscribeEvent
    public static void onPlayerLogout(net.minecraftforge.event.entity.player.PlayerEvent.PlayerLoggedOutEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer p)) return;

        MinecraftServer server = p.getServer();
        if (server == null) return;

        for (BossInstance inst : getAll(server)) {
            if (inst.bossBar != null) {
                inst.bossBar.removePlayer(p);
            }
        }
    }

    // =========================================================
    // ボスバーテキスト
    // =========================================================

    /**
     * ボスバーの名前を更新する。
     *
     * 表示形式:
     *   ??? §7(DPS: §f123.4§7) §c[即死: setHealth(0) §e@ external_mod§c]
     */
    private static void updateBossBarText(BossInstance inst) {
        if (inst.bossBar == null) return;

        StringBuilder text = new StringBuilder(inst.displayName);

        // DPS 部分
        text.append(" §7(DPS: §f")
                .append(String.format("%.1f", inst.dps))
                .append("§7)");

        // 即死情報（表示中のみ）
        if (inst.deathRecord != null && inst.deathRecord.isVisible()) {
            text.append(" ").append(inst.deathRecord.buildDisplay());
        }

        inst.bossBar.setName(net.minecraft.network.chat.Component.literal(text.toString()));
    }
}