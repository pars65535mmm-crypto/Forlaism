package com.tyami.forlaism.entity;

import com.tyami.forlaism.damage.RinneDamageSource;
import com.tyami.forlaism.registry.Items;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.BossEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
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
import net.minecraft.world.entity.item.PrimedTnt;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.SmallFireball;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Random;

/**
 * エンディストウォーデン。
 *
 * リバースの全能力継承 + 攻撃力2倍 + ソニックブーム3倍速・3倍距離・ホーミング。
 *
 * フェーズ:
 *   第一形態: 通常攻撃 + ソニックブーム + 突進
 *   第二形態: 1回目の死で移行 → TP攻撃 + 反射レーザー
 *   第三形態: 2回目の死で移行 → 属性乱射 + TNT + 隕石
 *   死亡: 3回目の死で真の死
 */
public class EndWardenFinalEntity extends PathfinderMob {

    // =========================================================
    // 定数
    // =========================================================

    public static final double MAX_HP = 9910.0D;
    private static final float ATTACK_DAMAGE = 96.0F;

    private static final double BEAM_RANGE = 90.0D;
    private static final double BEAM_STEP = 3.0D;
    private static final float BEAM_HIT_DAMAGE = 50.0F;

    private static final double HOMING_STRENGTH = 0.15D;

    private static final int TP_CHARGE_TICKS = 20;
    private static final double TP_BEHIND_DISTANCE = 30.0D;
    private static final float TP_RINNE_DAMAGE = 2_100_000_000.0F;
    private static final int TP_DASH_COUNT = 45;

    private static final int ELEMENTAL_INTERVAL = 100; // 5秒

    private static final Random RANDOM = new Random();

    // =========================================================
    // 状態
    // =========================================================

    private EndWardenFinalAttackPhase phase = EndWardenFinalAttackPhase.IDLE;
    private int phaseTick = 0;
    private int chargeTick = 0;
    private int beamProgress = 0;

    /** 死亡段階: 0=第一, 1=第二, 2=第三 */
    private int deathStage = 0;

    /** 第三形態フラグ。 */
    private boolean phase3Activated = false;

    /** TP攻撃の突進回数とクールダウン。 */
    private int tpDashRemaining = 0;
    private int tpDashCooldown = 0;

    /** ボスバー。 */
    private final ServerBossEvent bossBar = new ServerBossEvent(
            Component.literal("エンディストウォーデン"),
            BossEvent.BossBarColor.PURPLE,
            BossEvent.BossBarOverlay.PROGRESS
    );

    // =========================================================
    // コンストラクタ
    // =========================================================

    public EndWardenFinalEntity(EntityType<? extends PathfinderMob> type, Level level) {
        super(type, level);
        this.xpReward = 99999;
        this.setPersistenceRequired();
        this.setMaxUpStep(3.0F);
        this.moveControl = new FlyingMoveControl(this, 10, true);
    }

    // =========================================================
    // 属性
    // =========================================================

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, MAX_HP)
                .add(Attributes.ARMOR, 50.0D)
                .add(Attributes.ATTACK_DAMAGE, ATTACK_DAMAGE)
                .add(Attributes.MOVEMENT_SPEED, 0.5D)
                .add(Attributes.FLYING_SPEED, 0.6D)
                .add(Attributes.FOLLOW_RANGE, 200.0D)
                .add(Attributes.KNOCKBACK_RESISTANCE, 3.0D)
                .add(Attributes.ATTACK_KNOCKBACK, 3.0D);
    }

    // =========================================================
    // AI / ナビゲーション
    // =========================================================

    @Override
    protected PathNavigation createNavigation(Level level) {
        FlyingPathNavigation nav = new FlyingPathNavigation(this, level);
        nav.setCanOpenDoors(false);
        nav.setCanFloat(true);
        return nav;
    }

    @Override
    protected void registerGoals() {
        // 近接攻撃
        this.goalSelector.addGoal(1, new MeleeAttackGoal(this, 1.5D, true));

        // 徘徊
        this.goalSelector.addGoal(2, new RandomStrollGoal(this, 1.0D, 20) {
            @Override
            protected Vec3 getPosition() {
                RandomSource random = EndWardenFinalEntity.this.getRandom();
                double dx = EndWardenFinalEntity.this.getX() + (random.nextFloat() * 2.0F - 1.0F) * 16.0F;
                double dy = EndWardenFinalEntity.this.getY() + (random.nextFloat() * 2.0F - 1.0F) * 16.0F;
                double dz = EndWardenFinalEntity.this.getZ() + (random.nextFloat() * 2.0F - 1.0F) * 16.0F;
                return new Vec3(dx, dy, dz);
            }
        });

        // ターゲット選択
        this.targetSelector.addGoal(3, new NearestAttackableTargetGoal<>(this, Player.class, false, false));
        this.targetSelector.addGoal(4, new HurtByTargetGoal(this));

        this.goalSelector.addGoal(5, new RandomLookAroundGoal(this));
        this.goalSelector.addGoal(6, new FloatGoal(this));
    }

    // =========================================================
    // 基本設定
    // =========================================================

    @Override
    public boolean removeWhenFarAway(double d) { return false; }

    @Override
    public boolean causeFallDamage(float f, float m, DamageSource s) { return false; }

    @Override
    protected void checkFallDamage(double y, boolean g, BlockState s, BlockPos p) {}

    @Override
    public boolean fireImmune() { return true; }

    @Override
    public void aiStep() {
        super.aiStep();
        this.setNoGravity(true);
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (source.is(net.minecraft.tags.DamageTypeTags.IS_FIRE)) return false;
        if (source.is(net.minecraft.tags.DamageTypeTags.IS_FALL)) return false;
        if (source.is(net.minecraft.tags.DamageTypeTags.IS_DROWNING)) return false;
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
    // ボスバー
    // =========================================================

    @Override
    public void startSeenByPlayer(ServerPlayer p) {
        super.startSeenByPlayer(p);
        this.bossBar.addPlayer(p);
    }

    @Override
    public void stopSeenByPlayer(ServerPlayer p) {
        super.stopSeenByPlayer(p);
        this.bossBar.removePlayer(p);
    }

    // =========================================================
    // tick
    // =========================================================

    @Override
    public void tick() {
        super.tick();

        if (this.level().isClientSide) {
            // クライアント: 虚無の衣の演出
            if (this.tickCount % 3 == 0) {
                this.level().addParticle(
                        ParticleTypes.SCULK_SOUL,
                        this.getX() + (this.random.nextDouble() - 0.5) * 2.5,
                        this.getY() + this.random.nextDouble() * 3.5,
                        this.getZ() + (this.random.nextDouble() - 0.5) * 2.5,
                        0, 0.02, 0
                );
            }
            return;
        }

        ServerLevel sl = (ServerLevel) this.level();

        // ボスバー更新
        this.bossBar.setProgress(this.getHealth() / this.getMaxHealth());

        // 虚無の衣（常時オーラ）
        if (this.tickCount % 5 == 0) {
            EndWardenFinalParticleHelper.spawnVoidCloak(sl, this.position());
        }

        // フェーズ進行
        if (this.phaseTick > 0) {
            this.phaseTick--;
            tickPhase(sl);
        } else {
            selectNextPhase(sl);
        }

        // 第三形態中の常時攻撃（5秒ごと）
        if (phase3Activated && this.tickCount % ELEMENTAL_INTERVAL == 0) {
            elementalBarrage(sl);
        }

        // TP攻撃の突進処理
        if (this.tpDashRemaining > 0 && this.tpDashCooldown <= 0) {
            performTpDash(sl);
            this.tpDashCooldown = 2;
        }
        if (this.tpDashCooldown > 0) {
            this.tpDashCooldown--;
        }
    }

    // =========================================================
    // フェーズ選択
    // =========================================================

    private void selectNextPhase(ServerLevel sl) {
        LivingEntity target = this.getTarget();
        if (target == null || !target.isAlive()) {
            setPhase(EndWardenFinalAttackPhase.IDLE, 10);
            return;
        }

        double dist = this.distanceTo(target);

        // TP攻撃: 第二形態以降 + ランダム
        if (this.deathStage >= 1 && RANDOM.nextInt(4) == 0 && dist >= 5.0D) {
            setPhase(EndWardenFinalAttackPhase.TP_STRIKE, 60);
            return;
        }

        // ソニックブーム
        if (dist >= 7.0D && dist <= BEAM_RANGE) {
            if (RANDOM.nextInt(3) == 0) {
                setPhase(EndWardenFinalAttackPhase.SONIC_BOOM, 60);
                return;
            }
        }

        // 突進
        if (dist >= 5.0D && dist <= 40.0D) {
            if (RANDOM.nextInt(2) == 0) {
                setPhase(EndWardenFinalAttackPhase.DIVE, 40);
                return;
            }
        }

        // 近接
        if (dist <= 6.0D) {
            setPhase(EndWardenFinalAttackPhase.MELEE, 20);
            return;
        }

        setPhase(EndWardenFinalAttackPhase.IDLE, 5);
    }

    private void setPhase(EndWardenFinalAttackPhase newPhase, int duration) {
        this.phase = newPhase;
        this.phaseTick = duration;
        this.chargeTick = 0;
        this.beamProgress = 0;
        onPhaseStart(newPhase);
    }

    private void onPhaseStart(EndWardenFinalAttackPhase newPhase) {
        ServerLevel sl = (ServerLevel) this.level();

        switch (newPhase) {
            case SONIC_BOOM -> {
                this.chargeTick = 30;
                sl.playSound(null, this.getX(), this.getY(), this.getZ(),
                        SoundEvents.WARDEN_SONIC_CHARGE, SoundSource.HOSTILE, 3.0F, 1.0F);
            }
            case DIVE -> {
                sl.playSound(null, this.getX(), this.getY(), this.getZ(),
                        SoundEvents.WARDEN_ROAR, SoundSource.HOSTILE, 2.5F, 1.2F);
            }
            case TP_STRIKE -> {
                this.chargeTick = TP_CHARGE_TICKS;
                sl.playSound(null, this.getX(), this.getY(), this.getZ(),
                        SoundEvents.ENDERMAN_TELEPORT, SoundSource.HOSTILE, 2.0F, 0.5F);
            }
            default -> {}
        }
    }

    // =========================================================
    // フェーズ処理
    // =========================================================

    private void tickPhase(ServerLevel sl) {
        switch (this.phase) {
            case SONIC_BOOM -> tickSonicBoom(sl);
            case DIVE -> tickDive(sl);
            case MELEE -> tickMelee(sl);
            case TP_STRIKE -> tickTpStrike(sl);
            default -> tickIdle(sl);
        }
    }

    private void tickIdle(ServerLevel sl) {
        LivingEntity target = this.getTarget();
        if (target != null) {
            this.getLookControl().setLookAt(target, 30.0F, 30.0F);
        }
    }

    private void tickMelee(ServerLevel sl) {
        LivingEntity target = this.getTarget();
        if (target != null) {
            this.getLookControl().setLookAt(target, 30.0F, 30.0F);
        }
    }

    // =========================================================
    // ソニックブーム
    // =========================================================

    private void tickSonicBoom(ServerLevel sl) {
        LivingEntity target = this.getTarget();
        if (target == null || !target.isAlive()) {
            this.phaseTick = 0;
            return;
        }

        Vec3 eye = this.getEyePosition();

        // チャージ
        if (this.chargeTick > 0) {
            this.chargeTick--;
            float progress = 1.0F - (this.chargeTick / 30.0F);

            lookAtTarget(target);
            Vec3 look = this.getLookAngle().normalize();
            EndWardenFinalParticleHelper.spawnTpCharge(sl, eye, look, progress);

            if (this.chargeTick == 0) {
                sl.playSound(null, this.getX(), this.getY(), this.getZ(),
                        SoundEvents.WARDEN_SONIC_BOOM, SoundSource.HOSTILE, 4.0F, 1.0F);
            }
            return;
        }

        // ビーム: ホーミング
        Vec3 look = this.getLookAngle().normalize();

        if (target.isAlive()) {
            Vec3 toTarget = target.getEyePosition().subtract(eye).normalize();
            look = look.add(toTarget.scale(HOMING_STRENGTH)).normalize();
            this.setYRot((float)(Math.atan2(-look.x, look.z) * 180.0 / Math.PI));
            this.setXRot((float)(-Math.asin(look.y) * 180.0 / Math.PI));
        }

        int progress = this.beamProgress;
        Vec3 point = eye.add(look.scale(progress));

        for (double d = 0; d < progress; d += BEAM_STEP) {
            Vec3 p = eye.add(look.scale(d));
            EndWardenFinalParticleHelper.spawnReflectiveBeam(sl, p, p.add(look));
        }

        applyBeamDamage(sl, point);

        this.beamProgress += (int) BEAM_STEP;

        if (progress >= BEAM_RANGE) {
            this.phaseTick = 0;
        }
    }

    private void applyBeamDamage(ServerLevel sl, Vec3 point) {
        AABB box = new AABB(
                point.x - 1.5, point.y - 1.5, point.z - 1.5,
                point.x + 1.5, point.y + 1.5, point.z + 1.5
        );

        List<LivingEntity> hits = sl.getEntitiesOfClass(
                LivingEntity.class, box,
                e -> e.isAlive() && e != this && !e.isSpectator()
        );

        for (LivingEntity hit : hits) {
            hit.invulnerableTime = 0;
            hit.hurtTime = 0;
            hit.hurt(RinneDamageSource.of(sl, this), BEAM_HIT_DAMAGE);

            hit.addEffect(new MobEffectInstance(MobEffects.DARKNESS, 120, 2, false, false));
            hit.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 120, 2, false, false));
        }
    }

    // =========================================================
    // 突進
    // =========================================================

    private void tickDive(ServerLevel sl) {
        LivingEntity target = this.getTarget();
        if (target == null || !target.isAlive()) {
            this.phaseTick = 0;
            return;
        }

        Vec3 dir = target.position().subtract(this.position()).normalize();
        this.setDeltaMovement(dir.scale(1.8D));
        this.hurtMarked = true;

        EndWardenFinalParticleHelper.spawnMeteorTrail(sl, this.position());

        AABB hitBox = this.getBoundingBox().inflate(2.5D);
        List<LivingEntity> hits = sl.getEntitiesOfClass(
                LivingEntity.class, hitBox,
                e -> e.isAlive() && e != this && !e.isSpectator()
        );

        for (LivingEntity hit : hits) {
            hit.invulnerableTime = 0;
            hit.hurt(this.damageSources().mobAttack(this), ATTACK_DAMAGE);
            this.phaseTick = Math.min(this.phaseTick, 5);
            return;
        }
    }

    // =========================================================
    // TP移動攻撃
    // =========================================================

    private void tickTpStrike(ServerLevel sl) {
        LivingEntity target = this.getTarget();
        if (target == null || !target.isAlive()) {
            this.phaseTick = 0;
            return;
        }

        // 溜め
        if (this.chargeTick > 0) {
            this.chargeTick--;
            lookAtTarget(target);
            Vec3 eye = this.getEyePosition();
            Vec3 look = this.getLookAngle();
            EndWardenFinalParticleHelper.spawnTpCharge(sl, eye, look, 1.0F);

            if (this.chargeTick == 0) {
                // 溜め完了 → 背後30ブロックにTP
                Vec3 look2 = target.getLookAngle().normalize();
                Vec3 behind = target.position().subtract(look2.scale(TP_BEHIND_DISTANCE));

                sl.sendParticles(ParticleTypes.PORTAL,
                        this.getX(), this.getY(), this.getZ(),
                        40, 0.5, 1.0, 0.5, 0.5);

                this.teleportTo(behind.x, behind.y, behind.z);

                sl.sendParticles(ParticleTypes.PORTAL,
                        behind.x, behind.y, behind.z,
                        40, 0.5, 1.0, 0.5, 0.5);

                sl.playSound(null, behind.x, behind.y, behind.z,
                        SoundEvents.ENDERMAN_TELEPORT, SoundSource.HOSTILE, 2.0F, 0.5F);

                this.tpDashRemaining = TP_DASH_COUNT;
                this.tpDashCooldown = 0;
            }
            return;
        }
    }

    private void performTpDash(ServerLevel sl) {
        LivingEntity target = this.getTarget();
        if (target == null) {
            this.tpDashRemaining = 0;
            return;
        }

        Vec3 dir = target.position().subtract(this.position()).normalize();
        this.setDeltaMovement(dir.scale(3.5D));
        this.hurtMarked = true;

        EndWardenFinalParticleHelper.spawnLaserMeteorTrail(sl, this.position());

        AABB hitBox = this.getBoundingBox().inflate(1.5D);
        List<LivingEntity> hits = sl.getEntitiesOfClass(
                LivingEntity.class, hitBox,
                e -> e.isAlive() && e != this && !e.isSpectator()
        );

        for (LivingEntity hit : hits) {
            hit.invulnerableTime = 0;
            hit.hurtTime = 0;
            hit.hurt(RinneDamageSource.of(sl, this), TP_RINNE_DAMAGE);

            if (hit instanceof ServerPlayer sp) {
                sp.displayClientMessage(
                        Component.literal("§4§l終理の突進… §c魂ごと消し飛ばされた"),
                        true
                );
            }
        }

        this.tpDashRemaining--;

        if (this.tpDashRemaining <= 0) {
            this.phaseTick = 0;
        }
    }

    // =========================================================
    // 第三形態
    // =========================================================

    private void elementalBarrage(ServerLevel sl) {
        LivingEntity target = this.getTarget();
        if (target == null || !target.isAlive()) return;

        int attackType = RANDOM.nextInt(6);

        switch (attackType) {
            case 0 -> fireAttack(sl, target);
            case 1 -> iceAttack(sl, target);
            case 2 -> lightningAttack(sl, target);
            case 3 -> tntBarrage(sl, target);
            case 4 -> meteorStrike(sl, target, false);
            case 5 -> meteorStrike(sl, target, true);
        }

        EndWardenFinalParticleHelper.spawnElementalHint(sl, this.position(), attackType);
    }

    private void fireAttack(ServerLevel sl, LivingEntity target) {
        Vec3 dir = target.position().subtract(this.position()).normalize();
        for (double d = 0; d < 20.0; d += 1.0) {
            Vec3 p = this.getEyePosition().add(dir.scale(d));
            sl.sendParticles(ParticleTypes.FLAME, p.x, p.y, p.z, 5, 0.3, 0.3, 0.3, 0.05);
            sl.sendParticles(ParticleTypes.LAVA, p.x, p.y, p.z, 1, 0.2, 0.2, 0.2, 0.0);
        }

        AABB box = this.getBoundingBox().inflate(20.0);
        for (LivingEntity e : sl.getEntitiesOfClass(LivingEntity.class, box,
                e -> e.isAlive() && e != this)) {
            e.setSecondsOnFire(8);
            e.hurt(sl.damageSources().mobAttack(this), 20.0F);
        }
    }

    private void iceAttack(ServerLevel sl, LivingEntity target) {
        AABB box = this.getBoundingBox().inflate(20.0);
        for (LivingEntity e : sl.getEntitiesOfClass(LivingEntity.class, box,
                e -> e.isAlive() && e != this)) {
            e.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 200, 4, false, true));
            e.hurt(sl.damageSources().mobAttack(this), 15.0F);
        }

        for (int i = 0; i < 60; i++) {
            double a = RANDOM.nextDouble() * Math.PI * 2;
            double r = RANDOM.nextDouble() * 15;
            sl.sendParticles(ParticleTypes.SNOWFLAKE,
                    this.getX() + Math.cos(a) * r,
                    this.getY() + RANDOM.nextDouble() * 3,
                    this.getZ() + Math.sin(a) * r,
                    2, 0.1, 0.1, 0.1, 0.0);
        }
    }

    private void lightningAttack(ServerLevel sl, LivingEntity target) {
        var lightning = net.minecraft.world.entity.EntityType.LIGHTNING_BOLT.create(sl);
        if (lightning != null) {
            lightning.moveTo(target.getX(), target.getY(), target.getZ());
            lightning.setVisualOnly(false);
            sl.addFreshEntity(lightning);
        }

        for (int i = 0; i < 5; i++) {
            double a = RANDOM.nextDouble() * Math.PI * 2;
            double r = 5 + RANDOM.nextDouble() * 10;
            var l2 = net.minecraft.world.entity.EntityType.LIGHTNING_BOLT.create(sl);
            if (l2 != null) {
                l2.moveTo(this.getX() + Math.cos(a) * r, this.getY(), this.getZ() + Math.sin(a) * r);
                l2.setVisualOnly(false);
                sl.addFreshEntity(l2);
            }
        }
    }

    private void tntBarrage(ServerLevel sl, LivingEntity target) {
        for (int i = 0; i < 15; i++) {
            double a = RANDOM.nextDouble() * Math.PI * 2;
            double r = 3 + RANDOM.nextDouble() * 15;

            PrimedTnt tnt = new PrimedTnt(sl,
                    this.getX() + Math.cos(a) * r,
                    this.getY() + 5 + RANDOM.nextDouble() * 5,
                    this.getZ() + Math.sin(a) * r,
                    this);

            tnt.setFuse(20 + RANDOM.nextInt(20));
            sl.addFreshEntity(tnt);
        }
    }

    private void meteorStrike(ServerLevel sl, LivingEntity target, boolean laser) {
        int count = laser ? 3 : 5;

        for (int i = 0; i < count; i++) {
            double a = RANDOM.nextDouble() * Math.PI * 2;
            double r = RANDOM.nextDouble() * 12;

            double tx = target.getX() + Math.cos(a) * r;
            double ty = target.getY() + 40;
            double tz = target.getZ() + Math.sin(a) * r;

            SmallFireball meteor = new SmallFireball(sl, tx, ty, tz, 0, -2.5, 0);
            meteor.setOwner(this);
            sl.addFreshEntity(meteor);

            for (double y = ty; y > target.getY(); y -= 1.0) {
                if (laser) {
                    sl.sendParticles(ParticleTypes.SONIC_BOOM,
                            tx, y, tz, 3, 0.3, 0.3, 0.3, 0.0);
                } else {
                    EndWardenFinalParticleHelper.spawnMeteorTrail(sl,
                            new Vec3(tx, y, tz));
                }
            }

            final double fx = tx, fz = tz;
            final float targetY = (float) target.getY();
            sl.getServer().execute(() -> {
                sl.explode(this, fx, targetY, fz, laser ? 6.0F : 4.0F,
                        Level.ExplosionInteraction.MOB);

                if (laser) {
                    AABB box = new AABB(fx - 6, targetY - 3, fz - 6,
                                        fx + 6, targetY + 6, fz + 6);
                    for (LivingEntity e : sl.getEntitiesOfClass(LivingEntity.class, box,
                            e -> e.isAlive() && e != EndWardenFinalEntity.this)) {
                        e.invulnerableTime = 0;
                        e.hurt(RinneDamageSource.of(sl, EndWardenFinalEntity.this),
                                100_000_000.0F);
                    }
                }
            });
        }
    }

    // =========================================================
    // ヘルパ
    // =========================================================

    private void lookAtTarget(LivingEntity target) {
        double dx = target.getX() - this.getX();
        double dy = target.getEyeY() - this.getEyeY();
        double dz = target.getZ() - this.getZ();

        double horiz = Math.sqrt(dx * dx + dz * dz);
        float yaw = (float) (Math.atan2(dz, dx) * (180.0 / Math.PI)) - 90.0F;
        float pitch = (float) -(Math.atan2(dy, horiz) * (180.0 / Math.PI));

        this.setYRot(yaw);
        this.setYHeadRot(yaw);
        this.setXRot(pitch);
    }

    // =========================================================
    // 死亡（フェーズ移行）
    // =========================================================

    @Override
    public void die(DamageSource source) {
        ServerLevel sl = (ServerLevel) this.level();

        // =========================================================
        // 第一形態 → 第二形態
        // =========================================================
        if (getDeathStage() == 0) {
            setDeathStage(1);
            this.setHealth(this.getMaxHealth());

            EndWardenFinalParticleHelper.spawnFinalDeath(sl, this.position());
            sl.playSound(null, this.getX(), this.getY(), this.getZ(),
                    SoundEvents.WARDEN_ROAR, SoundSource.HOSTILE, 4.0F, 0.5F);

            sl.getPlayers(p -> p.distanceToSqr(this) < 256 * 256)
                    .forEach(p -> p.displayClientMessage(
                            Component.literal("§5§lエンディストウォーデンが §c第二形態 §5へ…"),
                            true
                    ));

            setPhase(EndWardenFinalAttackPhase.IDLE, 20);
            return;
        }

        // =========================================================
        // 第二形態 → 第三形態
        // =========================================================
        if (getDeathStage() == 1) {
            setDeathStage(2);
            this.setHealth(this.getMaxHealth());
            this.phase3Activated = true;

            EndWardenFinalParticleHelper.spawnFinalDeath(sl, this.position());
            sl.playSound(null, this.getX(), this.getY(), this.getZ(),
                    SoundEvents.WITHER_SPAWN, SoundSource.HOSTILE, 4.0F, 0.5F);
            sl.playSound(null, this.getX(), this.getY(), this.getZ(),
                    SoundEvents.ENDER_DRAGON_GROWL, SoundSource.HOSTILE, 3.0F, 0.5F);

            sl.sendParticles(ParticleTypes.EXPLOSION_EMITTER,
                    this.getX(), this.getY() + 1.5, this.getZ(),
                    5, 3.0, 3.0, 3.0, 0);

            sl.getPlayers(p -> p.distanceToSqr(this) < 256 * 256)
                    .forEach(p -> p.displayClientMessage(
                            Component.literal("§4§lエンディストウォーデンが §c最終形態 §4へ…"),
                            true
                    ));

            setPhase(EndWardenFinalAttackPhase.IDLE, 20);
            return;
        }

        // =========================================================
        // 第三形態 → 本当に死ぬ
        // =========================================================
        EndWardenFinalParticleHelper.spawnFinalDeath(sl, this.position());
        sl.playSound(null, this.getX(), this.getY(), this.getZ(),
                SoundEvents.WARDEN_DEATH, SoundSource.HOSTILE, 4.0F, 0.5F);
        sl.playSound(null, this.getX(), this.getY(), this.getZ(),
                SoundEvents.ENDER_DRAGON_DEATH, SoundSource.HOSTILE, 3.0F, 0.5F);

        // ドロップ: 大罪の石 ×4, null_sugar ×4
        this.spawnAtLocation(new ItemStack(Items.STONE_OF_SIN.get(), 4));
        this.spawnAtLocation(new ItemStack(Items.NULL_SUGAR.get(), 4));

        super.die(source);
    }

    // =========================================================
    // 死亡段階
    // =========================================================

    public int getDeathStage() {
        return this.deathStage;
    }

    private void setDeathStage(int stage) {
        this.deathStage = stage;
    }

    // =========================================================
    // NBT
    // =========================================================

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt("DeathStage", this.deathStage);
        tag.putBoolean("Phase3Activated", this.phase3Activated);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        this.deathStage = tag.getInt("DeathStage");
        this.phase3Activated = tag.getBoolean("Phase3Activated");
    }
}