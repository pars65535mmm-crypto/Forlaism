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

import java.util.UUID;

public class FactotumMinionEntity extends Entity {

    public enum State {
        WAITING,
        ATTACKING,
        EXHAUSTED
    }

    private static final EntityDataAccessor<Integer> DATA_STATE =
            SynchedEntityData.defineId(FactotumMinionEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DATA_SLOT_INDEX =
            SynchedEntityData.defineId(FactotumMinionEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DATA_TOTAL_SLOTS =
            SynchedEntityData.defineId(FactotumMinionEntity.class, EntityDataSerializers.INT);

    private UUID ownerUUID;
    private UUID targetUUID;
    private int attackDelay = 0;
    private int lifeTicks = 0;
    private static final int MAX_LIFE_TICKS = 1200; // 60秒待機で消滅

    public FactotumMinionEntity(EntityType<?> entityType, Level level) {
        super(entityType, level);
        this.noPhysics = true;
    }

    public FactotumMinionEntity(Level level, Player owner, int slotIndex, int totalSlots) {
        this(ModEntityTypes.FACTOTUM_MINION.get(), level);
        this.ownerUUID = owner.getUUID();
        this.entityData.set(DATA_SLOT_INDEX, slotIndex);
        this.entityData.set(DATA_TOTAL_SLOTS, totalSlots);
        this.entityData.set(DATA_STATE, State.WAITING.ordinal());

        Vec3 spawnPos = calculateWaitingPosition(owner, slotIndex, totalSlots);
        setPos(spawnPos.x, spawnPos.y, spawnPos.z);
    }

    @Override
    protected void defineSynchedData() {
        this.entityData.define(DATA_STATE, State.WAITING.ordinal());
        this.entityData.define(DATA_SLOT_INDEX, 0);
        this.entityData.define(DATA_TOTAL_SLOTS, 7);
    }

    public State getMinionState() {
        return State.values()[this.entityData.get(DATA_STATE)];
    }

    public void setMinionState(State state) {
        this.entityData.set(DATA_STATE, state.ordinal());
    }

    public int getSlotIndex() {
        return this.entityData.get(DATA_SLOT_INDEX);
    }

    public int getTotalSlots() {
        return this.entityData.get(DATA_TOTAL_SLOTS);
    }

    public void commandAttack(LivingEntity target, int staggerDelay) {
        this.targetUUID = target != null ? target.getUUID() : null;
        this.attackDelay = staggerDelay;
        setMinionState(State.ATTACKING);
    }

    @Override
    public void tick() {
        super.tick();
        this.lifeTicks++;

        if (level().isClientSide) {
            return;
        }

        Player owner = getOwnerPlayer();
        if (owner == null || !owner.isAlive() || this.lifeTicks > MAX_LIFE_TICKS) {
            this.discard();
            return;
        }

        State state = getMinionState();
        if (state == State.WAITING) {
            tickWaiting(owner);
        } else if (state == State.ATTACKING) {
            tickAttacking(owner);
        } else if (state == State.EXHAUSTED) {
            this.discard();
        }
    }

    private void tickWaiting(Player owner) {
        Vec3 targetPos = calculateWaitingPosition(owner, getSlotIndex(), getTotalSlots());
        Vec3 currentPos = position();

        // スムーズに目標待機位置へ補間追従
        Vec3 move = targetPos.subtract(currentPos).scale(0.35);
        setPos(currentPos.add(move));

        this.setYRot(owner.getYRot());
        this.setXRot(owner.getXRot());
    }

    private void tickAttacking(Player owner) {
        if (this.attackDelay > 0) {
            this.attackDelay--;
            tickWaiting(owner);
            return;
        }

        LivingEntity target = getTargetEntity();
        if (target != null && target.isAlive()) {
            double dist = position().distanceTo(target.position());

            // 100ブロック以上離れている場合、ターゲットの背後へテレポート
            if (dist > 100.0) {
                Vec3 behind = target.position().subtract(target.getLookAngle().scale(3.0)).add(0, 1.5, 0);
                setPos(behind.x, behind.y, behind.z);
            }

            Vec3 targetEye = target.getEyePosition();
            Vec3 dir = targetEye.subtract(position());
            double currentDist = dir.length();

            if (currentDist < 1.5) {
                // ヒット！独自ダメージ付与
                FactotumDamage.dealDamage(target, owner, this, 20.0F);
                setMinionState(State.EXHAUSTED);
                this.discard();
            } else {
                Vec3 velocity = dir.normalize().scale(2.2);
                setPos(position().add(velocity));
                setDeltaMovement(velocity);

                float yaw = (float) (Math.atan2(-velocity.x, velocity.z) * (180.0 / Math.PI));
                float pitch = (float) (Math.asin(-velocity.y / velocity.length()) * (180.0 / Math.PI));
                this.setYRot(yaw);
                this.setXRot(pitch);
            }
        } else {
            // 敵がいない場合は視線方向へ高速突進して消滅
            Vec3 look = owner.getLookAngle();
            Vec3 next = position().add(look.scale(2.0));
            setPos(next.x, next.y, next.z);
            if (this.lifeTicks % 20 == 0) {
                this.discard();
            }
        }
    }

    private Vec3 calculateWaitingPosition(Player owner, int index, int total) {
        float yawRad = (float) Math.toRadians(owner.getYRot());
        Vec3 forward = new Vec3(-Math.sin(yawRad), 0, Math.cos(yawRad));
        Vec3 right = new Vec3(forward.z, 0, -forward.x);
        Vec3 up = new Vec3(0, 1, 0);

        // 扇状・翼状のオフセット計算
        // index 0 が最上部または中心、交互に左右に展開
        float spread = (total > 1) ? ((float) index / (float) (total - 1) - 0.5F) * 2.0F : 0.0F;
        double sideOffset = spread * 1.5;
        double heightOffset = (1.0 - Math.abs(spread) * 0.4) * 0.8;
        double backOffset = -1.2 - Math.abs(spread) * 0.3;

        return owner.position().add(0, owner.getEyeHeight() * 0.8, 0)
                .add(forward.scale(backOffset))
                .add(right.scale(sideOffset))
                .add(up.scale(heightOffset));
    }

    private LivingEntity getTargetEntity() {
        if (this.targetUUID != null && level() instanceof ServerLevel serverLevel) {
            Entity entity = serverLevel.getEntity(this.targetUUID);
            if (entity instanceof LivingEntity living) {
                return living;
            }
        }
        return null;
    }

    private Player getOwnerPlayer() {
        if (ownerUUID != null && level() instanceof ServerLevel serverLevel) {
            return serverLevel.getPlayerByUUID(ownerUUID);
        }
        return null;
    }

    public UUID getOwnerUUID() {
        return this.ownerUUID;
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        if (tag.hasUUID("Owner")) {
            this.ownerUUID = tag.getUUID("Owner");
        }
        if (tag.hasUUID("Target")) {
            this.targetUUID = tag.getUUID("Target");
        }
        this.lifeTicks = tag.getInt("LifeTicks");
        this.attackDelay = tag.getInt("AttackDelay");
        if (tag.contains("SlotIndex")) {
            this.entityData.set(DATA_SLOT_INDEX, tag.getInt("SlotIndex"));
        }
        if (tag.contains("TotalSlots")) {
            this.entityData.set(DATA_TOTAL_SLOTS, tag.getInt("TotalSlots"));
        }
        if (tag.contains("State")) {
            this.entityData.set(DATA_STATE, tag.getInt("State"));
        }
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        if (this.ownerUUID != null) {
            tag.putUUID("Owner", this.ownerUUID);
        }
        if (this.targetUUID != null) {
            tag.putUUID("Target", this.targetUUID);
        }
        tag.putInt("LifeTicks", this.lifeTicks);
        tag.putInt("AttackDelay", this.attackDelay);
        tag.putInt("SlotIndex", getSlotIndex());
        tag.putInt("TotalSlots", getTotalSlots());
        tag.putInt("State", getMinionState().ordinal());
    }

    @Override
    public Packet<ClientGamePacketListener> getAddEntityPacket() {
        return NetworkHooks.getEntitySpawningPacket(this);
    }
}
