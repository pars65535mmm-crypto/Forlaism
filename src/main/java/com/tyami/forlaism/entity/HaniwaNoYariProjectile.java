package com.tyami.forlaism.entity;

import com.tyami.forlaism.item.HaniwaNoYariItem;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.server.level.ServerLevel;
import com.tyami.forlaism.registry.ModEntityTypes;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
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
import java.util.Random;

/**
 * ハニワノヤリの投擲体。
 *
 * - チャージして発射される
 * - 回収不可
 * - Shift発射版は着弾地点で爆破
 * - 命中時も 0.5% で即死
 */
public class HaniwaNoYariProjectile extends ThrowableItemProjectile {

    private static final Random RANDOM = new Random();

    /** 命中ダメージ。 */
    private float damage = HaniwaNoYariItem.PROJECTILE_DAMAGE;

    /** Shift発射版かどうか。 */
    private boolean explosive = false;

    /** 寿命tick。 */
    private static final int MAX_LIFE = 200;

    public HaniwaNoYariProjectile(EntityType<? extends HaniwaNoYariProjectile> type, Level level) {
        super(type, level);
    }

    public HaniwaNoYariProjectile(Level level, LivingEntity shooter) {
        super(ModEntityTypes.HANIWA_NO_YARI.get(), shooter, level);
    }

    @Override
    protected Item getDefaultItem() {
        return com.tyami.forlaism.registry.Items.HANIWA_NO_YARI.get();
    }

    public void setDamage(float damage) {
        this.damage = damage;
    }

    public void setExplosive(boolean explosive) {
        this.explosive = explosive;
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

        // クライアント：発光パーティクル（ゲーミング）
        if (this.level().isClientSide) {
            this.level().addParticle(
                    ParticleTypes.END_ROD,
                    this.getX(), this.getY(), this.getZ(),
                    0, 0, 0
            );
            if (this.tickCount % 2 == 0) {
                this.level().addParticle(
                        ParticleTypes.FLAME,
                        this.getX(), this.getY(), this.getZ(),
                        0, 0, 0
                );
            }
        } else if (this.level() instanceof ServerLevel sl) {
    // =========================================================
    // 残像トレイル（派手め）
    // =========================================================
    Vec3 vel = this.getDeltaMovement();
    Vec3 back = vel.normalize().scale(-0.5);

    // メインの発光
    sl.sendParticles(
            ParticleTypes.END_ROD,
            this.getX(), this.getY(), this.getZ(),
            2, 0.05, 0.05, 0.05, 0.0
    );

    // 火の粉
    sl.sendParticles(
            ParticleTypes.FLAME,
            this.getX() + back.x, this.getY() + back.y, this.getZ() + back.z,
            2, 0.05, 0.05, 0.05, 0.01
    );

    // 時々ソウルファイア
    if (this.tickCount % 3 == 0) {
        sl.sendParticles(
                ParticleTypes.SOUL_FIRE_FLAME,
                this.getX(), this.getY(), this.getZ(),
                2, 0.1, 0.1, 0.1, 0.01
        );
    }

    // 電気の火花
    if (this.tickCount % 4 == 0) {
        sl.sendParticles(
                ParticleTypes.ELECTRIC_SPARK,
                this.getX(), this.getY(), this.getZ(),
                3, 0.1, 0.1, 0.1, 0.05
        );
    }

    // 通常弾よりちょっと多い煙
    if (this.tickCount % 2 == 0) {
        sl.sendParticles(
                ParticleTypes.SMOKE,
                this.getX() + back.x, this.getY() + back.y, this.getZ() + back.z,
                1, 0.05, 0.05, 0.05, 0.0
        );
    }
}
    }

    // =========================================================
    // 命中：Entity
    // =========================================================

    @Override
    protected void onHitEntity(EntityHitResult result) {
        super.onHitEntity(result);

        if (!(this.level() instanceof ServerLevel sl)) return;

        Entity hit = result.getEntity();
        if (!(hit instanceof LivingEntity target)) return;

        // 自分自身（発射者）は除外
        Entity owner = this.getOwner();
        if (owner != null && target.getUUID().equals(owner.getUUID())) return;

        // 発火
        target.setSecondsOnFire(8);

        // 通常ダメージ
        DamageSource source = (owner instanceof Player p)
                ? sl.damageSources().playerAttack(p)
                : sl.damageSources().generic();

        target.invulnerableTime = 0;
        target.hurt(source, damage);

        // 0.5% 即死
        if (RANDOM.nextFloat() < HaniwaNoYariItem.INSTANT_KILL_CHANCE) {
            target.invulnerableTime = 0;
            target.setHealth(0.0F);
            if (!target.isDeadOrDying()) {
                target.die(source);
            }

            sl.sendParticles(
                    ParticleTypes.EXPLOSION_EMITTER,
                    target.getX(), target.getY() + 1.0, target.getZ(),
                    1, 0, 0, 0, 0
            );
            sl.playSound(null,
                    target.getX(), target.getY(), target.getZ(),
                    SoundEvents.WITHER_DEATH,
                    SoundSource.PLAYERS, 1.5F, 0.5F
            );
        }

        // Shift発射版：命中で爆破
        if (explosive) {
            detonate(sl);
        }

        this.discard();
    }

    // =========================================================
    // 命中：Block
    // =========================================================

    @Override
    protected void onHitBlock(BlockHitResult result) {
        super.onHitBlock(result);

        if (!(this.level() instanceof ServerLevel sl)) return;

        if (explosive) {
            detonate(sl);
        }

        sl.sendParticles(
                ParticleTypes.CRIT,
                this.getX(), this.getY(), this.getZ(),
                8, 0.2, 0.2, 0.2, 0.1
        );

        this.discard();
    }

    // =========================================================
    // 爆破
    // =========================================================

    private void detonate(ServerLevel sl) {
        Vec3 pos = this.position();

        sl.explode(
                this,
                pos.x, pos.y, pos.z,
                3.0F,
                Level.ExplosionInteraction.NONE
        );

        sl.sendParticles(
                ParticleTypes.EXPLOSION_EMITTER,
                pos.x, pos.y, pos.z,
                1, 0, 0, 0, 0
        );
        sl.sendParticles(
                ParticleTypes.FLAME,
                pos.x, pos.y, pos.z,
                40, 1.0, 1.0, 1.0, 0.15
        );

        sl.playSound(null,
                pos.x, pos.y, pos.z,
                SoundEvents.GENERIC_EXPLODE,
                SoundSource.PLAYERS, 1.5F, 1.0F
        );
    }

    // =========================================================
    // 同期・NBT
    // =========================================================

    @Override
    public Packet<ClientGamePacketListener> getAddEntityPacket() {
        return NetworkHooks.getEntitySpawningPacket(this);
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putFloat("Damage", damage);
        tag.putBoolean("Explosive", explosive);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        if (tag.contains("Damage")) damage = tag.getFloat("Damage");
        explosive = tag.getBoolean("Explosive");
    }
}