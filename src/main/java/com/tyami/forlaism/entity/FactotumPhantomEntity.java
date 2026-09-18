package com.tyami.forlaism.entity;

import com.tyami.forlaism.damage.FactotumDamage;
import com.tyami.forlaism.registry.ModEntityTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.TargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public class FactotumPhantomEntity extends PathfinderMob {

    private static final EntityDataAccessor<Optional<UUID>> DATA_OWNER_UUID =
            SynchedEntityData.defineId(FactotumPhantomEntity.class, EntityDataSerializers.OPTIONAL_UUID);

    private static final int LIFETIME_TICKS = 600; // 30秒
    private int lifeTicks = 0;

    public FactotumPhantomEntity(EntityType<? extends PathfinderMob> entityType, Level level) {
        super(entityType, level);
        this.noPhysics = false;
        this.setInvulnerable(true);
    }

    public FactotumPhantomEntity(Level level, Player owner) {
        this(ModEntityTypes.FACTOTUM_PHANTOM.get(), level);
        setOwnerUUID(owner.getUUID());
        setPos(owner.getX() + (random.nextDouble() - 0.5) * 4.0,
                owner.getY(),
                owner.getZ() + (random.nextDouble() - 0.5) * 4.0);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return PathfinderMob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 100.0)
                .add(Attributes.MOVEMENT_SPEED, 0.35)
                .add(Attributes.ATTACK_DAMAGE, 12.0)
                .add(Attributes.FOLLOW_RANGE, 32.0);
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(DATA_OWNER_UUID, Optional.empty());
    }

    public Optional<UUID> getOwnerUUID() {
        return this.entityData.get(DATA_OWNER_UUID);
    }

    public void setOwnerUUID(UUID uuid) {
        this.entityData.set(DATA_OWNER_UUID, Optional.ofNullable(uuid));
    }

    public Player getOwnerPlayer() {
        return getOwnerUUID().map(uuid -> {
            if (level() instanceof ServerLevel serverLevel) {
                return serverLevel.getPlayerByUUID(uuid);
            }
            return level().getPlayerByUUID(uuid);
        }).orElse(null);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(1, new PhantomAttackGoal(this));
        this.goalSelector.addGoal(2, new FollowOwnerGoal(this, 1.2, 5.0F, 2.0F));
        this.goalSelector.addGoal(3, new WaterAvoidingRandomStrollGoal(this, 1.0));
        this.goalSelector.addGoal(4, new LookAtPlayerGoal(this, LivingEntity.class, 8.0F));

        this.targetSelector.addGoal(1, new PhantomOwnerTargetGoal(this));
    }

    @Override
    public void tick() {
        super.tick();
        this.lifeTicks++;

        if (!level().isClientSide) {
            // 30秒で自動消滅
            if (this.lifeTicks >= LIFETIME_TICKS) {
                this.discard();
                return;
            }

            // デコイ機能: 周囲のMobのターゲットを自分に向けさせる
            if (this.lifeTicks % 10 == 0) {
                List<Monster> nearbyMonsters = level().getEntitiesOfClass(Monster.class, this.getBoundingBox().inflate(12.0));
                for (Monster monster : nearbyMonsters) {
                    Player owner = getOwnerPlayer();
                    if (monster.getTarget() == null || (owner != null && monster.getTarget() == owner)) {
                        monster.setTarget(this);
                    }
                }
            }
        }
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        // 幻影は実体を持たず死亡しない
        return false;
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    protected void doPush(net.minecraft.world.entity.Entity entity) {
        // 実体としての衝突なし
    }

    @Override
    public boolean doHurtTarget(net.minecraft.world.entity.Entity entity) {
        if (entity instanceof LivingEntity target) {
            Player owner = getOwnerPlayer();
            // 独自Damage
            return FactotumDamage.dealDamage(target, owner, this, 12.0F);
        }
        return false;
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        getOwnerUUID().ifPresent(uuid -> tag.putUUID("Owner", uuid));
        tag.putInt("LifeTicks", this.lifeTicks);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        if (tag.hasUUID("Owner")) {
            setOwnerUUID(tag.getUUID("Owner"));
        }
        this.lifeTicks = tag.getInt("LifeTicks");
    }

    // 攻撃AIゴール
    private static class PhantomAttackGoal extends MeleeAttackGoal {
        public PhantomAttackGoal(FactotumPhantomEntity phantom) {
            super(phantom, 1.25, true);
        }
    }

    // オーナーのターゲットを狙うゴール
    private static class PhantomOwnerTargetGoal extends TargetGoal {
        private final FactotumPhantomEntity phantom;

        public PhantomOwnerTargetGoal(FactotumPhantomEntity phantom) {
            super(phantom, false);
            this.phantom = phantom;
            this.setFlags(EnumSet.of(Flag.TARGET));
        }

        @Override
        public boolean canUse() {
            Player owner = this.phantom.getOwnerPlayer();
            if (owner == null) return false;

            LivingEntity target = owner.getLastHurtMob();
            if (target != null && target.isAlive() && target != owner) {
                return true;
            }

            LivingEntity attacker = owner.getLastHurtByMob();
            return attacker != null && attacker.isAlive() && attacker != owner;
        }

        @Override
        public void start() {
            Player owner = this.phantom.getOwnerPlayer();
            if (owner != null) {
                LivingEntity target = owner.getLastHurtMob();
                if (target == null || !target.isAlive()) {
                    target = owner.getLastHurtByMob();
                }
                this.phantom.setTarget(target);
            }
            super.start();
        }
    }

    // オーナー追従ゴール
    private static class FollowOwnerGoal extends Goal {
        private final FactotumPhantomEntity phantom;
        private final double speedModifier;
        private final float stopDistance;
        private final float startDistance;

        public FollowOwnerGoal(FactotumPhantomEntity phantom, double speed, float startDist, float stopDist) {
            this.phantom = phantom;
            this.speedModifier = speed;
            this.startDistance = startDist;
            this.stopDistance = stopDist;
            this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            Player owner = this.phantom.getOwnerPlayer();
            return owner != null && !owner.isSpectator() && this.phantom.distanceToSqr(owner) > (double) (this.startDistance * this.startDistance);
        }

        @Override
        public void tick() {
            Player owner = this.phantom.getOwnerPlayer();
            if (owner != null) {
                this.phantom.getLookControl().setLookAt(owner, 10.0F, (float) this.phantom.getMaxHeadXRot());
                if (this.phantom.distanceToSqr(owner) > (double) (this.stopDistance * this.stopDistance)) {
                    this.phantom.getNavigation().moveTo(owner, this.speedModifier);
                }
            }
        }
    }
}
