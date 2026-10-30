package com.tyami.forlaism.entity;

import com.tyami.forlaism.registry.ModEntityTypes;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ThrowableItemProjectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.NetworkHooks;

import javax.annotation.Nullable;
import java.util.List;
import java.util.UUID;

/**
 * チキンキャノンの飛んでく鶏。
 *
 * - 発射から 20tick (1秒) はまっすぐ飛ぶ
 * - 20tick経過で卵を大量噴射しながら爆発（地形破壊なし）
 * - 爆発で周囲の敵にダメージ
 */
public class ChickenCannonProjectileEntity extends ThrowableItemProjectile {

    /** 爆発するまでの時間 (tick)。1秒 = 20tick。 */
    public static final int FLIGHT_TICKS = 20;

    /** 爆発半径。 */
    public static final double EXPLOSION_RADIUS = 4.0D;

    /** 爆発ダメージ。 */
    public static final float EXPLOSION_DAMAGE = 6.0F;

    /** 卵の数。 */
    public static final int EGG_COUNT = 24;

    @Nullable
    private UUID ownerUUID;

    public ChickenCannonProjectileEntity(EntityType<? extends ChickenCannonProjectileEntity> type, Level level) {
        super(type, level);
        this.setNoGravity(true);
    }

    public ChickenCannonProjectileEntity(Level level, LivingEntity shooter) {
        super(ModEntityTypes.CHICKEN_CANNON_PROJECTILE.get(), shooter, level);
        this.setNoGravity(true);
        this.ownerUUID = shooter.getUUID();
    }

    @Override
    protected Item getDefaultItem() {
        return Items.CHICKEN;
    }

    public void shoot(Vec3 direction, double speed) {
        this.setDeltaMovement(direction.normalize().scale(speed));
        this.hurtMarked = true;
    }

    // =========================================================
    // tick
    // =========================================================

    @Override
    public void tick() {
        super.tick();

        if (this.isRemoved()) return;

        // 飛行中：鶏パーティクルを撒く
        if (this.level() instanceof ServerLevel sl) {
            sl.sendParticles(
                    ParticleTypes.CLOUD,
                    this.getX(), this.getY() + 0.3, this.getZ(),
                    2, 0.1, 0.1, 0.1, 0.0
            );
            sl.sendParticles(
                    ParticleTypes.CLOUD,
                    this.getX(), this.getY(), this.getZ(),
                    1, 0.05, 0.05, 0.05, 0.01
            );
        }

        // 1秒経過 → 爆発
        if (this.tickCount >= FLIGHT_TICKS) {
            explode();
        }
    }

    // =========================================================
    // 爆発
    // =========================================================

    private void explode() {
        if (!(this.level() instanceof ServerLevel sl)) {
            this.discard();
            return;
        }

        Vec3 pos = this.position();

        // =========================================================
        // 1. 卵を大量噴射
        // =========================================================
        for (int i = 0; i < EGG_COUNT; i++) {
            double angle = this.random.nextDouble() * Math.PI * 2;
            double pitch = (this.random.nextDouble() - 0.5) * Math.PI;
            double speed = 0.4 + this.random.nextDouble() * 0.4;

            double vx = Math.cos(angle) * Math.cos(pitch) * speed;
            double vy = Math.sin(pitch) * speed + 0.2;
            double vz = Math.sin(angle) * Math.cos(pitch) * speed;

            sl.sendParticles(
                    ParticleTypes.ITEM_SNOWBALL, // 卵っぽく見える
                    pos.x, pos.y + 0.5, pos.z,
                    1,
                    vx, vy, vz,
                    0.5
            );
        }

        // =========================================================
        // 2. 爆発パーティクル
        // =========================================================
        sl.sendParticles(
                ParticleTypes.EXPLOSION_EMITTER,
                pos.x, pos.y + 0.5, pos.z,
                1, 0, 0, 0, 0
        );
        sl.sendParticles(
                ParticleTypes.EXPLOSION,
                pos.x, pos.y + 0.5, pos.z,
                5, 1.0, 0.5, 1.0, 0
        );
        sl.sendParticles(
                ParticleTypes.CRIT,
                pos.x, pos.y + 0.5, pos.z,
                40, 1.5, 1.0, 1.5, 0.1
        );

        // =========================================================
        // 3. 爆発音
        // =========================================================
        sl.playSound(null, pos.x, pos.y, pos.z,
                SoundEvents.GENERIC_EXPLODE, SoundSource.PLAYERS,
                1.5F, 1.2F);
        sl.playSound(null, pos.x, pos.y, pos.z,
                SoundEvents.CHICKEN_HURT, SoundSource.PLAYERS,
                1.5F, 0.8F);
        sl.playSound(null, pos.x, pos.y, pos.z,
                SoundEvents.CHICKEN_EGG, SoundSource.PLAYERS,
                2.0F, 1.5F);

        // =========================================================
        // 4. 範囲ダメージ（地形破壊なし）
        // =========================================================
        AABB area = new AABB(
                pos.x - EXPLOSION_RADIUS, pos.y - EXPLOSION_RADIUS, pos.z - EXPLOSION_RADIUS,
                pos.x + EXPLOSION_RADIUS, pos.y + EXPLOSION_RADIUS, pos.z + EXPLOSION_RADIUS
        );

        List<LivingEntity> targets = sl.getEntitiesOfClass(
                LivingEntity.class, area,
                e -> e.isAlive() && !e.isSpectator()
        );

        Entity owner = getOwnerEntity();
        for (LivingEntity target : targets) {
            // 発射主は除外
            if (owner != null && target.getUUID().equals(owner.getUUID())) continue;

            // 距離減衰
            double dist = target.position().distanceTo(pos);
            if (dist > EXPLOSION_RADIUS) continue;
            float falloff = (float) (1.0D - dist / EXPLOSION_RADIUS);
            float dmg = EXPLOSION_DAMAGE * (0.5F + 0.5F * falloff);

            target.invulnerableTime = 0;
            target.hurtTime = 0;

            DamageSource source = owner instanceof Player p
                    ? sl.damageSources().playerAttack(p)
                    : sl.damageSources().generic();

            target.hurt(source, dmg);

            // ノックバック
            Vec3 away = target.position().subtract(pos).normalize();
            target.push(away.x * 0.6, 0.4, away.z * 0.6);
            target.hurtMarked = true;
        }

        this.discard();
    }

    // =========================================================
    // 命中処理
    // =========================================================

    @Override
    protected void onHitEntity(EntityHitResult result) {
        super.onHitEntity(result);
        // 命中したらすぐ爆発
        explode();
    }

    @Override
    protected void onHit(net.minecraft.world.phys.HitResult result) {
        super.onHit(result);
        if (result.getType() == HitResult.Type.BLOCK) {
            explode();
        }
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

    @Override
    public Packet<ClientGamePacketListener> getAddEntityPacket() {
        return NetworkHooks.getEntitySpawningPacket(this);
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        if (ownerUUID != null) tag.putUUID("OwnerUUID", ownerUUID);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        if (tag.hasUUID("OwnerUUID")) ownerUUID = tag.getUUID("OwnerUUID");
    }
}