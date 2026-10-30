package com.tyami.forlaism.entity;

import com.tyami.forlaism.damage.RinneDamageSource;
import com.tyami.forlaism.registry.ModEntityTypes;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.ThrowableItemProjectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.NetworkHooks;

import javax.annotation.Nullable;
import java.util.UUID;

/**
 * 射出型の星（門星/星々で使用）。
 *
 * - 直進、重力なし
 * - 命中時: 輪廻ダメージ30 + 流星召喚
 * - 一定寿命で消滅
 */
public class FlyingStarEntity extends ThrowableItemProjectile {

    public static final float HIT_DAMAGE = 30.0F;
    private static final int DEFAULT_LIFE = 60;

    @Nullable
    private UUID ownerUUID;

    private int life = DEFAULT_LIFE;
    private float damage = HIT_DAMAGE;

    public FlyingStarEntity(EntityType<? extends FlyingStarEntity> type, Level level) {
        super(type, level);
        this.setNoGravity(true);
        this.noCulling = true;
    }

    public FlyingStarEntity(Level level, LivingEntity shooter) {
        super(ModEntityTypes.FLYING_STAR.get(), shooter, level);
        this.setNoGravity(true);
        this.noCulling = true;
        this.ownerUUID = shooter.getUUID();
    }

    @Override
    protected Item getDefaultItem() {
        return Items.AIR;
    }

    public void shoot(Vec3 dir, double speed) {
        this.setDeltaMovement(dir.normalize().scale(speed));
        this.hurtMarked = true;
    }

    public void setLife(int life) {
        this.life = life;
    }

    public void setDamage(float damage) {
        this.damage = damage;
    }

    @Override
    public void tick() {
        super.tick();

        if (--life <= 0 || this.isRemoved()) {
            this.discard();
            return;
        }

        if (this.level().isClientSide) {
            this.level().addParticle(
                    ParticleTypes.END_ROD,
                    this.getX(), this.getY(), this.getZ(),
                    0, 0, 0
            );
            this.level().addParticle(
                    ParticleTypes.FIREWORK,
                    this.getX(), this.getY(), this.getZ(),
                    0, 0, 0
            );
        } else if (this.level() instanceof ServerLevel sl) {
            sl.sendParticles(
                    ParticleTypes.END_ROD,
                    this.getX(), this.getY(), this.getZ(),
                    1, 0.05, 0.05, 0.05, 0
            );
            if (this.tickCount % 2 == 0) {
                sl.sendParticles(
                        ParticleTypes.FIREWORK,
                        this.getX(), this.getY(), this.getZ(),
                        1, 0.05, 0.05, 0.05, 0.01
                );
            }
        }
    }

    @Override
    protected void onHitEntity(EntityHitResult result) {
        super.onHitEntity(result);

        if (!(this.level() instanceof ServerLevel sl)) return;
        if (!(result.getEntity() instanceof LivingEntity target)) return;
        if (ownerUUID != null && target.getUUID().equals(ownerUUID)) return;

        target.invulnerableTime = 0;
        target.hurtTime = 0;
        target.hurt(RinneDamageSource.of(sl, this.getOwner()), damage);

        // 流星を1発
        spawnMeteorStar(sl, target);

        sl.sendParticles(
                ParticleTypes.FLASH,
                this.getX(), this.getY(), this.getZ(),
                1, 0, 0, 0, 0
        );

        this.discard();
    }

    @Override
    protected void onHitBlock(BlockHitResult result) {
        super.onHitBlock(result);

        if (this.level() instanceof ServerLevel sl) {
            sl.sendParticles(
                    ParticleTypes.FLASH,
                    this.getX(), this.getY(), this.getZ(),
                    1, 0, 0, 0, 0
            );
        }

        this.discard();
    }

    private void spawnMeteorStar(ServerLevel sl, LivingEntity target) {
        MeteorStarEntity star = new MeteorStarEntity(sl, this.getOwner() instanceof net.minecraft.world.entity.player.Player p ? p : null);
        star.setPos(target.getX(), target.getY() + 40.0D, target.getZ());
        star.setTargetY(target.getY());
        star.setFallTicks(60);
        star.setDamage(30.0F);
        star.setHealAmount(5.0F);
        sl.addFreshEntity(star);
    }

    @Nullable
    public net.minecraft.world.entity.Entity getOwner() {
        if (ownerUUID == null) return null;
        if (this.level() instanceof ServerLevel sl) {
            return sl.getEntity(ownerUUID);
        }
        return null;
    }

    @Override
    public Packet<ClientGamePacketListener> getAddEntityPacket() {
        return NetworkHooks.getEntitySpawningPacket(this);
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        if (ownerUUID != null) tag.putUUID("OwnerUUID", ownerUUID);
        tag.putInt("Life", life);
        tag.putFloat("StarDamage", damage);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        if (tag.hasUUID("OwnerUUID")) ownerUUID = tag.getUUID("OwnerUUID");
        if (tag.contains("Life")) life = tag.getInt("Life");
        if (tag.contains("StarDamage")) damage = tag.getFloat("StarDamage");
    }
}