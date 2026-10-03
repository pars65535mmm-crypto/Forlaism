package com.tyami.forlaism.entity;

import com.tyami.forlaism.damage.RinneDamageSource;
import com.tyami.forlaism.registry.ModEntityTypes;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.ThrowableItemProjectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.NetworkHooks;

import javax.annotation.Nullable;
import java.util.UUID;

/**
 * エネルギーダガーから放たれる投擲ナイフ。
 *
 * - 重力なし
 * - 輪廻 3 + 二刀の極致 2
 * - ヒット時に消滅
 */
public class EnergyKnifeEntity extends ThrowableItemProjectile {

    /** 輪廻ダメージ量。 */
    public static final float RINNE_DAMAGE = 3.0F;

    /** dual_wielding_mastery ダメージ量。 */
    public static final float DUAL_WIELDING_DAMAGE = 2.0F;

    /** 二刀の極致ダメージソース。 */
    private static final ResourceKey<net.minecraft.world.damagesource.DamageType> DUAL_WIELDING =
            ResourceKey.create(
                    Registries.DAMAGE_TYPE,
                    new ResourceLocation("forlaism", "dual_wielding_mastery_ddd")
            );

    private static final float VELOCITY = 3.0F;
    private static final int MAX_LIFE = 1200;

    @Nullable
    private UUID ownerUUID;

    public EnergyKnifeEntity(EntityType<? extends EnergyKnifeEntity> type, Level level) {
        super(type, level);
        this.setNoGravity(true);
    }

    public EnergyKnifeEntity(Level level, LivingEntity shooter) {
        super(ModEntityTypes.ENERGY_KNIFE.get(), shooter, level);
        this.ownerUUID = shooter.getUUID();
        this.setNoGravity(true);
    }

    @Override
    protected Item getDefaultItem() {
        return com.tyami.forlaism.registry.Items.ENERGY_DAGGER.get();
    }

    public void shoot(Vec3 direction) {
        Vec3 d = direction.normalize().scale(VELOCITY);
        this.setDeltaMovement(d);
        this.hurtMarked = true;
    }

    @Override
    public void tick() {
        super.tick();

        if (this.tickCount > MAX_LIFE || this.isRemoved()) {
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
                    ParticleTypes.ELECTRIC_SPARK,
                    this.getX(), this.getY(), this.getZ(),
                    (this.random.nextDouble() - 0.5) * 0.02,
                    (this.random.nextDouble() - 0.5) * 0.02,
                    (this.random.nextDouble() - 0.5) * 0.02
            );
        } else if (this.level() instanceof ServerLevel sl && this.tickCount % 2 == 0) {
            sl.sendParticles(
                    ParticleTypes.END_ROD,
                    this.getX(), this.getY(), this.getZ(),
                    1, 0.02, 0.02, 0.02, 0.0
            );
            sl.sendParticles(
                    ParticleTypes.ELECTRIC_SPARK,
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

        Entity hit = result.getEntity();
        if (!(hit instanceof LivingEntity target)) return;
        if (ownerUUID != null && target.getUUID().equals(ownerUUID)) return;

        // 発射者を解決
        Entity owner = null;
        if (ownerUUID != null) {
            owner = sl.getEntity(ownerUUID);
            if (owner == null) {
                owner = sl.getServer().getPlayerList().getPlayer(ownerUUID);
            }
        }

        // 無敵時間リセット
        target.invulnerableTime = 0;
        target.hurtTime = 0;

        // 輪廻ダメージ 3
        target.hurt(RinneDamageSource.of(sl, owner), RINNE_DAMAGE);

        // 二刀の極致ダメージ 2
        target.invulnerableTime = 0;
        DamageSource dualSource = new DamageSource(
                sl.registryAccess()
                        .registryOrThrow(Registries.DAMAGE_TYPE)
                        .getHolderOrThrow(DUAL_WIELDING),
                owner
        );
        target.hurt(dualSource, DUAL_WIELDING_DAMAGE);

        // ヒット演出
        sl.sendParticles(
                ParticleTypes.FLASH,
                this.getX(), this.getY(), this.getZ(),
                1, 0, 0, 0, 0
        );
        sl.sendParticles(
                ParticleTypes.ELECTRIC_SPARK,
                this.getX(), this.getY(), this.getZ(),
                15, 0.2, 0.2, 0.2, 0.15
        );

        sl.playSound(
                null,
                this.getX(), this.getY(), this.getZ(),
                SoundEvents.PLAYER_ATTACK_CRIT,
                SoundSource.PLAYERS,
                0.8F, 1.5F
        );

        this.discard();
    }

    @Override
    protected void onHitBlock(BlockHitResult result) {
        super.onHitBlock(result);

        if (this.level().isClientSide) return;
        if (!(this.level() instanceof ServerLevel sl)) return;

        sl.sendParticles(
                ParticleTypes.ELECTRIC_SPARK,
                this.getX(), this.getY(), this.getZ(),
                10, 0.15, 0.15, 0.15, 0.1
        );

        sl.playSound(
                null,
                this.getX(), this.getY(), this.getZ(),
                SoundEvents.AMETHYST_BLOCK_HIT,
                SoundSource.PLAYERS,
                0.6F, 1.8F
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
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        if (tag.hasUUID("OwnerUUID")) ownerUUID = tag.getUUID("OwnerUUID");
    }
}