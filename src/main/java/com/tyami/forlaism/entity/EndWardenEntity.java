package com.tyami.forlaism.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.BossEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.AnimationState;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.control.FlyingMoveControl;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.RandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.ai.navigation.FlyingPathNavigation;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;
import java.util.List;

/**
 * EndWarden。
 *
 * 旧 MCreator 実装を軽量化・最適化して 1.20.1 Forge に移植。
 *
 * ─ スペック ────────────────────────────────────
 *   HP             : 1024
 *   防御           : 20
 *   攻撃力         : 24
 *   移動速度       : 0.4
 *   飛行速度       : 0.5
 *   追跡範囲       : 129
 *   ノックバック耐性: 2.0
 *   攻撃KB         : 2.0
 *   XP             : 1024
 *   BossBar        : 青 / PROGRESS
 *
 * ─ フェーズ ────────────────────────────────────
 *   IDLE        : 徘徊（ターゲットを常に視認）
 *   MELEE       : 近接攻撃（24ダメージ）
 *   SONIC_BOOM  : ビーム発射（5+5+5ダメージ + 暗黒+鈍足III）
 *   DIVE        : 突進（10ダメージ）
 *   DEATH       : 死亡演出
 */
public class EndWardenEntity extends PathfinderMob {

    // =========================================================
    // 同期データ
    // =========================================================

    public static final EntityDataAccessor<Boolean> DATA_AAA =
            SynchedEntityData.defineId(EndWardenEntity.class, EntityDataSerializers.BOOLEAN);

    public static final EntityDataAccessor<Boolean> DATA_SONIC =
            SynchedEntityData.defineId(EndWardenEntity.class, EntityDataSerializers.BOOLEAN);

    public static final EntityDataAccessor<Integer> DATA_BEAM_PROGRESS =
            SynchedEntityData.defineId(EndWardenEntity.class, EntityDataSerializers.INT);

    public static final EntityDataAccessor<Integer> DATA_DEATH_STAGE =
            SynchedEntityData.defineId(EndWardenEntity.class, EntityDataSerializers.INT);

    /** フェーズ（ordinal）をクライアントと同期。 */
    public static final EntityDataAccessor<Integer> DATA_PHASE =
            SynchedEntityData.defineId(EndWardenEntity.class, EntityDataSerializers.INT);

    // =========================================================
    // アニメーション（クライアント側）
    // =========================================================

    public final AnimationState idleAnimationState = new AnimationState();
    public final AnimationState walkAnimationState = new AnimationState();
    public final AnimationState attackAnimationState = new AnimationState();
    public final AnimationState sonicBoomAnimationState = new AnimationState();

    /** クライアント側で最後にアニメを切り替えたフェーズ。 */
    @Nullable
    private EndWardenAttackPhase lastAnimPhase = null;

    // =========================================================
    // 定数
    // =========================================================

    private static final double BEAM_RANGE = 30.0D;
    private static final double BEAM_STEP_PER_TICK = 1.0D;
    private static final double BEAM_HIT_RADIUS = 1.5D;
    private static final float BEAM_HIT_DAMAGE = 5.0F;
    private static final int DEBUFF_DURATION = 120;
    private static final int DEBUFF_AMPLIFIER = 2;

    private static final float DIVE_HIT_DAMAGE = 10.0F;
    private static final double DIVE_SPEED = 1.2D;

    private static final double SONIC_RANGE_MIN = 7.0D;
    private static final double SONIC_RANGE_MAX = 30.0D;
    private static final double DIVE_RANGE_MIN = 5.0D;
    private static final double DIVE_RANGE_MAX = 30.0D;

    /** ソニックブームのチャージ時間（tick）。 */
    private static final int SONIC_CHARGE_TICKS = 30;

    // =========================================================
    // フェーズ状態
    // =========================================================

    private EndWardenAttackPhase phase = EndWardenAttackPhase.IDLE;
    private int phaseTick = 0;

        /** 現在フェーズの残りtickを取得（サブクラス用）。 */
    protected int getPhaseTick() {
        return this.phaseTick;
    }

    /** フェーズを強制変更（サブクラス用）。 */
    protected void applyPhase(EndWardenAttackPhase newPhase, int duration) {
        setPhase(newPhase, duration);
    }

    /** フェーズ処理の tick（サブクラスでオーバーライド可能）。 */
    protected void tickPhase(ServerLevel level) {
        // 既存の tickPhase の中身を protected に変更
        switch (this.phase) {
            case SONIC_BOOM -> tickSonicBoom(level);
            case DIVE -> tickDive(level);
            case MELEE -> tickMelee(level);
            default -> tickIdle(level);
        }
    }

    /** ビームダメージ（サブクラスでオーバーライド可能）。 */
    protected void applyBeamDamage(ServerLevel level, Vec3 point) {
        // 既存の applyBeamDamage の中身を protected に
        AABB box = new AABB(
                point.x - BEAM_HIT_RADIUS, point.y - BEAM_HIT_RADIUS, point.z - BEAM_HIT_RADIUS,
                point.x + BEAM_HIT_RADIUS, point.y + BEAM_HIT_RADIUS, point.z + BEAM_HIT_RADIUS
        );

        List<LivingEntity> hits = level.getEntitiesOfClass(
                LivingEntity.class,
                box,
                e -> e.isAlive() && e != this && !e.isSpectator()
        );

        for (LivingEntity hit : hits) {
            hit.invulnerableTime = 0;
            hit.hurt(createAbyssalDamage(level), BEAM_HIT_DAMAGE);

            hit.invulnerableTime = 0;
            hit.hurt(level.damageSources().sonicBoom(this), BEAM_HIT_DAMAGE);

            hit.invulnerableTime = 0;
            hit.hurt(createDualWieldingDamage(level), BEAM_HIT_DAMAGE);

            hit.addEffect(new MobEffectInstance(
                    MobEffects.DARKNESS,
                    DEBUFF_DURATION,
                    DEBUFF_AMPLIFIER,
                    false, false
            ));
            hit.addEffect(new MobEffectInstance(
                    MobEffects.MOVEMENT_SLOWDOWN,
                    DEBUFF_DURATION,
                    DEBUFF_AMPLIFIER,
                    false, false
            ));
        }
    }

    @Nullable
    private LivingEntity lockedTarget = null;

    private int chargeTick = 0;

    // =========================================================
    // BossBar
    // =========================================================

    private final ServerBossEvent bossInfo =
            new ServerBossEvent(
                    this.getDisplayName(),
                    BossEvent.BossBarColor.BLUE,
                    BossEvent.BossBarOverlay.PROGRESS
            );

    // =========================================================
    // コンストラクタ
    // =========================================================

    public EndWardenEntity(EntityType<? extends PathfinderMob> type, Level level) {
        super(type, level);
        this.xpReward = 1024;
        this.setPersistenceRequired();
        this.moveControl = new FlyingMoveControl(this, 10, true);
        this.setMaxUpStep(3.0F);
    }

    // =========================================================
    // 属性
    // =========================================================

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 1024.0D)
                .add(Attributes.ARMOR, 20.0D)
                .add(Attributes.ATTACK_DAMAGE, 24.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.4D)
                .add(Attributes.FLYING_SPEED, 0.5D)
                .add(Attributes.FOLLOW_RANGE, 129.0D)
                .add(Attributes.KNOCKBACK_RESISTANCE, 2.0D)
                .add(Attributes.ATTACK_KNOCKBACK, 2.0D);
    }

    // =========================================================
    // 同期データ定義
    // =========================================================

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(DATA_AAA, false);
        this.entityData.define(DATA_SONIC, false);
        this.entityData.define(DATA_BEAM_PROGRESS, 0);
        this.entityData.define(DATA_DEATH_STAGE, 0);
        this.entityData.define(DATA_PHASE, 0);
    }

    // =========================================================
    // ナビゲーション
    // =========================================================

    @Override
    protected PathNavigation createNavigation(Level level) {
        FlyingPathNavigation nav = new FlyingPathNavigation(this, level);
        nav.setCanOpenDoors(false);
        nav.setCanFloat(true);
        return nav;
    }

    // =========================================================
    // AI Goals
    // =========================================================

    @Override
    protected void registerGoals() {
        // 近接攻撃（高速・視界外も追跡）
        this.goalSelector.addGoal(1, new MeleeAttackGoal(this, 1.5D, true));

        // 徘徊
        this.goalSelector.addGoal(2, new RandomStrollGoal(this, 1.0D, 20) {
            @Override
            protected Vec3 getPosition() {
                RandomSource random = EndWardenEntity.this.getRandom();
                double dx = EndWardenEntity.this.getX() + (random.nextFloat() * 2.0F - 1.0F) * 16.0F;
                double dy = EndWardenEntity.this.getY() + (random.nextFloat() * 2.0F - 1.0F) * 16.0F;
                double dz = EndWardenEntity.this.getZ() + (random.nextFloat() * 2.0F - 1.0F) * 16.0F;
                return new Vec3(dx, dy, dz);
            }
        });

        this.targetSelector.addGoal(3, new NearestAttackableTargetGoal<>(this, Player.class, false, false));
        this.targetSelector.addGoal(4, new HurtByTargetGoal(this));

        this.goalSelector.addGoal(5, new RandomLookAroundGoal(this));
        this.goalSelector.addGoal(6, new FloatGoal(this));
    }

    // =========================================================
    // 基本設定
    // =========================================================

    @Override
    public boolean removeWhenFarAway(double distance) {
        return false;
    }

    @Override
    public boolean causeFallDamage(float fallDistance, float multiplier, DamageSource source) {
        return false;
    }

    @Override
    protected void checkFallDamage(double y, boolean onGround, BlockState state, BlockPos pos) {
        // 落下ダメージ無効
    }

    @Override
    public boolean fireImmune() {
        return true;
    }

    @Override
    public void aiStep() {
        super.aiStep();
        this.setNoGravity(true);
    }

    // =========================================================
    // 無効ダメージ
    // =========================================================

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (source.is(DamageTypes.IN_FIRE)) return false;
        if (source.is(DamageTypes.FALL)) return false;
        if (source.is(DamageTypes.CACTUS)) return false;
        if (source.is(DamageTypes.DROWN)) return false;
        if (source.is(DamageTypes.DRAGON_BREATH)) return false;
        if (source.is(DamageTypes.WITHER)) return false;
        if (source.is(DamageTypes.WITHER_SKULL)) return false;

        return super.hurt(source, amount);
    }

    // =========================================================
    // サウンド
    // =========================================================

    @Nullable
    @Override
    protected SoundEvent getAmbientSound() {
        return SoundEvents.WARDEN_AMBIENT;
    }

    @Nullable
    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.ENDER_DRAGON_HURT;
    }

    @Nullable
    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.ENDER_DRAGON_DEATH;
    }

    // =========================================================
    // BossBar
    // =========================================================

    @Override
    public void startSeenByPlayer(ServerPlayer player) {
        super.startSeenByPlayer(player);
        this.bossInfo.addPlayer(player);
    }

    @Override
    public void stopSeenByPlayer(ServerPlayer player) {
        super.stopSeenByPlayer(player);
        this.bossInfo.removePlayer(player);
    }

    @Override
    protected void customServerAiStep() {
        super.customServerAiStep();
        this.bossInfo.setProgress(this.getHealth() / this.getMaxHealth());
    }

    // =========================================================
    // tick
    // =========================================================

    @Override
    public void tick() {
        super.tick();

        // クライアント側はアニメ更新のみ
        if (this.level().isClientSide) {
            updateAnimationStates();
            return;
        }

        // サーバー側：フェーズ進行
        if (this.phaseTick > 0) {
            this.phaseTick--;
            tickPhase((ServerLevel) this.level());
        } else {
            selectNextPhase();
        }
    }

    // =========================================================
    // アニメーション（クライアント）
    // =========================================================

    /**
     * フェーズが変化した時だけアニメを切り替える。
     *
     * 毎tick startIfStopped すると開幕連打になるので、
     * lastAnimPhase で変化検出する。
     */
    private void updateAnimationStates() {
        EndWardenAttackPhase currentPhase = getPhase();

        if (currentPhase == this.lastAnimPhase) {
            return;
        }
        this.lastAnimPhase = currentPhase;

        // 全停止
        this.idleAnimationState.stop();
        this.walkAnimationState.stop();
        this.attackAnimationState.stop();
        this.sonicBoomAnimationState.stop();

        // フェーズに応じて再生開始
        switch (currentPhase) {
            case SONIC_BOOM -> this.sonicBoomAnimationState.start(this.tickCount);
            case MELEE -> this.attackAnimationState.start(this.tickCount);
            case DIVE, WALK -> this.walkAnimationState.start(this.tickCount);
            default -> this.idleAnimationState.start(this.tickCount);
        }
    }

    // =========================================================
    // フェーズ選択
    // =========================================================

    /**
     * 次のフェーズを選択する。
     *
     * 旧実装の「1/240, 1/200, 1/3」を、体感できる確率に調整。
     */
    private void selectNextPhase() {
        LivingEntity target = this.getTarget();

        // ターゲットなし
        if (target == null || !target.isAlive()) {
            setPhase(EndWardenAttackPhase.IDLE, 10);
            return;
        }

        double dist = this.distanceTo(target);

        // ---- 遠距離：ソニックブーム（1/3） ----
        if (dist >= SONIC_RANGE_MIN && dist <= SONIC_RANGE_MAX) {
            if (this.random.nextInt(3) == 0) {
                setPhase(EndWardenAttackPhase.SONIC_BOOM,
                        EndWardenAttackPhase.SONIC_BOOM.getBaseDurationTicks());
                return;
            }
        }

        // ---- 中距離：突進（1/2） ----
        if (dist >= DIVE_RANGE_MIN && dist <= DIVE_RANGE_MAX) {
            if (this.random.nextInt(2) == 0) {
                setPhase(EndWardenAttackPhase.DIVE,
                        EndWardenAttackPhase.DIVE.getBaseDurationTicks());
                return;
            }
        }

        // ---- 近距離：近接 ----
        if (dist <= 6.0D) {
            setPhase(EndWardenAttackPhase.MELEE,
                    EndWardenAttackPhase.MELEE.getBaseDurationTicks());
            return;
        }

        // ---- 何もしない ----
        setPhase(EndWardenAttackPhase.IDLE, 10);
    }

    /**
     * フェーズを切り替える。
     */
    protected void setPhase(EndWardenAttackPhase newPhase, int duration) {
        this.phase = newPhase;
        this.phaseTick = duration;

        // クライアントに同期（変化時のみ）
        if (this.entityData.get(DATA_PHASE) != newPhase.ordinal()) {
            this.entityData.set(DATA_PHASE, newPhase.ordinal());
        }

        // フラグリセット
        setFlagSonic(false);
        setFlagDive(false);
        setBeamProgress(0);

        onPhaseStart(newPhase);
    }

    /**
     * フェーズ開始時の初期化。
     */
    private void onPhaseStart(EndWardenAttackPhase newPhase) {
        this.lockedTarget = this.getTarget();

        if (this.level().isClientSide) return;
        ServerLevel level = (ServerLevel) this.level();

        switch (newPhase) {
            case SONIC_BOOM -> {
                if (this.lockedTarget != null) {
                    lookAtTarget(this.lockedTarget);
                }
                setFlagSonic(true);
                this.chargeTick = SONIC_CHARGE_TICKS;

                level.playSound(null, this.getX(), this.getY(), this.getZ(),
                        SoundEvents.WARDEN_SONIC_CHARGE, SoundSource.HOSTILE, 2.0F, 1.0F);
            }
            case DIVE -> {
                setFlagDive(true);
                level.playSound(null, this.getX(), this.getY(), this.getZ(),
                        SoundEvents.WARDEN_ROAR, SoundSource.HOSTILE, 2.0F, 1.2F);
            }
            case MELEE -> {
                level.playSound(null, this.getX(), this.getY(), this.getZ(),
                        SoundEvents.WARDEN_ATTACK_IMPACT, SoundSource.HOSTILE, 1.5F, 1.0F);
            }
            default -> {}
        }
    }

    // =========================================================
    // フェーズ別処理
    // =========================================================
/* 
    protected void tickPhase(ServerLevel level) {
        switch (this.phase) {
            case SONIC_BOOM -> tickSonicBoom(level);
            case DIVE -> tickDive(level);
            case MELEE -> tickMelee(level);
            default -> tickIdle(level);
        }
    }
        */

    /**
     * 待機フェーズ。
     * ターゲットがいれば常にそちらを向く。
     */
    protected void tickIdle(ServerLevel level) {
        LivingEntity target = this.getTarget();
        if (target != null) {
            this.getLookControl().setLookAt(target, 30.0F, 30.0F);
        }

        if (this.tickCount % 5 == 0) {
            EndWardenParticleHelper.spawnAmbient(level, this.position());
        }
    }

    /**
     * 近接フェーズ。
     * MeleeAttackGoal が動かすので、ここでは視点ロックのみ。
     */
    protected void tickMelee(ServerLevel level) {
        LivingEntity target = this.getTarget();
        if (target != null) {
            this.getLookControl().setLookAt(target, 30.0F, 30.0F);
        }
    }

    /**
     * ソニックブームフェーズ。
     *
     * 0 ～ 30  : チャージ
     * 30 ～ 60 : ビーム伸長
     */
    protected void tickSonicBoom(ServerLevel level) {
        LivingEntity target = this.lockedTarget;

        if (target == null || !target.isAlive()) {
            this.phaseTick = 0;
            return;
        }

        Vec3 eye = this.getEyePosition();

        // ---- チャージ ----
        if (this.chargeTick > 0) {
            this.chargeTick--;
            float progress = 1.0F - (this.chargeTick / (float) SONIC_CHARGE_TICKS);

            lookAtTarget(target);

            Vec3 look = this.getLookAngle().normalize();
            EndWardenParticleHelper.spawnCharge(level, eye, look, progress);

            if (this.chargeTick == 0) {
                level.playSound(null, this.getX(), this.getY(), this.getZ(),
                        SoundEvents.WARDEN_SONIC_BOOM, SoundSource.HOSTILE, 3.0F, 1.0F);
            }
            return;
        }

        // ---- ビーム伸長 ----
        Vec3 look = this.getLookAngle().normalize();
        int progress = this.getBeamProgress();

        // ビームを進行度まで描画
        EndWardenParticleHelper.spawnBeam(level, eye, look, BEAM_RANGE, BEAM_STEP_PER_TICK, progress);

        // 現在の先端位置で判定
        Vec3 point = eye.add(look.scale(progress));
        applyBeamDamage(level, point);

        // 進行
        setBeamProgress(progress + 1);

        // 最大到達でフェーズ終了
        if (progress >= BEAM_RANGE) {
            this.phaseTick = 0;
        }
    }

    /**
     * 突進フェーズ。
     */
    protected void tickDive(ServerLevel level) {
        LivingEntity target = this.lockedTarget;

        if (target == null || !target.isAlive()) {
            this.phaseTick = 0;
            return;
        }

        // ターゲット方向へ加速
        Vec3 dir = target.position().subtract(this.position()).normalize();
        double dist = this.distanceTo(target);
        double speed = dist > 10 ? DIVE_SPEED * 1.5 : DIVE_SPEED;

        this.setDeltaMovement(dir.scale(speed));
        this.hurtMarked = true;

        // 軌跡
        EndWardenParticleHelper.spawnDiveTrail(level, this.position());

        // 接触判定（少し広め）
        AABB hitBox = this.getBoundingBox().inflate(2.0D);
        List<LivingEntity> hits = level.getEntitiesOfClass(
                LivingEntity.class,
                hitBox,
                e -> e.isAlive() && e != this && !e.isSpectator()
        );

        for (LivingEntity hit : hits) {
            hit.invulnerableTime = 0;
            hit.hurt(this.damageSources().mobAttack(this), DIVE_HIT_DAMAGE);
            EndWardenParticleHelper.spawnDiveImpact(level, hit.position());

            // ヒットしても即終了せず余韻
            this.phaseTick = Math.min(this.phaseTick, 5);
            return;
        }
    }

    // =========================================================
    // ビームダメージ
    // =========================================================
/* 
    private void applyBeamDamage(ServerLevel level, Vec3 point) {
        AABB box = new AABB(
                point.x - BEAM_HIT_RADIUS, point.y - BEAM_HIT_RADIUS, point.z - BEAM_HIT_RADIUS,
                point.x + BEAM_HIT_RADIUS, point.y + BEAM_HIT_RADIUS, point.z + BEAM_HIT_RADIUS
        );

        List<LivingEntity> hits = level.getEntitiesOfClass(
                LivingEntity.class,
                box,
                e -> e.isAlive() && e != this && !e.isSpectator()
        );

        for (LivingEntity hit : hits) {
            // 3種ダメージ
            hit.invulnerableTime = 0;
            hit.hurt(createAbyssalDamage(level), BEAM_HIT_DAMAGE);

            hit.invulnerableTime = 0;
            hit.hurt(level.damageSources().sonicBoom(this), BEAM_HIT_DAMAGE);

            hit.invulnerableTime = 0;
            hit.hurt(createDualWieldingDamage(level), BEAM_HIT_DAMAGE);

            // デバフ
            hit.addEffect(new MobEffectInstance(
                    MobEffects.DARKNESS,
                    DEBUFF_DURATION,
                    DEBUFF_AMPLIFIER,
                    false, false
            ));
            hit.addEffect(new MobEffectInstance(
                    MobEffects.MOVEMENT_SLOWDOWN,
                    DEBUFF_DURATION,
                    DEBUFF_AMPLIFIER,
                    false, false
            ));
        }
    }
        */

    protected DamageSource createAbyssalDamage(ServerLevel level) {
        return new DamageSource(
                level.registryAccess()
                        .registryOrThrow(Registries.DAMAGE_TYPE)
                        .getHolderOrThrow(ResourceKey.create(
                                Registries.DAMAGE_TYPE,
                                new ResourceLocation("forlaism", "abyssal_darkness")
                        )),
                this
        );
    }

    protected DamageSource createDualWieldingDamage(ServerLevel level) {
        return new DamageSource(
                level.registryAccess()
                        .registryOrThrow(Registries.DAMAGE_TYPE)
                        .getHolderOrThrow(ResourceKey.create(
                                Registries.DAMAGE_TYPE,
                                new ResourceLocation("forlaism", "dual_wielding_mastery_ddd")
                        )),
                this
        );
    }

    // =========================================================
    // 死亡演出
    // =========================================================

    @Override
    public void die(DamageSource source) {

        // 1回目：HP1024で復活
        if (getDeathStage() == 0) {
            setDeathStage(1);
            this.setHealth(1024.0F);

            if (this.level() instanceof ServerLevel sl) {
                EndWardenParticleHelper.spawnDeathBurst(sl, this.position());
                sl.playSound(null, this.getX(), this.getY(), this.getZ(),
                        SoundEvents.WARDEN_DEATH, SoundSource.HOSTILE, 3.0F, 0.8F);
            }

            setPhase(EndWardenAttackPhase.DEATH, 20);
            return;
        }

        // 2回目：本当に死ぬ
        setDeathStage(2);

        if (this.level() instanceof ServerLevel sl) {
            EndWardenParticleHelper.spawnDeathBurst(sl, this.position());
            sl.playSound(null, this.getX(), this.getY(), this.getZ(),
                    SoundEvents.ENDER_DRAGON_DEATH, SoundSource.HOSTILE, 3.0F, 0.8F);

            this.spawnAtLocation(net.minecraft.world.item.Items.ECHO_SHARD, 16);
            this.spawnAtLocation(net.minecraft.world.item.Items.NETHERITE_INGOT, 4);
            this.spawnAtLocation(com.tyami.forlaism.registry.Items.MADOROMU.get(), 1);
        }

        super.die(source);
    }

    // =========================================================
    // ヘルパ
    // =========================================================

    /**
     * ターゲットへ視線を強制ロック。
     */
    private void lookAtTarget(LivingEntity target) {
        double dx = target.getX() - this.getX();
        double dy = target.getEyeY() - this.getEyeY();
        double dz = target.getZ() - this.getZ();

        double horizontalDist = Math.sqrt(dx * dx + dz * dz);
        float yaw = (float) (Mth.atan2(dz, dx) * (180.0F / Math.PI)) - 90.0F;
        float pitch = (float) -(Mth.atan2(dy, horizontalDist) * (180.0F / Math.PI));

        this.setYRot(yaw);
        this.setYHeadRot(yaw);
        this.setXRot(pitch);
    }

    // =========================================================
    // フラグ getter/setter（変化時のみ set）
    // =========================================================

    public boolean getFlagDive() {
        return this.entityData.get(DATA_AAA);
    }

    private void setFlagDive(boolean value) {
        if (this.entityData.get(DATA_AAA) != value) {
            this.entityData.set(DATA_AAA, value);
        }
    }

    public boolean getFlagSonic() {
        return this.entityData.get(DATA_SONIC);
    }

    private void setFlagSonic(boolean value) {
        if (this.entityData.get(DATA_SONIC) != value) {
            this.entityData.set(DATA_SONIC, value);
        }
    }

    public int getBeamProgress() {
        return this.entityData.get(DATA_BEAM_PROGRESS);
    }

    private void setBeamProgress(int value) {
        if (this.entityData.get(DATA_BEAM_PROGRESS) != value) {
            this.entityData.set(DATA_BEAM_PROGRESS, value);
        }
    }

    public int getDeathStage() {
        return this.entityData.get(DATA_DEATH_STAGE);
    }

    protected void setDeathStage(int value) {
        if (this.entityData.get(DATA_DEATH_STAGE) != value) {
            this.entityData.set(DATA_DEATH_STAGE, value);
        }
    }

    /**
     * クライアント・サーバー共通のフェーズ取得。
     * クライアント側では同期データを参照する。
     */
    public EndWardenAttackPhase getPhase() {
        if (this.level().isClientSide) {
            int ord = this.entityData.get(DATA_PHASE);
            EndWardenAttackPhase[] values = EndWardenAttackPhase.values();
            if (ord >= 0 && ord < values.length) {
                return values[ord];
            }
            return EndWardenAttackPhase.IDLE;
        }
        return this.phase;
    }

    public String getCurrentAnimationName() {
        return getPhase().getAnimationName();
    }

    // =========================================================
    // NBT
    // =========================================================

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putBoolean("DiveFlag", getFlagDive());
        tag.putBoolean("SonicFlag", getFlagSonic());
        tag.putInt("BeamProgress", getBeamProgress());
        tag.putInt("DeathStage", getDeathStage());
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        setFlagDive(tag.getBoolean("DiveFlag"));
        setFlagSonic(tag.getBoolean("SonicFlag"));
        setBeamProgress(tag.getInt("BeamProgress"));
        setDeathStage(tag.getInt("DeathStage"));
    }
}