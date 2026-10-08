package com.tyami.forlaism.entity;

import com.tyami.forlaism.registry.Items;
import com.tyami.forlaism.registry.ModEntityTypes;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ThrowableItemProjectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.NetworkHooks;

import javax.annotation.Nullable;

/**
 * 投げナイフのエンティティ。
 *
 * - 飛行中は Z 軸回転（Renderer側で描画）
 * - 敵ヒット → その場にドロップ
 * - ブロックヒット → 刺さったまま（スタック状態）、近づくと回収
 */
public class IinekoreKnifeEntity extends ThrowableItemProjectile {

    /** 刺さっているかどうか。 */
    private static final EntityDataAccessor<Boolean> DATA_STUCK =
            SynchedEntityData.defineId(IinekoreKnifeEntity.class, EntityDataSerializers.BOOLEAN);

    /** 拾える距離（プレイヤーとナイフの距離）。 */
    private static final double PICKUP_RADIUS = 1.5D;

    /** 投擲ダメージ（Item側で設定される）。 */
    private float damage = 6.0F;

    /** 刺さったtick数（寿命用）。 */
    private int stuckTicks = 0;

    /** 刺さった時の向き（保存用）。 */
    private float stuckYaw = 0.0F;
    private float stuckPitch = 0.0F;

    public IinekoreKnifeEntity(EntityType<? extends IinekoreKnifeEntity> type, Level level) {
        super(type, level);
    }

    public IinekoreKnifeEntity(Level level, LivingEntity shooter) {
        super(ModEntityTypes.IINEKORE_KNIFE.get(), shooter, level);
    }

    @Override
    protected Item getDefaultItem() {
        return Items.IINEKORE_KNIFE.get();
    }

    public void setDamage(float damage) {
        this.damage = damage;
    }

    public float getDamage() {
        return damage;
    }

    // =========================================================
    // 同期データ
    // =========================================================

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(DATA_STUCK, false);
    }

    public boolean isStuck() {
        return this.entityData.get(DATA_STUCK);
    }

    private void setStuck(boolean stuck) {
        this.entityData.set(DATA_STUCK, stuck);
    }

    // =========================================================
    // tick
    // =========================================================

    @Override
    public void tick() {
        // 刺さってる時は tick を軽くする
        if (isStuck()) {
            stuckTicks++;

            // 5分で消滅（矢と同じ）
            if (stuckTicks > 6000) {
                this.discard();
                return;
            }

            // プレイヤーが近づいたら回収
            if (!this.level().isClientSide) {
                this.level().getEntitiesOfClass(
                        Player.class,
                        this.getBoundingBox().inflate(PICKUP_RADIUS)
                ).forEach(p -> tryPickup(p));
            }

            return;
        }

        super.tick();

        // 寿命
        if (this.tickCount > 200) {
            this.discard();
        }
    }

    /**
     * プレイヤーが拾えるか試みる。
     */
    private void tryPickup(Player player) {
        if (player.isSpectator()) return;

        ItemStack pickup = new ItemStack(Items.IINEKORE_KNIFE.get());

        if (player.getInventory().add(pickup)) {
            // 回収成功
            this.discard();

            this.level().playSound(null,
                    this.getX(), this.getY(), this.getZ(),
                    SoundEvents.ITEM_PICKUP,
                    SoundSource.PLAYERS,
                    0.5F, 1.2F
            );
        }
    }

    // =========================================================
    // 敵ヒット
    // =========================================================

    @Override
    protected void onHitEntity(EntityHitResult result) {
        super.onHitEntity(result);

        if (!(this.level() instanceof ServerLevel sl)) return;

        Entity hit = result.getEntity();
        if (!(hit instanceof LivingEntity target)) return;

        Entity owner = this.getOwner();
        if (owner != null && target.getUUID().equals(owner.getUUID())) return;

        DamageSource source = (owner instanceof Player p)
                ? sl.damageSources().playerAttack(p)
                : sl.damageSources().generic();

        target.invulnerableTime = 0;
        target.hurt(source, damage);

        // その場にドロップ
        ItemStack drop = new ItemStack(Items.IINEKORE_KNIFE.get());
        ItemEntity itemEntity = new ItemEntity(
                sl,
                target.getX(), target.getY() + target.getBbHeight() * 0.5, target.getZ(),
                drop
        );
        itemEntity.setPickUpDelay(10);
        itemEntity.setDeltaMovement(
                (this.random.nextDouble() - 0.5) * 0.2,
                0.2,
                (this.random.nextDouble() - 0.5) * 0.2
        );
        sl.addFreshEntity(itemEntity);

        sl.playSound(null,
                this.getX(), this.getY(), this.getZ(),
                SoundEvents.ARROW_HIT_PLAYER,
                SoundSource.PLAYERS,
                1.0F, 1.4F
        );

        this.discard();
    }

    // =========================================================
    // ブロックヒット：刺さる
    // =========================================================

    @Override
    protected void onHitBlock(BlockHitResult result) {
        super.onHitBlock(result);

        if (this.level().isClientSide) {
            this.setStuck(true);
            return;
        }

        // 位置を固定
        Vec3 hitLoc = result.getLocation();
        this.setPos(hitLoc.x, hitLoc.y, hitLoc.z);

        // 向きを保存（刺さった見た目用）
        this.stuckYaw = this.getYRot();
        this.stuckPitch = this.getXRot();

        // 刺さり状態
        this.setDeltaMovement(Vec3.ZERO);
        this.setStuck(true);
        this.noPhysics = true;

        // 刺さった瞬間の音
        this.level().playSound(null,
                this.getX(), this.getY(), this.getZ(),
                SoundEvents.TRIDENT_HIT_GROUND,
                SoundSource.PLAYERS,
                1.0F, 1.2F
        );

        // ちょいパーティクル
        if (this.level() instanceof ServerLevel sl) {
            sl.sendParticles(
                    ParticleTypes.CRIT,
                    this.getX(), this.getY(), this.getZ(),
                    6, 0.1, 0.1, 0.1, 0.05
            );
        }
    }

    // =========================================================
    // 刺さった後は重力なし・動かない
    // =========================================================

    @Override
    protected float getGravity() {
        return isStuck() ? 0.0F : super.getGravity();
    }

    // =========================================================
    // 拾えるか（矢と同じく、刺さってる時のみ有効）
    // =========================================================

    @Override
    public boolean isPickable() {
        return true;
    }

    @Override
    public boolean canBeCollidedWith() {
        return isStuck();
    }

    // =========================================================
    // 同期パケット
    // =========================================================

    @Override
    public Packet<ClientGamePacketListener> getAddEntityPacket() {
        return NetworkHooks.getEntitySpawningPacket(this);
    }

    // =========================================================
    // NBT
    // =========================================================

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putFloat("Damage", damage);
        tag.putBoolean("Stuck", isStuck());
        tag.putInt("StuckTicks", stuckTicks);
        tag.putFloat("StuckYaw", stuckYaw);
        tag.putFloat("StuckPitch", stuckPitch);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        if (tag.contains("Damage")) damage = tag.getFloat("Damage");
        if (tag.contains("Stuck")) setStuck(tag.getBoolean("Stuck"));
        stuckTicks = tag.getInt("StuckTicks");
        stuckYaw = tag.getFloat("StuckYaw");
        stuckPitch = tag.getFloat("StuckPitch");
    }

    // =========================================================
    // getter（Renderer用）
    // =========================================================

    public float getStuckYaw() {
        return stuckYaw;
    }

    public float getStuckPitch() {
        return stuckPitch;
    }
}