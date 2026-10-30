package com.tyami.forlaism.entity;

import com.tyami.forlaism.network.FactotumPacketHandler;
import com.tyami.forlaism.network.JillstlairleatWaveTextPacket;
import com.tyami.forlaism.registry.ModEntityTypes;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraftforge.network.NetworkHooks;
import net.minecraftforge.network.PacketDistributor;

/**
 * 耐久値-1のジルスレイトで20回Shift+右クリックした時に出現する演出用Entity。
 *
 * 【演出タイムライン】
 *  - 0 ~ 3秒 (0 ~ 60t): 乱回転
 *  - 3 ~ 6秒 (60 ~ 120t): 光り出す
 *  - 6 ~ 9秒 (120 ~ 180t): アイテムから抜け出し空中へ浮上
 *  - 9秒 (180t): 自ら地面にブッ刺さる
 *  - 以降: 永久に刺さったまま固定され、引き抜くことはできない
 */
public class StuckJillstlairleatEntity extends Entity {

    private static final EntityDataAccessor<Integer> ANIM_TICKS =
            SynchedEntityData.defineId(StuckJillstlairleatEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Boolean> IS_STUCK =
            SynchedEntityData.defineId(StuckJillstlairleatEntity.class, EntityDataSerializers.BOOLEAN);

    public StuckJillstlairleatEntity(EntityType<?> type, Level level) {
        super(type, level);
        this.noPhysics = true;
    }

    public StuckJillstlairleatEntity(Level level, double x, double y, double z) {
        this(ModEntityTypes.STUCK_JILLSTLAIRLEAT.get(), level);
        this.setPos(x, y, z);
    }

    @Override
    protected void defineSynchedData() {
        this.entityData.define(ANIM_TICKS, 0);
        this.entityData.define(IS_STUCK, false);
    }

    public int getAnimTicks() {
        return this.entityData.get(ANIM_TICKS);
    }

    public boolean isStuck() {
        return this.entityData.get(IS_STUCK);
    }

    @Override
    public void tick() {
        super.tick();

        int ticks = getAnimTicks();
        if (!isStuck()) {
            ticks++;
            if (!this.level().isClientSide) {
                this.entityData.set(ANIM_TICKS, ticks);
            }

            // 3秒後 (60tick): 光り出すパーティクル
            if (ticks >= 60 && ticks < 180) {
                if (this.level().isClientSide) {
                    for (int i = 0; i < 3; i++) {
                        this.level().addParticle(
                                ParticleTypes.GLOW,
                                this.getX() + (random.nextDouble() - 0.5) * 0.6,
                                this.getY() + (random.nextDouble() - 0.5) * 0.6 + 0.3,
                                this.getZ() + (random.nextDouble() - 0.5) * 0.6,
                                0, 0.05, 0
                        );
                    }
                }
            }

            // 6秒後 (120tick): アイテムから抜け出し浮上
            if (ticks >= 120 && ticks < 180) {
                if (!this.level().isClientSide) {
                    this.setPos(this.getX(), this.getY() + 0.02, this.getZ());
                }
            }

            // 9秒後 (180tick): 地面にブッ刺さる！
            if (ticks >= 180) {
                if (!this.level().isClientSide) {
                    this.entityData.set(IS_STUCK, true);
                    this.level().playSound(
                            null, this.getX(), this.getY(), this.getZ(),
                            SoundEvents.ANVIL_LAND, SoundSource.PLAYERS, 1.2F, 0.8F
                    );
                    this.level().playSound(
                            null, this.getX(), this.getY(), this.getZ(),
                            SoundEvents.TRIDENT_HIT_GROUND, SoundSource.PLAYERS, 1.5F, 0.5F
                    );
                } else {
                    // 刺さった瞬間の衝撃パーティクル
                    for (int i = 0; i < 20; i++) {
                        this.level().addParticle(
                                ParticleTypes.SOUL_FIRE_FLAME,
                                this.getX() + (random.nextDouble() - 0.5) * 1.0,
                                this.getY() + 0.1,
                                this.getZ() + (random.nextDouble() - 0.5) * 1.0,
                                (random.nextDouble() - 0.5) * 0.2, 0.1, (random.nextDouble() - 0.5) * 0.2
                        );
                    }
                }
            }
        } else {
            // 刺さった後: 常時静止
            this.setDeltaMovement(0, 0, 0);
        }
    }

    /**
     * 刺さった後に右クリックしても引き抜けない。
     */
    @Override
    public InteractionResult interact(Player player, InteractionHand hand) {
        if (!this.level().isClientSide && isStuck()) {
            if (player instanceof ServerPlayer sp) {
                FactotumPacketHandler.CHANNEL.send(
                        PacketDistributor.PLAYER.with(() -> sp),
                        new JillstlairleatWaveTextPacket("決して引き抜くことはできない..", 60)
                );
            }
        }
        return InteractionResult.sidedSuccess(this.level().isClientSide);
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        // 完全無敵（何があっても破壊されない）
        return false;
    }

    @Override
    public boolean isPickable() {
        return true;
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        this.entityData.set(ANIM_TICKS, tag.getInt("AnimTicks"));
        this.entityData.set(IS_STUCK, tag.getBoolean("IsStuck"));
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        tag.putInt("AnimTicks", getAnimTicks());
        tag.putBoolean("IsStuck", isStuck());
    }

    @Override
    public Packet<ClientGamePacketListener> getAddEntityPacket() {
        return NetworkHooks.getEntitySpawningPacket(this);
    }
}
