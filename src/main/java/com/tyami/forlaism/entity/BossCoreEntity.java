package com.tyami.forlaism.entity;

import com.tyami.forlaism.world.BossDeathDetector;
import com.tyami.forlaism.world.BossDeathRecord;
import com.tyami.forlaism.world.BossManager;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.Level;

import java.util.UUID;

/**
 * ボスの「判定役」Entity。
 *
 * サンドバックとしても機能する。
 * 即死攻撃を受けたら BossDeathDetector に通知してボスバーに表示する。
 *
 * setHealth / setRemoved / discard は Entity / LivingEntity 側で final なので、
 * Mixin (BossCoreDetectorMixin) で検出する。
 */
public class BossCoreEntity extends PathfinderMob {

    private static final EntityDataAccessor<String> DATA_BOSS_ID =
            SynchedEntityData.defineId(BossCoreEntity.class, EntityDataSerializers.STRING);

    private static final EntityDataAccessor<String> DATA_DISPLAY_NAME =
            SynchedEntityData.defineId(BossCoreEntity.class, EntityDataSerializers.STRING);

    private static final EntityDataAccessor<String> DATA_SKIN_ID =
            SynchedEntityData.defineId(BossCoreEntity.class, EntityDataSerializers.STRING);

    public BossCoreEntity(EntityType<? extends PathfinderMob> type, Level level) {
        super(type, level);
        this.setNoAi(true);
        this.setNoGravity(true);
        this.setInvulnerable(false);
        this.setPersistenceRequired();
        this.noPhysics = true;
        this.setSilent(true);
        this.knockback(0.0, 0.0, 0.0);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 1.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.0D)
                .add(Attributes.KNOCKBACK_RESISTANCE, 1.0D);
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(DATA_BOSS_ID, "");
        this.entityData.define(DATA_DISPLAY_NAME, "???");
        this.entityData.define(DATA_SKIN_ID, "kumorizoraneko");
    }

    // =========================================================
    // 同期データ
    // =========================================================

    public UUID getBossId() {
        String s = this.entityData.get(DATA_BOSS_ID);
        if (s.isEmpty()) return null;
        try {
            return UUID.fromString(s);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    public void setBossId(UUID id) {
        this.entityData.set(DATA_BOSS_ID, id == null ? "" : id.toString());
    }

    public String getBossDisplayName() {
        return this.entityData.get(DATA_DISPLAY_NAME);
    }

    public void setBossDisplayName(String name) {
        this.entityData.set(DATA_DISPLAY_NAME, name);
    }

    public String getSkinId() {
        return this.entityData.get(DATA_SKIN_ID);
    }

    public void setSkinId(String id) {
        this.entityData.set(DATA_SKIN_ID, id);
    }

    // =========================================================
    // 動作
    // =========================================================

    @Override
    public void tick() {
        this.setDeltaMovement(0, 0, 0);
        this.fallDistance = 0.0F;
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    protected void doPush(Entity entity) {
    }

    @Override
    public void push(double x, double y, double z) {
    }

    @Override
    public boolean canBeCollidedWith() {
        return true;
    }

    @Override
    public boolean isPickable() {
        return true;
    }

    @Override
    public boolean isAttackable() {
        return true;
    }

    // =========================================================
    // 即死検出（override 可能なもののみ）
    // =========================================================

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (this.level().isClientSide) return false;

        UUID bossId = getBossId();

        if (bossId != null) {
            if (amount >= Float.MAX_VALUE * 0.9F) {
                BossDeathDetector.detect(
                        bossId,
                        BossDeathRecord.Type.HURT_MAX_VALUE,
                        "amount=" + amount
                );
            } else if (amount > 1000.0F) {
                BossDeathDetector.detect(
                        bossId,
                        BossDeathRecord.Type.HURT_MAX_VALUE,
                        "amount=" + amount
                );
            } else {
                BossManager.onCoreHurt(
                        (ServerLevel) this.level(),
                        bossId,
                        source,
                        amount,
                        this
                );
            }
        }

        return true;
    }

    @Override
    public void die(DamageSource source) {
        UUID bossId = getBossId();
        if (bossId != null) {
            BossDeathDetector.detect(
                    bossId,
                    BossDeathRecord.Type.DIE_CALL,
                    "source=" + source.getMsgId()
            );
        }
        // 死なない
    }

    @Override
    public void kill() {
        UUID bossId = getBossId();
        if (bossId != null) {
            BossDeathDetector.detect(
                    bossId,
                    BossDeathRecord.Type.KILL_CALL,
                    ""
            );
        }
        // /kill 無効
    }

    @Override
    public void remove(RemovalReason reason) {
        UUID bossId = getBossId();
        if (bossId != null) {
            BossDeathRecord.Type type = switch (reason) {
                case KILLED -> BossDeathRecord.Type.REMOVE_CALL;
                case DISCARDED -> BossDeathRecord.Type.DISCARD_CALL;
                default -> BossDeathRecord.Type.REMOVE_CALL;
            };
            BossDeathDetector.detect(
                    bossId,
                    type,
                    "reason=" + reason.name()
            );
        }

        if (reason == RemovalReason.KILLED || reason == RemovalReason.DISCARDED) {
            return;
        }
        super.remove(reason);
    }

    // =========================================================
    // NBT
    // =========================================================

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        UUID id = getBossId();
        if (id != null) tag.putUUID("BossId", id);
        tag.putString("DisplayName", getBossDisplayName());
        tag.putString("SkinId", getSkinId());
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        if (tag.hasUUID("BossId")) setBossId(tag.getUUID("BossId"));
        if (tag.contains("DisplayName")) setBossDisplayName(tag.getString("DisplayName"));
        if (tag.contains("SkinId")) setSkinId(tag.getString("SkinId"));
    }
}