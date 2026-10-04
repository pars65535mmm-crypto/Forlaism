package com.tyami.forlaism.entity;

import com.tyami.forlaism.damage.MadoromiDamageSource;
import com.tyami.forlaism.damage.RinneDamageSource;
import com.tyami.forlaism.registry.ModEntityTypes;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import com.tyami.forlaism.damage.RinneDamageSource;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
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
 * プリズムライトの光の弾。
 *
 * - ロック対象へ滑らかにホーミング
 * - ダメージ: 微睡4 + 奈落4 + 通常4 = 12
 * - フルチャージ弾は着弾時に雷撃 + 爆裂
 */
public class PrismLightOrbEntity extends ThrowableItemProjectile {

public static final float RINNE_DAMAGE = 6.0F;
public static final float NORMAL_DAMAGE = 6.0F;

    /** ホーミング強度（0.0〜1.0）。 */
    private static final double HOMING_STRENGTH = 0.35D;

    /** 弾速。 */
    private static final double SPEED = 2.2D;

    /** 最大寿命tick。 */
    private static final int MAX_LIFE = 200;

    /** フルチャージ弾かどうか。 */
    private boolean fullCharged = false;

    @Nullable
    private UUID targetUUID;

    public PrismLightOrbEntity(EntityType<? extends PrismLightOrbEntity> type, Level level) {
        super(type, level);
        this.setNoGravity(true);
    }

    public PrismLightOrbEntity(Level level, LivingEntity shooter) {
        super(ModEntityTypes.PRISM_LIGHT_ORB.get(), shooter, level);
        this.setNoGravity(true);
    }

    @Override
    protected Item getDefaultItem() {
        return Items.ENDER_EYE;
    }

    public void setTarget(@Nullable LivingEntity target) {
        this.targetUUID = target != null ? target.getUUID() : null;
    }

    public boolean hasTarget() {
    return targetUUID != null;
}

    public void setFullCharged(boolean value) {
        this.fullCharged = value;
    }

    public void shoot(Vec3 direction) {
        Vec3 d = direction.normalize().scale(SPEED);
        this.setDeltaMovement(d);
        this.hurtMarked = true;
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

        // =========================================================
        // ホーミング + 手動当たり判定
        // =========================================================
        if (!this.level().isClientSide && targetUUID != null) {
            Entity target = ((ServerLevel) this.level()).getEntity(targetUUID);

            if (target instanceof LivingEntity living && living.isAlive()) {

                // ---- 手動当たり判定 ----
                // デフォルトの当たり判定は弾が小さすぎてデカいentityに当たらないことがある
                double dx = living.getX() - this.getX();
                double dy = (living.getY() + living.getBbHeight() * 0.5) - this.getY();
                double dz = living.getZ() - this.getZ();
                double distSqr = dx * dx + dy * dy + dz * dz;

                // 当たり半径: entityの幅と弾の半径を考慮
                double hitRadius = 0.5 + Math.max(living.getBbWidth(), living.getBbHeight()) * 0.5;
                if (distSqr <= hitRadius * hitRadius) {
                    // ヒット！
                    ServerLevel sl = (ServerLevel) this.level();
                    Entity owner = this.getOwner();
                    applyDualDamage(sl, living, owner);
                    if (fullCharged) {
                        triggerFullChargeExplosion(sl, this.position());
                    }
                    this.discard();
                    return;
                }

                // ---- ホーミング ----
                Vec3 targetPos = new Vec3(
                        living.getX(),
                        living.getY() + living.getBbHeight() * 0.5,
                        living.getZ()
                );
                Vec3 toTarget = targetPos.subtract(this.position());
                double distance = toTarget.length();

                if (distance > 1.5) {
                    Vec3 currentDir = this.getDeltaMovement().normalize();
                    Vec3 targetDir = toTarget.normalize();

                    // 角度制限ホーミング（8度/tick）
                    double maxTurn = Math.toRadians(8.0);
                    double dot = Math.max(-1.0, Math.min(1.0, currentDir.dot(targetDir)));
                    double angleDiff = Math.acos(dot);

                    Vec3 newDir;
                    if (angleDiff <= maxTurn) {
                        newDir = targetDir;
                    } else {
                        double t = maxTurn / angleDiff;
                        newDir = currentDir.lerp(targetDir, t).normalize();
                    }

                    this.setDeltaMovement(newDir.scale(SPEED));
                    this.hurtMarked = true;
                }

            } else {
                targetUUID = null;
            }
        }

        // =========================================================
        // パーティクル
        // =========================================================
        if (!this.level().isClientSide && this.level() instanceof ServerLevel sl) {
            sl.sendParticles(ParticleTypes.FLASH,
                    this.getX(), this.getY(), this.getZ(),
                    1, 0, 0, 0, 0);
            sl.sendParticles(ParticleTypes.END_ROD,
                    this.getX(), this.getY(), this.getZ(),
                    3, 0.08, 0.08, 0.08, 0.02);
            sl.sendParticles(ParticleTypes.SOUL_FIRE_FLAME,
                    this.getX(), this.getY(), this.getZ(),
                    2, 0.1, 0.1, 0.1, 0.01);

            float time = this.tickCount * 0.3F;
            for (int i = 0; i < 3; i++) {
                double angle = time + (i * Math.PI * 2 / 3);
                double r = 0.35;
                sl.sendParticles(ParticleTypes.ELECTRIC_SPARK,
                        this.getX() + Math.cos(angle) * r,
                        this.getY() + Math.sin(time * 1.5 + i) * 0.15,
                        this.getZ() + Math.sin(angle) * r,
                        1, 0, 0, 0, 0);
            }

            sl.sendParticles(ParticleTypes.DRAGON_BREATH,
                    this.getX(), this.getY(), this.getZ(),
                    1, 0.03, 0.03, 0.03, 0.0);

            if (fullCharged) {
                sl.sendParticles(ParticleTypes.SOUL,
                        this.getX(), this.getY(), this.getZ(),
                        4, 0.15, 0.15, 0.15, 0.02);
            }
        }
    }

    private void spawnClientParticles() {
        this.level().addParticle(
                ParticleTypes.END_ROD,
                this.getX(), this.getY(), this.getZ(),
                0, 0, 0
        );
        this.level().addParticle(
                ParticleTypes.FLASH,
                this.getX(), this.getY(), this.getZ(),
                0, 0, 0
        );
    }

    // =========================================================
    // ヒット処理
    // =========================================================

    @Override
    protected void onHitEntity(EntityHitResult result) {
        super.onHitEntity(result);
        if (this.level().isClientSide) return;
        if (!(this.level() instanceof ServerLevel sl)) return;

        Entity hit = result.getEntity();
        if (!(hit instanceof LivingEntity target)) return;

        // 発射者自身は除外
        Entity owner = this.getOwner();
        if (owner != null && target.getUUID().equals(owner.getUUID())) return;

        // 3種ダメージ
        applyDualDamage(sl, target, owner);

        // フルチャージなら着弾点で雷撃
        if (fullCharged) {
            triggerFullChargeExplosion(sl, this.position());
        }

        this.discard();
    }

    @Override
    protected void onHitBlock(BlockHitResult result) {
        super.onHitBlock(result);
        if (this.level().isClientSide) return;
        if (!(this.level() instanceof ServerLevel sl)) return;

        // フルチャージなら着弾点で雷撃
        if (fullCharged) {
            triggerFullChargeExplosion(sl, this.position());
        }

        this.discard();
    }

    private void applyDualDamage(ServerLevel sl, LivingEntity target, @Nullable Entity owner) {

        target.invulnerableTime = 0;
        target.hurtTime = 0;

        // 1. 通常ダメージ
        DamageSource normal = (owner instanceof net.minecraft.world.entity.player.Player p)
                ? sl.damageSources().playerAttack(p)
                : sl.damageSources().generic();
        target.hurt(normal, NORMAL_DAMAGE);

        // 2. 輪廻ダメージ
        target.invulnerableTime = 0;
        target.hurtTime = 0;
        target.hurt(RinneDamageSource.of(sl, owner), RINNE_DAMAGE);

        // ヒット演出
        sl.sendParticles(
                ParticleTypes.FLASH,
                target.getX(), target.getY() + 1.0, target.getZ(),
                1, 0, 0, 0, 0
        );
        sl.playSound(null, target.getX(), target.getY(), target.getZ(),
                SoundEvents.AMETHYST_BLOCK_HIT, SoundSource.PLAYERS, 1.0F, 1.8F);
    }

    private void triggerFullChargeExplosion(ServerLevel sl, Vec3 pos) {

        // 落雷
        var lightning = EntityType.LIGHTNING_BOLT.create(sl);
        if (lightning != null) {
            lightning.moveTo(pos.x, pos.y, pos.z);
            lightning.setVisualOnly(true);
            sl.addFreshEntity(lightning);
        }

        // 爆発演出
        sl.sendParticles(
                ParticleTypes.EXPLOSION_EMITTER,
                pos.x, pos.y, pos.z,
                1, 0, 0, 0, 0
        );
        sl.sendParticles(
                ParticleTypes.END_ROD,
                pos.x, pos.y, pos.z,
                40, 1.5, 1.5, 1.5, 0.15
        );
        sl.playSound(null, pos.x, pos.y, pos.z,
                SoundEvents.LIGHTNING_BOLT_THUNDER, SoundSource.PLAYERS, 1.5F, 1.5F);

        // 周囲に奈落ダメージ
        var nearby = sl.getEntitiesOfClass(
                LivingEntity.class,
                new net.minecraft.world.phys.AABB(pos.x - 3, pos.y - 3, pos.z - 3,
                        pos.x + 3, pos.y + 3, pos.z + 3),
                e -> e.isAlive() && !e.isSpectator()
        );
        for (LivingEntity e : nearby) {
            e.invulnerableTime = 0;
            e.hurt(sl.damageSources().fellOutOfWorld(), 6.0F);
        }
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
        if (targetUUID != null) tag.putUUID("TargetUUID", targetUUID);
        tag.putBoolean("FullCharged", fullCharged);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        if (tag.hasUUID("TargetUUID")) targetUUID = tag.getUUID("TargetUUID");
        fullCharged = tag.getBoolean("FullCharged");
    }
}