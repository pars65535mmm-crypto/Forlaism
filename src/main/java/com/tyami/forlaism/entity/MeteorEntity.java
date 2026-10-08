package com.tyami.forlaism.entity;

import com.tyami.forlaism.registry.ModEntityTypes;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.NetworkHooks;

/**
 * ヤツハニサクの隕石。
 *
 * 空高くに出現し、1分間その場に留まったあと、
 * 地表に向かって落下して大爆発する。
 *
 * モデルは無し。Renderer 側で発光する球体として描画する。
 */
public class MeteorEntity extends Entity {

    /** 落下までの待機tick。0.5分 = 600 tick。 */
    public static final int WAIT_TICKS = 600;

    /** 落下速度（ブロック/tick）。 */
    private static final double FALL_SPEED = 1.1D;

    /** 爆発威力（TNT 10個分くらい）。 */
    private static final float EXPLOSION_POWER = 120.0F;

    /** 爆発半径（破壊範囲）。 */
    private static final double EXPLOSION_RADIUS = 100.0D;

    /** 待機中tick。 */
    private int waitTicks = WAIT_TICKS;

    /** 落下モードに入ったか。 */
    private boolean falling = false;

    /** 発光するか（描画用）。 */
    private boolean glowing = true;

    public MeteorEntity(EntityType<?> type, Level level) {
        super(type, level);
        this.noPhysics = true;
        this.setNoGravity(true);
        this.setInvulnerable(true);
    }

    public MeteorEntity(Level level, double x, double y, double z) {
        this(ModEntityTypes.METEOR.get(), level);
        this.setPos(x, y, z);
    }

    // =========================================================
    // 同期データ（使わないが defineSynchedData は必須）
    // =========================================================

    @Override
    protected void defineSynchedData() {
        // 何も同期しない
    }

    // =========================================================
    // tick
    // =========================================================

    @Override
    public void tick() {
        super.tick();

        if (this.level().isClientSide) {
            clientTick();
            return;
        }

        // サーバー側
        if (!falling) {
            waitTicks--;

            // 周囲に火の粉を撒く
            if (this.tickCount % 4 == 0) {
                spawnAmbientParticles();
            }

            // 時々ゴゴゴという音
            if (waitTicks % 200 == 0 && waitTicks > 0) {
                this.level().playSound(
                        null,
                        this.getX(), this.getY(), this.getZ(),
                        SoundEvents.ENDER_DRAGON_GROWL,
                        SoundSource.WEATHER,
                        2.0F, 0.5F
                );
            }

            if (waitTicks <= 0) {
                // 落下開始
                falling = true;
                this.level().playSound(
                        null,
                        this.getX(), this.getY(), this.getZ(),
                        SoundEvents.WITHER_SPAWN,
                        SoundSource.WEATHER,
                        2.0F, 0.6F
                );
            }

        } else {
            // 落下中
            this.setPos(
                    this.getX(),
                    this.getY() - FALL_SPEED,
                    this.getZ()
            );

            // 落下中のトレイル
            spawnFallingTrail();

            // 地表到達チェック
            if (!this.level().isEmptyBlock(this.blockPosition())) {
                explode();
            }

            // 念のため、Y=-64以下でも爆発
            if (this.getY() < this.level().getMinBuildHeight()) {
                explode();
            }
        }
    }

    /**
     * クライアント側のパーティクル。
     */
    private void clientTick() {
        // 火の粉
        for (int i = 0; i < 3; i++) {
            this.level().addParticle(
                    ParticleTypes.FLAME,
                    this.getX() + (this.random.nextDouble() - 0.5) * 2.0,
                    this.getY() + (this.random.nextDouble() - 0.5) * 2.0,
                    this.getZ() + (this.random.nextDouble() - 0.5) * 2.0,
                    0, 0, 0
            );
        }

        // 煙
        this.level().addParticle(
                ParticleTypes.LARGE_SMOKE,
                this.getX() + (this.random.nextDouble() - 0.5) * 1.5,
                this.getY() + (this.random.nextDouble() - 0.5) * 1.5,
                this.getZ() + (this.random.nextDouble() - 0.5) * 1.5,
                0, 0.05, 0
        );

        // 光
        this.level().addParticle(
                ParticleTypes.FLASH,
                this.getX(), this.getY(), this.getZ(),
                0, 0, 0
        );
    }

    // =========================================================
    // パーティクル
    // =========================================================

    private void spawnAmbientParticles() {
        if (!(this.level() instanceof ServerLevel sl)) return;

        sl.sendParticles(
                ParticleTypes.FLAME,
                this.getX(), this.getY(), this.getZ(),
                20, 2.5, 2.5, 2.5, 0.05
        );
        sl.sendParticles(
                ParticleTypes.LARGE_SMOKE,
                this.getX(), this.getY(), this.getZ(),
                10, 2.0, 2.0, 2.0, 0.02
        );
    }

    private void spawnFallingTrail() {
        if (!(this.level() instanceof ServerLevel sl)) return;

        sl.sendParticles(
                ParticleTypes.FLAME,
                this.getX(), this.getY() + 1, this.getZ(),
                15, 1.5, 2.5, 1.5, 0.1
        );
        sl.sendParticles(
                ParticleTypes.LAVA,
                this.getX(), this.getY() + 1, this.getZ(),
                5, 1.0, 2.0, 1.0, 0.05
        );
    }

    // =========================================================
    // 爆発
    // =========================================================

        private void explode() {
        if (!(this.level() instanceof ServerLevel sl)) return;

        // =========================================================
        // ① メイン爆発（超ド級）
        // =========================================================
        sl.explode(
                this,
                this.getX(),
                this.getY(),
                this.getZ(),
                EXPLOSION_POWER,
                Level.ExplosionInteraction.TNT
        );

        // =========================================================
        // ② 多重爆発（周囲にも連鎖爆発）
        // =========================================================
        for (int i = 0; i < 8; i++) {
            double angle = (i / 8.0) * Math.PI * 2.0;
            double r = 12.0D;
            double ox = Math.cos(angle) * r;
            double oz = Math.sin(angle) * r;

            sl.explode(
                    this,
                    this.getX() + ox,
                    this.getY() + 2,
                    this.getZ() + oz,
                    EXPLOSION_POWER * 0.4F,
                    Level.ExplosionInteraction.TNT
            );
        }

        // =========================================================
        // ③ クレーター形成（地表を削る）
        // =========================================================
        int craterRadius = 25;
        int craterDepth = 15;

        for (int dx = -craterRadius; dx <= craterRadius; dx++) {
            for (int dz = -craterRadius; dz <= craterRadius; dz++) {
                double distSqr = dx * dx + dz * dz;
                if (distSqr > craterRadius * craterRadius) continue;

                // 距離に応じて深さを変える（中心ほど深い）
                double distRatio = Math.sqrt(distSqr) / craterRadius;
                int depth = (int) (craterDepth * (1.0 - distRatio));

                for (int dy = 0; dy < depth; dy++) {
                    BlockPos pos = BlockPos.containing(
                            this.getX() + dx,
                            this.getY() - dy,
                            this.getZ() + dz
                    );

                    // Fstone は壊さない（夢だから）
                    var state = sl.getBlockState(pos);
                    if (state.is(com.tyami.forlaism.registry.Blocks.FSTONE.get())) {
                        continue;
                    }

                    // 岩盤も壊さない
                    if (state.is(net.minecraft.world.level.block.Blocks.BEDROCK)) {
                        continue;
                    }

                    sl.destroyBlock(pos, false);
                }
            }
        }

        // =========================================================
        // ④ 炎上（周囲に火を撒く）
        // =========================================================
        for (int dx = -craterRadius; dx <= craterRadius; dx++) {
            for (int dz = -craterRadius; dz <= craterRadius; dz++) {
                for (int dy = -3; dy < 3; dy++) {
                    BlockPos pos = BlockPos.containing(
                            this.getX() + dx,
                            this.getY() + dy,
                            this.getZ() + dz
                    );

                    if (sl.getBlockState(pos).isAir()
                            && sl.getBlockState(pos.below()).isSolidRender(sl, pos.below())) {
                        sl.setBlock(pos, net.minecraft.world.level.block.Blocks.FIRE.defaultBlockState(), 3);
                        break; // 1列1火で十分
                    }
                }
            }
        }

        // =========================================================
        // ⑤ 衝撃波（範囲内の生き物を吹き飛ばす）
        // =========================================================
        AABB shockwave = new AABB(
                this.getX() - EXPLOSION_RADIUS,
                this.getY() - 20,
                this.getZ() - EXPLOSION_RADIUS,
                this.getX() + EXPLOSION_RADIUS,
                this.getY() + 20,
                this.getZ() + EXPLOSION_RADIUS
        );

        for (LivingEntity target : sl.getEntitiesOfClass(LivingEntity.class, shockwave)) {
            // 吹っ飛ばす
            Vec3 dir = target.position().subtract(this.position()).normalize();
            target.setDeltaMovement(
                    dir.x * 5.0D,
                    3.0D,
                    dir.z * 5.0D
            );
            target.hurtMarked = true;

            // 大ダメージ
            target.invulnerableTime = 0;
            target.hurt(
                    this.damageSources().explosion(this, this),
                    500.0F
            );
        }

        // =========================================================
        // ⑥ 演出（派手に）
        // =========================================================
        sl.sendParticles(
                ParticleTypes.EXPLOSION_EMITTER,
                this.getX(), this.getY(), this.getZ(),
                50, 10.0, 5.0, 10.0, 0
        );

        sl.sendParticles(
                ParticleTypes.FLAME,
                this.getX(), this.getY(), this.getZ(),
                500, 20.0, 10.0, 20.0, 0.5
        );

        sl.sendParticles(
                ParticleTypes.LARGE_SMOKE,
                this.getX(), this.getY(), this.getZ(),
                300, 20.0, 10.0, 20.0, 0.2
        );

        sl.sendParticles(
                ParticleTypes.LAVA,
                this.getX(), this.getY(), this.getZ(),
                100, 15.0, 8.0, 15.0, 0.3
        );

        // 音（重ねがけ）
        sl.playSound(
                null,
                this.getX(), this.getY(), this.getZ(),
                SoundEvents.GENERIC_EXPLODE,
                SoundSource.WEATHER,
                10.0F, 0.3F
        );

        sl.playSound(
                null,
                this.getX(), this.getY(), this.getZ(),
                SoundEvents.DRAGON_FIREBALL_EXPLODE,
                SoundSource.WEATHER,
                10.0F, 0.5F
        );

        sl.playSound(
                null,
                this.getX(), this.getY(), this.getZ(),
                SoundEvents.LIGHTNING_BOLT_THUNDER,
                SoundSource.WEATHER,
                8.0F, 0.4F
        );

        sl.playSound(
                null,
                this.getX(), this.getY(), this.getZ(),
                SoundEvents.WITHER_SPAWN,
                SoundSource.WEATHER,
                5.0F, 0.5F
        );

        // =========================================================
        // 消滅
        // =========================================================
        this.discard();
    }

    // =========================================================
    // その他
    // =========================================================

    @Override
    public boolean isPickable() {
        return false;
    }

    @Override
    public boolean isAttackable() {
        return false;
    }

    @Override
    public boolean canBeCollidedWith() {
        return false;
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        this.waitTicks = tag.getInt("WaitTicks");
        this.falling = tag.getBoolean("Falling");
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        tag.putInt("WaitTicks", this.waitTicks);
        tag.putBoolean("Falling", this.falling);
    }

    @Override
    public Packet<ClientGamePacketListener> getAddEntityPacket() {
        return NetworkHooks.getEntitySpawningPacket(this);
    }
}