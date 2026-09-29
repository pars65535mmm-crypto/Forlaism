package com.tyami.forlaism.world;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.world.BossEvent;
import net.minecraft.world.phys.Vec3;

import java.util.UUID;

/**
 * ボス1体分のランタイムデータ。
 */
public class BossInstance {

    /** ボスのUUID（BossCoreEntity とは別物）。 */
    public final UUID id;

    /** 表示名。 */
    public String displayName;

    /** スキンID（assets/forlaism/textures/entity/<skinId>.png）。 */
    public String skinId;

    /** 現在座標。 */
    public double x, y, z;

    /** Yaw（見た目の向き）。 */
    public float yaw;

    /** 仮想的なHP（実質無限）。 */
    public double hp;

    /** 最大HP。 */
    public double maxHp;

    /** 無敵時間（tick）。 */
    public int invulnerableTicks;

    /** 判定役 Entity のUUID（再召喚のため保持）。 */
    public UUID coreEntityId;

    // =========================================================
    // DPS計測
    // =========================================================

    /** 直近1秒間の累計ダメージ。 */
    public double recentDamage = 0.0D;

    /** 直近1秒のDPS（毎秒更新）。 */
    public double dps = 0.0D;

    /** 累計ダメージ。 */
    public double totalDamage = 0.0D;

    /** DPS計測のリセット用tickカウンタ。 */
    public int dpsTickCounter = 0;

    /** このボスに与えたプレイヤーのUUIDごとの累計ダメージ。 */
    public final java.util.Map<UUID, Double> damageByPlayer = new java.util.HashMap<>();

    /** ボスバー。 */
    public transient ServerBossEvent bossBar;

    // =========================================================
    // 即死記録
    // =========================================================

    /** 直近の即死記録。DPSと同じく時間で消える。 */
    public BossDeathRecord deathRecord = new BossDeathRecord();

    public BossInstance(UUID id, String displayName, String skinId, double x, double y, double z) {
        this.id = id;
        this.displayName = displayName;
        this.skinId = skinId;
        this.x = x;
        this.y = y;
        this.z = z;
        this.yaw = 0.0F;
        this.hp = 1.0e308;
        this.maxHp = 1.0e308;
        this.invulnerableTicks = 0;
    }

    public Vec3 pos() {
        return new Vec3(x, y, z);
    }

    public void setPos(double x, double y, double z) {
        this.x = x;
        this.y = y;
        this.z = z;
    }

    // =========================================================
    // ボスバー
    // =========================================================

    public ServerBossEvent createBossBar() {
        ServerBossEvent bar = new ServerBossEvent(
                net.minecraft.network.chat.Component.literal(displayName),
                BossEvent.BossBarColor.PURPLE,
                BossEvent.BossBarOverlay.PROGRESS
        );
        bar.setProgress(1.0F);
        return bar;
    }

    // =========================================================
    // NBT
    // =========================================================

    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        tag.putUUID("Id", id);
        tag.putString("DisplayName", displayName);
        tag.putString("SkinId", skinId);
        tag.putDouble("X", x);
        tag.putDouble("Y", y);
        tag.putDouble("Z", z);
        tag.putFloat("Yaw", yaw);
        tag.putDouble("Hp", hp);
        tag.putDouble("MaxHp", maxHp);
        tag.putDouble("TotalDamage", totalDamage);
        return tag;
    }

    public static BossInstance load(CompoundTag tag) {
        UUID id = tag.getUUID("Id");
        String name = tag.getString("DisplayName");
        String skin = tag.getString("SkinId");
        double x = tag.getDouble("X");
        double y = tag.getDouble("Y");
        double z = tag.getDouble("Z");
        BossInstance inst = new BossInstance(id, name, skin, x, y, z);
        inst.yaw = tag.getFloat("Yaw");
        inst.hp = tag.getDouble("Hp");
        inst.maxHp = tag.getDouble("MaxHp");
        inst.totalDamage = tag.getDouble("TotalDamage");
        return inst;
    }
}