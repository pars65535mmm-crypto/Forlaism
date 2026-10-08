package com.tyami.forlaism.entity;

import com.tyami.forlaism.damage.FrostbiteExecution;
import com.tyami.forlaism.registry.Items;
import com.tyami.forlaism.registry.ModEntityTypes;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ThrowableItemProjectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.NetworkHooks;

import javax.annotation.Nullable;
import java.util.UUID;

/**
 * フラスラティアの投擲体。
 *
 * 【常時表示・カリング完全無効化版】
 *   noCulling = true
 *   getBoundingBoxForCulling() を巨大に
 *   これで視錐台カリングに絶対に弾かれない。
 */
public class FraslatiaThrowEntity extends ThrowableItemProjectile {

    private static final double SPEED = 1.5D;
    private static final double MAX_RANGE = 40.0D;
    private static final double RETURN_SPEED = 1.8D;
    private static final float HIT_DAMAGE = 50.0F;

    private static final int FREEZE_DURATION = 60;
    private static final int SLOW_DURATION = 160;
    private static final int FROSTBITE_DURATION = 200;

    private Vec3 originPos = Vec3.ZERO;
    private boolean returning = false;

    @Nullable
    private UUID ownerUUID;

    public FraslatiaThrowEntity(EntityType<? extends FraslatiaThrowEntity> type, Level level) {
        super(type, level);
        this.setNoGravity(true);
        // ★ カリング完全無効化
        this.noCulling = true;
    }

    public FraslatiaThrowEntity(Level level, LivingEntity shooter) {
        super(ModEntityTypes.FRASLATIA_THROW.get(), shooter, level);
        this.setNoGravity(true);
        // ★ カリング完全無効化
        this.noCulling = true;
        this.ownerUUID = shooter.getUUID();
        this.originPos = shooter.position();
    }

    /**
     * ★ カリング用BoundingBoxを巨大化
     *   視錐台判定に絶対に引っかからないようにする。
     */
    @Override
    public AABB getBoundingBoxForCulling() {
        return super.getBoundingBox().inflate(64.0D);
    }

    @Override
    protected Item getDefaultItem() {
        return Items.FRASLATIA.get();
    }

    // =========================================================
    // 発射
    // =========================================================

    public void shoot(Vec3 direction) {
        Vec3 d = direction.normalize().scale(SPEED);
        this.setDeltaMovement(d);
        this.hasImpulse = true;
        this.hurtMarked = true;
    }

    // =========================================================
    // tick
    // =========================================================

    @Override
    public void tick() {
        super.tick();

        if (this.isRemoved()) return;

        // ★ 位置を明示的に更新（同期のため）
        if (!this.level().isClientSide) {
            tickMove();
        }

        // パーティクル
        if (this.level() instanceof ServerLevel sl) {
            sl.sendParticles(
                    ParticleTypes.SNOWFLAKE,
                    this.getX(), this.getY(), this.getZ(),
                    4, 0.2, 0.2, 0.2, 0.02
            );
            sl.sendParticles(
                    ParticleTypes.ITEM_SNOWBALL,
                    this.getX(), this.getY(), this.getZ(),
                    2, 0.15, 0.15, 0.15, 0.02
            );
            sl.sendParticles(
                    ParticleTypes.END_ROD,
                    this.getX(), this.getY(), this.getZ(),
                    2, 0.1, 0.1, 0.1, 0.0
            );
        }

        // 帰還中
        if (returning) {
            tickReturn();
            return;
        }

        // 飛翔中の距離チェック
        if (this.originPos.distanceTo(this.position()) > MAX_RANGE) {
            startReturn();
        }
    }

    private void tickMove() {
        this.xo = this.getX();
        this.yo = this.getY();
        this.zo = this.getZ();

        Vec3 v = this.getDeltaMovement();
        Vec3 next = this.position().add(v);

        this.setPos(next.x, next.y, next.z);
        this.hasImpulse = true;
    }

    // =========================================================
    // 帰還処理
    // =========================================================

    private void startReturn() {
        this.returning = true;
        this.setNoGravity(true);

        if (this.level() instanceof ServerLevel sl) {
            sl.sendParticles(
                    ParticleTypes.FLASH,
                    this.getX(), this.getY(), this.getZ(),
                    1, 0, 0, 0, 0
            );
            sl.playSound(null,
                    this.getX(), this.getY(), this.getZ(),
                    SoundEvents.ENDERMAN_TELEPORT,
                    SoundSource.PLAYERS, 0.8F, 1.5F
            );
        }
    }

    private void tickReturn() {
        Entity owner = getOwnerEntity();

        if (!(owner instanceof LivingEntity living) || !living.isAlive()) {
            dropAsItem();
            return;
        }

        Vec3 target = living.getEyePosition();
        Vec3 toTarget = target.subtract(this.position());
        double dist = toTarget.length();

        if (dist < 1.5) {
            giveBackToOwner(living);
            this.discard();
            return;
        }

        Vec3 dir = toTarget.normalize().scale(RETURN_SPEED);
        this.setDeltaMovement(dir);
        this.hasImpulse = true;
        this.hurtMarked = true;

        Vec3 next = this.position().add(dir);
        this.setPos(next.x, next.y, next.z);
        this.hasImpulse = true;

        if (this.level() instanceof ServerLevel sl && this.tickCount % 2 == 0) {
            sl.sendParticles(
                    ParticleTypes.END_ROD,
                    this.getX(), this.getY(), this.getZ(),
                    1, 0.05, 0.05, 0.05, 0.0
            );
        }
    }

    // =========================================================
    // 命中処理
    // =========================================================

    @Override
    protected void onHitEntity(EntityHitResult result) {
        super.onHitEntity(result);
        if (this.level().isClientSide) return;
        if (!(this.level() instanceof ServerLevel sl)) return;
        if (returning) return;

        Entity hit = result.getEntity();
        if (!(hit instanceof LivingEntity target)) return;

        Entity owner = getOwnerEntity();
        if (owner != null && target.getUUID().equals(owner.getUUID())) return;

        applyHitEffects(sl, target, owner);
        startReturn();
    }

    @Override
    protected void onHitBlock(BlockHitResult result) {
        super.onHitBlock(result);
        if (this.level().isClientSide) return;
        if (returning) return;

        startReturn();
    }

    // =========================================================
    // 命中効果
    // =========================================================

    private void applyHitEffects(ServerLevel sl, LivingEntity target, @Nullable Entity owner) {

        target.invulnerableTime = 0;
        target.hurtTime = 0;

        if (owner instanceof Player p) {
            target.hurt(sl.damageSources().playerAttack(p), HIT_DAMAGE);
        } else {
            target.hurt(sl.damageSources().generic(), HIT_DAMAGE);
        }

        target.addEffect(new MobEffectInstance(
                MobEffects.MOVEMENT_SLOWDOWN, FREEZE_DURATION, 3, false, true, true));
        target.addEffect(new MobEffectInstance(
                MobEffects.DIG_SLOWDOWN, FREEZE_DURATION, 3, false, true, true));
        target.addEffect(new MobEffectInstance(
                MobEffects.MOVEMENT_SLOWDOWN, SLOW_DURATION, 1, false, true, true));

        FrostbiteExecution.apply(target, FROSTBITE_DURATION);

        sl.sendParticles(
                ParticleTypes.SNOWFLAKE,
                target.getX(), target.getY() + target.getBbHeight() * 0.5, target.getZ(),
                30, 0.6, 0.6, 0.6, 0.08
        );
        sl.sendParticles(
                ParticleTypes.FLASH,
                target.getX(), target.getY() + 1.0, target.getZ(),
                1, 0, 0, 0, 0
        );
        sl.playSound(null,
                target.getX(), target.getY(), target.getZ(),
                SoundEvents.GLASS_BREAK, SoundSource.PLAYERS, 1.0F, 1.5F);
        sl.playSound(null,
                target.getX(), target.getY(), target.getZ(),
                SoundEvents.PLAYER_HURT_FREEZE, SoundSource.PLAYERS, 1.0F, 0.8F);
    }

    // =========================================================
    // 手元に戻す
    // =========================================================

    private void giveBackToOwner(LivingEntity owner) {

        ItemStack stack = new ItemStack(Items.FRASLATIA.get());

        if (owner instanceof Player player) {
            if (!player.getInventory().add(stack)) {
                ItemEntity drop = new ItemEntity(
                        player.level(),
                        player.getX(), player.getY() + 0.5, player.getZ(),
                        stack
                );
                drop.setPickUpDelay(0);
                player.level().addFreshEntity(drop);
            }
        } else {
            ItemEntity drop = new ItemEntity(
                    owner.level(),
                    owner.getX(), owner.getY() + 0.5, owner.getZ(),
                    stack
            );
            owner.level().addFreshEntity(drop);
        }

        if (owner.level() instanceof ServerLevel sl) {
            sl.playSound(null,
                    owner.getX(), owner.getY(), owner.getZ(),
                    SoundEvents.ITEM_PICKUP, SoundSource.PLAYERS, 1.0F, 1.5F);
        }
    }

    private void dropAsItem() {
        if (this.level() instanceof ServerLevel sl) {
            ItemStack stack = new ItemStack(Items.FRASLATIA.get());
            ItemEntity drop = new ItemEntity(
                    sl, this.getX(), this.getY(), this.getZ(), stack
            );
            drop.setPickUpDelay(10);
            sl.addFreshEntity(drop);
        }
        this.discard();
    }

    // =========================================================
    // ユーティリティ
    // =========================================================

    @Nullable
    private Entity getOwnerEntity() {
        if (ownerUUID == null) return null;
        if (this.level() instanceof ServerLevel sl) {
            return sl.getEntity(ownerUUID);
        }
        return null;
    }

    // =========================================================
    // 同期
    // =========================================================

    @Override
    public Packet<ClientGamePacketListener> getAddEntityPacket() {
        return NetworkHooks.getEntitySpawningPacket(this);
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        if (ownerUUID != null) tag.putUUID("OwnerUUID", ownerUUID);
        tag.putBoolean("Returning", returning);
        tag.putDouble("OriginX", originPos.x);
        tag.putDouble("OriginY", originPos.y);
        tag.putDouble("OriginZ", originPos.z);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        if (tag.hasUUID("OwnerUUID")) ownerUUID = tag.getUUID("OwnerUUID");
        returning = tag.getBoolean("Returning");
        originPos = new Vec3(
                tag.getDouble("OriginX"),
                tag.getDouble("OriginY"),
                tag.getDouble("OriginZ")
        );
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
    }
}