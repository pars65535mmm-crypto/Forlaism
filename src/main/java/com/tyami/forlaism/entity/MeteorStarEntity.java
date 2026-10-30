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
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.NetworkHooks;

import javax.annotation.Nullable;
import java.util.List;
import java.util.UUID;

/**
 * インサジネーターの流星。
 *
 * - 上空で待機 → 落下 → 着弾で範囲ダメージ
 * - 発動者以外に30ダメージ + ノックバック
 * - 発動者を10回復
 */
public class MeteorStarEntity extends Entity {

    /** デフォルト落下時間（tick）。 */
    private static final int DEFAULT_FALL_TICKS = 100;

    /** 着弾時のダメージ半径。 */
    private static final double IMPACT_RADIUS = 5.0D;

    @Nullable
    private UUID ownerUUID;

    private double targetY;
    private int fallTicks = DEFAULT_FALL_TICKS;

    /** ★ 自前の経過tick（Entity#tickCount と衝突しないように改名） */
    private int elapsedTicks = 0;

    private float damage = 30.0F;
    private float healAmount = 10.0F;
    private boolean impacted = false;

    public MeteorStarEntity(EntityType<?> type, Level level) {
        super(type, level);
        this.noPhysics = true;
        this.setNoGravity(true);
        this.setInvulnerable(true);
    }

    public MeteorStarEntity(Level level, Player owner) {
        this(ModEntityTypes.METEOR_STAR.get(), level);
        this.ownerUUID = owner.getUUID();
    }

    @Override
    protected void defineSynchedData() {
    }

    // =========================================================
    // Setter
    // =========================================================

    public void setTargetY(double y) {
        this.targetY = y;
    }

    public void setFallTicks(int ticks) {
        this.fallTicks = ticks;
    }

    public void setDamage(float damage) {
        this.damage = damage;
    }

    public void setHealAmount(float heal) {
        this.healAmount = heal;
    }

    /** ★ Renderer 用に読み取り専用 getter を用意 */
    public int getElapsedTicks() {
        return elapsedTicks;
    }

    // =========================================================
    // Tick
    // =========================================================

    @Override
    public void tick() {
        super.tick();

        if (this.level().isClientSide) {
            clientParticles();
            return;
        }

        if (impacted) return;

        elapsedTicks++;

        // 落下中
        double progress = Math.min(1.0D, elapsedTicks / (double) fallTicks);
        double eased = easeInQuad(progress);

        double startY = targetY + 40.0D;
        double newY = startY + (targetY - startY) * eased;

        this.setPos(this.getX(), newY, this.getZ());

        // 落下中のパーティクル
        if (this.level() instanceof ServerLevel sl) {
            spawnTrail(sl);
        }

        // 着弾判定
        if (progress >= 1.0D) {
            impact();
        }
    }

    private static double easeInQuad(double t) {
        return t * t;
    }

    // =========================================================
    // 演出
    // =========================================================

    private void clientParticles() {
        this.level().addParticle(
                ParticleTypes.END_ROD,
                this.getX(), this.getY(), this.getZ(),
                0, 0, 0
        );
    }

    private void spawnTrail(ServerLevel sl) {
        // 星の軌跡：END_ROD + FIREWORK + ELECTRIC_SPARK
        sl.sendParticles(
                ParticleTypes.END_ROD,
                this.getX(), this.getY(), this.getZ(),
                3, 0.15, 0.15, 0.15, 0.02
        );

        if (elapsedTicks % 2 == 0) {
            sl.sendParticles(
                    ParticleTypes.FIREWORK,
                    this.getX(), this.getY(), this.getZ(),
                    2, 0.2, 0.2, 0.2, 0.05
            );
        }

        if (elapsedTicks % 3 == 0) {
            sl.sendParticles(
                    ParticleTypes.ELECTRIC_SPARK,
                    this.getX(), this.getY(), this.getZ(),
                    3, 0.2, 0.2, 0.2, 0.05
            );
        }

        // 落下中の音
        if (elapsedTicks % 10 == 0) {
            sl.playSound(
                    null,
                    this.getX(), this.getY(), this.getZ(),
                    SoundEvents.FIREWORK_ROCKET_SHOOT,
                    SoundSource.WEATHER,
                    0.8F, 0.6F
            );
        }
    }

    // =========================================================
    // 着弾
    // =========================================================

    private void impact() {

        impacted = true;

        if (!(this.level() instanceof ServerLevel sl)) {
            this.discard();
            return;
        }

        Vec3 pos = this.position();

        // 着弾演出
        sl.sendParticles(
                ParticleTypes.EXPLOSION_EMITTER,
                pos.x, pos.y, pos.z,
                1, 0, 0, 0, 0
        );

        sl.sendParticles(
                ParticleTypes.FIREWORK,
                pos.x, pos.y + 0.5, pos.z,
                60, 1.5, 1.5, 1.5, 0.3
        );

        sl.sendParticles(
                ParticleTypes.END_ROD,
                pos.x, pos.y + 0.5, pos.z,
                40, 1.5, 1.5, 1.5, 0.15
        );

        sl.sendParticles(
                ParticleTypes.ELECTRIC_SPARK,
                pos.x, pos.y + 0.5, pos.z,
                30, 1.2, 1.2, 1.2, 0.2
        );

        sl.sendParticles(
                ParticleTypes.FLASH,
                pos.x, pos.y + 0.5, pos.z,
                2, 0, 0, 0, 0
        );

        sl.playSound(
                null,
                pos.x, pos.y, pos.z,
                SoundEvents.GENERIC_EXPLODE,
                SoundSource.WEATHER,
                2.0F, 1.2F
        );

        sl.playSound(
                null,
                pos.x, pos.y, pos.z,
                SoundEvents.FIREWORK_ROCKET_LARGE_BLAST,
                SoundSource.WEATHER,
                2.0F, 1.0F
        );

        // =========================================================
        // 範囲ダメージ
        // =========================================================
        AABB area = new AABB(
                pos.x - IMPACT_RADIUS, pos.y - IMPACT_RADIUS, pos.z - IMPACT_RADIUS,
                pos.x + IMPACT_RADIUS, pos.y + IMPACT_RADIUS, pos.z + IMPACT_RADIUS
        );

        List<LivingEntity> targets = sl.getEntitiesOfClass(
                LivingEntity.class, area,
                e -> e.isAlive() && !e.isSpectator()
        );

        Entity owner = getOwnerEntity(sl);

        for (LivingEntity target : targets) {

            // 発動者は除外
            if (owner != null && target.getUUID().equals(owner.getUUID())) continue;

            target.invulnerableTime = 0;
            target.hurtTime = 0;

            DamageSource src = owner instanceof Player p
                    ? sl.damageSources().playerAttack(p)
                    : sl.damageSources().generic();

            target.hurt(src, damage);

            // ノックバック（上 + 外向き）
            Vec3 away = target.position().subtract(pos).normalize();
            target.push(
                    away.x * 1.8D,
                    0.9D,
                    away.z * 1.8D
            );
            target.hurtMarked = true;
        }

        // =========================================================
        // 発動者の回復
        // =========================================================
        if (owner instanceof Player player && player.isAlive()) {
            float newHealth = Math.min(
                    player.getMaxHealth(),
                    player.getHealth() + healAmount
            );
            player.setHealth(newHealth);

            sl.sendParticles(
                    ParticleTypes.HEART,
                    player.getX(), player.getY() + 1.5, player.getZ(),
                    3, 0.3, 0.3, 0.3, 0
            );
        }

        this.discard();
    }

    @Nullable
    private Entity getOwnerEntity(ServerLevel sl) {
        if (ownerUUID == null) return null;
        return sl.getEntity(ownerUUID);
    }

    // =========================================================
    // セーブ・同期
    // =========================================================

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        if (tag.hasUUID("Owner")) {
            this.ownerUUID = tag.getUUID("Owner");
        }
        this.targetY = tag.getDouble("TargetY");
        this.fallTicks = tag.getInt("FallTicks");
        this.elapsedTicks = tag.getInt("ElapsedTicks");
        this.damage = tag.getFloat("Damage");
        this.healAmount = tag.getFloat("HealAmount");
        this.impacted = tag.getBoolean("Impacted");
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        if (ownerUUID != null) tag.putUUID("Owner", ownerUUID);
        tag.putDouble("TargetY", targetY);
        tag.putInt("FallTicks", fallTicks);
        tag.putInt("ElapsedTicks", elapsedTicks);
        tag.putFloat("Damage", damage);
        tag.putFloat("HealAmount", healAmount);
        tag.putBoolean("Impacted", impacted);
    }

    @Override
    public Packet<ClientGamePacketListener> getAddEntityPacket() {
        return NetworkHooks.getEntitySpawningPacket(this);
    }
}