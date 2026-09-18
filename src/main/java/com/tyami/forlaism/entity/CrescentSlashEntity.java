package com.tyami.forlaism.entity;

import com.tyami.forlaism.damage.FactotumDamage;
import com.tyami.forlaism.registry.ModEntityTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.NetworkHooks;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public class CrescentSlashEntity extends Entity {

    private static final EntityDataAccessor<Float> DATA_YAW =
            SynchedEntityData.defineId(CrescentSlashEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> DATA_PITCH =
            SynchedEntityData.defineId(CrescentSlashEntity.class, EntityDataSerializers.FLOAT);

    private static final float SPEED = 1.8F;
    private static final float MAX_DISTANCE = 100.0F;

    private UUID ownerUUID;
    private Vec3 startPos;
    private Vec3 shootDirection;
    private final Set<Integer> hitEntityIds = new HashSet<>();

    public CrescentSlashEntity(EntityType<?> entityType, Level level) {
        super(entityType, level);
        this.noPhysics = true;
    }

    public CrescentSlashEntity(Level level, Player owner, Vec3 direction) {
        this(ModEntityTypes.CRESCENT_SLASH.get(), level);
        this.ownerUUID = owner.getUUID();
        this.startPos = owner.getEyePosition();
        this.shootDirection = direction.normalize();

        setPos(this.startPos.x, this.startPos.y, this.startPos.z);
        setDeltaMovement(this.shootDirection.scale(SPEED));

        float yaw = (float) (Math.atan2(-this.shootDirection.x, this.shootDirection.z) * (180.0 / Math.PI));
        float pitch = (float) (Math.asin(-this.shootDirection.y) * (180.0 / Math.PI));
        this.entityData.set(DATA_YAW, yaw);
        this.entityData.set(DATA_PITCH, pitch);
        this.setYRot(yaw);
        this.setXRot(pitch);
    }

    @Override
    protected void defineSynchedData() {
        this.entityData.define(DATA_YAW, 0.0F);
        this.entityData.define(DATA_PITCH, 0.0F);
    }

    public float getSyncedYaw() {
        return this.entityData.get(DATA_YAW);
    }

    public float getSyncedPitch() {
        return this.entityData.get(DATA_PITCH);
    }

    @Override
    public void tick() {
        super.tick();

        if (this.startPos == null) {
            this.startPos = position();
        }

        if (this.shootDirection == null) {
            Vec3 delta = getDeltaMovement();
            if (delta.lengthSqr() > 0.0001) {
                this.shootDirection = delta.normalize();
            } else {
                this.shootDirection = new Vec3(0, 0, 1);
            }
        }

        // 位置の更新
        Vec3 currentPos = position();
        Vec3 nextPos = currentPos.add(this.shootDirection.scale(SPEED));
        setPos(nextPos.x, nextPos.y, nextPos.z);

        if (!level().isClientSide) {
            // 飛行距離のチェック (最大100ブロック)
            if (currentPos.distanceTo(this.startPos) >= MAX_DISTANCE || this.tickCount > 100) {
                this.discard();
                return;
            }

            // 当たり判定
            AABB hitBox = this.getBoundingBox().inflate(1.2, 0.8, 1.2);
            List<LivingEntity> targets = level().getEntitiesOfClass(LivingEntity.class, hitBox,
                    e -> e.isAlive() && !e.isSpectator() && (ownerUUID == null || !e.getUUID().equals(ownerUUID)));

            Player owner = getOwnerPlayer();
            for (LivingEntity target : targets) {
                if (!hitEntityIds.contains(target.getId())) {
                    hitEntityIds.add(target.getId());
                    // 独自ダメージを付与 (25.0F の高威力ダメージ)
                    FactotumDamage.dealDamage(target, owner, this, 25.0F);
                }
            }
        }
    }

    private Player getOwnerPlayer() {
        if (ownerUUID != null && level() instanceof ServerLevel serverLevel) {
            return serverLevel.getPlayerByUUID(ownerUUID);
        }
        return null;
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        if (tag.hasUUID("Owner")) {
            this.ownerUUID = tag.getUUID("Owner");
        }
        if (tag.contains("StartX")) {
            this.startPos = new Vec3(tag.getDouble("StartX"), tag.getDouble("StartY"), tag.getDouble("StartZ"));
        }
        if (tag.contains("DirX")) {
            this.shootDirection = new Vec3(tag.getDouble("DirX"), tag.getDouble("DirY"), tag.getDouble("DirZ"));
        }
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        if (this.ownerUUID != null) {
            tag.putUUID("Owner", this.ownerUUID);
        }
        if (this.startPos != null) {
            tag.putDouble("StartX", this.startPos.x);
            tag.putDouble("StartY", this.startPos.y);
            tag.putDouble("StartZ", this.startPos.z);
        }
        if (this.shootDirection != null) {
            tag.putDouble("DirX", this.shootDirection.x);
            tag.putDouble("DirY", this.shootDirection.y);
            tag.putDouble("DirZ", this.shootDirection.z);
        }
    }

    @Override
    public Packet<ClientGamePacketListener> getAddEntityPacket() {
        return NetworkHooks.getEntitySpawningPacket(this);
    }
}
