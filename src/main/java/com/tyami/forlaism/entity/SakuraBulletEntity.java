package com.tyami.forlaism.entity;

import com.tyami.forlaism.damage.MoonlightDamageSource;
import com.tyami.forlaism.registry.ModEntityTypes;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
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
 * SKR360 Inf の弾丸。
 *
 * - 直進
 * - 月光ダメージ（防御貫通）120
 * - 桜色の軌跡
 */
public class SakuraBulletEntity extends ThrowableItemProjectile {

    private static final int MAX_LIFE = 100;

    @Nullable
    private UUID ownerUUID;

    private float damage = 120.0F;

    public SakuraBulletEntity(EntityType<? extends SakuraBulletEntity> type, Level level) {
        super(type, level);
        this.setNoGravity(true);
    }

    public SakuraBulletEntity(Level level, LivingEntity shooter) {
        super(ModEntityTypes.SAKURA_BULLET.get(), shooter, level);
        this.setNoGravity(true);
        this.ownerUUID = shooter.getUUID();
    }

    @Override
    protected Item getDefaultItem() {
        return Items.AIR;
    }

    public void shoot(Vec3 direction, float speed, float damage) {
        Vec3 d = direction.normalize().scale(speed);
        this.setDeltaMovement(d);
        this.hurtMarked = true;
        this.damage = damage;
    }

    // =========================================================
    // tick
    // =========================================================

    @Override
    public void tick() {
        super.tick();

        if (this.tickCount > MAX_LIFE || this.isRemoved()) {
            this.discard();
            return;
        }

        // クライアント側: パーティクル
        if (this.level().isClientSide) {
            this.level().addParticle(
                    ParticleTypes.CHERRY_LEAVES,
                    this.getX(), this.getY(), this.getZ(),
                    (this.random.nextDouble() - 0.5) * 0.05,
                    (this.random.nextDouble() - 0.5) * 0.05,
                    (this.random.nextDouble() - 0.5) * 0.05
            );
            this.level().addParticle(
                    ParticleTypes.END_ROD,
                    this.getX(), this.getY(), this.getZ(),
                    0, 0, 0
            );
        } else if (this.level() instanceof ServerLevel sl && this.tickCount % 2 == 0) {
            sl.sendParticles(
                    ParticleTypes.CHERRY_LEAVES,
                    this.getX(), this.getY(), this.getZ(),
                    1, 0.05, 0.05, 0.05, 0.01
            );
            sl.sendParticles(
                    ParticleTypes.END_ROD,
                    this.getX(), this.getY(), this.getZ(),
                    1, 0.02, 0.02, 0.02, 0.0
            );
        }
    }

    // =========================================================
    // ヒット処理
    // =========================================================

    @Override
    protected void onHitEntity(EntityHitResult result) {
        super.onHitEntity(result);

        if (this.level().isClientSide) return;
        if (!(this.level() instanceof ServerLevel sl)) return;

        if (!(result.getEntity() instanceof LivingEntity target)) return;
        if (ownerUUID != null && target.getUUID().equals(ownerUUID)) return;

        // 発射者を解決
        net.minecraft.world.entity.Entity owner = null;
        if (ownerUUID != null) {
            owner = sl.getEntity(ownerUUID);
            if (owner == null) {
                owner = sl.getServer().getPlayerList().getPlayer(ownerUUID);
            }
        }

        // 月光ダメージ（防御貫通）
        target.invulnerableTime = 0;
        target.hurtTime = 0;
        target.hurt(
                MoonlightDamageSource.of(sl, owner),
                damage
        );

        // ヒット演出
        sl.sendParticles(
                ParticleTypes.CHERRY_LEAVES,
                this.getX(), this.getY(), this.getZ(),
                30, 0.4, 0.4, 0.4, 0.1
        );
        sl.sendParticles(
                ParticleTypes.FLASH,
                this.getX(), this.getY(), this.getZ(),
                1, 0, 0, 0, 0
        );
        sl.playSound(
                null,
                this.getX(), this.getY(), this.getZ(),
                SoundEvents.AMETHYST_BLOCK_CHIME,
                SoundSource.PLAYERS,
                1.2F, 1.5F
        );

        this.discard();
    }

    @Override
    protected void onHitBlock(BlockHitResult result) {
        super.onHitBlock(result);

        if (this.level().isClientSide) return;
        if (!(this.level() instanceof ServerLevel sl)) return;

        sl.sendParticles(
                ParticleTypes.CHERRY_LEAVES,
                this.getX(), this.getY(), this.getZ(),
                15, 0.2, 0.2, 0.2, 0.05
        );

        this.discard();
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
        tag.putFloat("BulletDamage", damage);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        if (tag.hasUUID("OwnerUUID")) ownerUUID = tag.getUUID("OwnerUUID");
        if (tag.contains("BulletDamage")) damage = tag.getFloat("BulletDamage");
    }
}