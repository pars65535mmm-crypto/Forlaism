package com.tyami.forlaism.entity;

import com.tyami.forlaism.damage.FrostbiteExecution;
import com.tyami.forlaism.damage.MoonlightDamageSource;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
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
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.RandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.core.animation.AnimationState;
import software.bernie.geckolib.core.animation.RawAnimation;
import software.bernie.geckolib.core.object.PlayState;
import software.bernie.geckolib.util.GeckoLibUtil;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Random;

/**
 * ノクスラティア（Noxslratia）。
 *
 * ノクスルクス（夜に沈む剣）とフラスラティア（ニヴヘイムの死神の鎌）を
 * 併せ持つボス。
 *
 * 【変更点】
 *   - 浮遊タイプ → 歩行タイプ（GroundPathNavigation + 通常 MoveControl）
 *   - アニメーションは AnimationController 側でフェーズを見て切り替え
 *   - 歩行アニメ（walk）を追加
 */
public class NoxslratiaEntity extends PathfinderMob implements GeoEntity {

    // =========================================================
    // GeckoLib
    // =========================================================

    private static final RawAnimation IDLE_ANIM =
            RawAnimation.begin().thenLoop("idle");
    private static final RawAnimation WALK_ANIM =
            RawAnimation.begin().thenLoop("walk");
    private static final RawAnimation SPAWN_ANIM =
            RawAnimation.begin().thenPlay("spawn(botu)");
    private static final RawAnimation SCYTHE_ANIM =
            RawAnimation.begin().thenPlay("attack1");
    private static final RawAnimation SWORD_ANIM =
            RawAnimation.begin().thenPlay("attack2");
    private static final RawAnimation SWORD3_ANIM =
            RawAnimation.begin().thenPlay("attack3");
    private static final RawAnimation TP_ANIM =
            RawAnimation.begin().thenPlay("tp");

    private final AnimatableInstanceCache geoCache = GeckoLibUtil.createInstanceCache(this);

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(
                this,
                "main",
                3,
                this::predicate
        ));
    }

    private PlayState predicate(AnimationState<NoxslratiaEntity> state) {
        // ---------------------------------------------------------
        // スポーン中
        // ---------------------------------------------------------
        if (getSpawning()) {
            state.getController().setAnimation(SPAWN_ANIM);
            return PlayState.CONTINUE;
        }

        // ---------------------------------------------------------
        // フェーズごとのアニメ
        // ---------------------------------------------------------
        NoxslratiaAttackPhase phase = getPhase();
        switch (phase) {
            case SCYTHE_ATTACK -> {
                state.getController().setAnimation(SCYTHE_ANIM);
                return PlayState.CONTINUE;
            }
            case SWORD_ATTACK -> {
                state.getController().setAnimation(SWORD_ANIM);
                return PlayState.CONTINUE;
            }
            case SWORD_ATTACK_3 -> {
                state.getController().setAnimation(SWORD3_ANIM);
                return PlayState.CONTINUE;
            }
            case TELEPORT -> {
                state.getController().setAnimation(TP_ANIM);
                return PlayState.CONTINUE;
            }
            default -> { /* 下の歩行/待機へ */ }
        }

        // ---------------------------------------------------------
        // 歩行中かどうかで idle / walk を切替
        // ---------------------------------------------------------
        boolean moving = state.isMoving();
        if (moving) {
            state.getController().setAnimation(WALK_ANIM);
        } else {
            state.getController().setAnimation(IDLE_ANIM);
        }
        return PlayState.CONTINUE;
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return geoCache;
    }

    // =========================================================
    // 同期データ
    // =========================================================

    public static final EntityDataAccessor<Integer> DATA_PHASE =
            SynchedEntityData.defineId(NoxslratiaEntity.class, EntityDataSerializers.INT);

    public static final EntityDataAccessor<Integer> DATA_FORM =
            SynchedEntityData.defineId(NoxslratiaEntity.class, EntityDataSerializers.INT);

    public static final EntityDataAccessor<Boolean> DATA_SPAWNING =
            SynchedEntityData.defineId(NoxslratiaEntity.class, EntityDataSerializers.BOOLEAN);

    // =========================================================
    // 定数
    // =========================================================

    private static final double SCYTHE_RANGE = 4.0D;
    private static final double SWORD_RANGE = 3.5D;
    private static final double SWORD3_RANGE = 5.0D;

    private static final double TELEPORT_BEHIND = 3.0D;
    private static final int TELEPORT_COOLDOWN = 100;

    private static final float MOONLIGHT_DAMAGE = 6.0F;

    private static final int FREEZE_DURATION = 60;
    private static final int SLOW_DURATION = 160;
    private static final int FROSTBITE_DURATION = 200;

    private static final double MAX_HIT_DIST = 6.0D;

    // =========================================================
    // 状態
    // =========================================================

    private NoxslratiaAttackPhase phase = NoxslratiaAttackPhase.SPAWN;
    private int phaseTick = 0;
    private NoxslratiaForm form = NoxslratiaForm.FORM_1;

    private int teleportCooldown = 0;
    private int attackCombo = 0;

    private static final Random RANDOM = new Random();

    private final ServerBossEvent bossBar = new ServerBossEvent(
            this.getDisplayName(),
            BossEvent.BossBarColor.PURPLE,
            BossEvent.BossBarOverlay.PROGRESS
    );

    // =========================================================
    // コンストラクタ
    // =========================================================

    public NoxslratiaEntity(EntityType<? extends PathfinderMob> type, Level level) {
        super(type, level);
        this.xpReward = 500;
        this.setPersistenceRequired();
        // ★ 歩行タイプ: FlyingMoveControl は使わない（デフォルトの MoveControl のまま）
        this.setMaxUpStep(2.0F);
    }

    // =========================================================
    // 属性
    // =========================================================

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 1500.0D)
                .add(Attributes.ARMOR, 30.0D)
                .add(Attributes.ATTACK_DAMAGE, 40.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.32D)
                .add(Attributes.FOLLOW_RANGE, 128.0D)
                .add(Attributes.KNOCKBACK_RESISTANCE, 3.0D)
                .add(Attributes.ATTACK_KNOCKBACK, 2.0D);
    }

    // =========================================================
    // 同期データ定義
    // =========================================================

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(DATA_PHASE, 0);
        this.entityData.define(DATA_FORM, 0);
        this.entityData.define(DATA_SPAWNING, true);
    }

    // ★ createNavigation はオーバーライドしない（デフォルトの GroundPathNavigation のまま）

    // =========================================================
    // AI Goals
    // =========================================================

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(1, new MeleeAttackGoal(this, 1.5D, true));
        this.goalSelector.addGoal(2, new RandomStrollGoal(this, 1.0D, 20) {
            @Override
            protected Vec3 getPosition() {
                RandomSource r = NoxslratiaEntity.this.getRandom();
                double dx = NoxslratiaEntity.this.getX() + (r.nextFloat() * 2.0F - 1.0F) * 16.0F;
                double dy = NoxslratiaEntity.this.getY() + (r.nextFloat() * 2.0F - 1.0F) * 16.0F;
                double dz = NoxslratiaEntity.this.getZ() + (r.nextFloat() * 2.0F - 1.0F) * 16.0F;
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
    public boolean removeWhenFarAway(double d) { return false; }

    @Override
    public boolean causeFallDamage(float f, float m, DamageSource s) { return false; }

    @Override
    protected void checkFallDamage(double y, boolean g, BlockState s, net.minecraft.core.BlockPos p) {}

    @Override
    public boolean fireImmune() { return true; }

    // ★ aiStep の setNoGravity(true) は削除（重力ありの歩行タイプ）

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
            return;  // アニメは AnimationController 側で自動更新
        }

        ServerLevel sl = (ServerLevel) this.level();

        this.bossBar.setProgress(this.getHealth() / this.getMaxHealth());

        if (getSpawning()) {
            tickSpawn(sl);
            return;
        }

        if (teleportCooldown > 0) teleportCooldown--;

        NoxslratiaForm newForm = NoxslratiaForm.fromHpRatio(this.getHealth() / this.getMaxHealth());
        if (newForm != this.form) {
            onFormChanged(newForm);
        }

         // 技クールダウン更新
        skillCooldowns.entrySet().removeIf(e -> {
            int v = e.getValue() - 1;
            if (v <= 0) return true;
            e.setValue(v);
            return false;
        });

        if (this.phaseTick > 0) {
            this.phaseTick--;
            tickPhase(sl);
        } else {
            selectNextPhase(sl);
        }
    }

    private void tickSpawn(ServerLevel sl) {
        if (this.tickCount >= NoxslratiaAttackPhase.SPAWN.getBaseDurationTicks()) {
            setSpawning(false);
            setPhase(NoxslratiaAttackPhase.IDLE, 10);
        }
    }

    // =========================================================
    // 形態
    // =========================================================

    public NoxslratiaForm getForm() {
        if (this.level().isClientSide) {
            int ord = this.entityData.get(DATA_FORM);
            NoxslratiaForm[] values = NoxslratiaForm.values();
            if (ord >= 0 && ord < values.length) return values[ord];
            return NoxslratiaForm.FORM_1;
        }
        return this.form;
    }

    private void onFormChanged(NoxslratiaForm newForm) {
        this.form = newForm;
        this.entityData.set(DATA_FORM, newForm.ordinal());

        if (this.level() instanceof ServerLevel sl) {
            sl.playSound(null, this.getX(), this.getY(), this.getZ(),
                    SoundEvents.WARDEN_ROAR, SoundSource.HOSTILE, 3.0F, 0.5F);

            sl.sendParticles(ParticleTypes.SCULK_SOUL,
                    this.getX(), this.getY() + 1.5, this.getZ(),
                    120, 2.0, 2.5, 2.0, 0.2);

            sl.getPlayers(p -> p.distanceToSqr(this) < 64 * 64)
                    .forEach(p -> p.displayClientMessage(
                            net.minecraft.network.chat.Component.literal(
                                    "§5§lノクスラティアが §c" + newForm.name() + " §5へ…"),
                            true
                    ));
        }
    }

    // =========================================================
    // フェーズ選択
    // =========================================================

        private void selectNextPhase(ServerLevel sl) {
        LivingEntity target = this.getTarget();

        if (target == null || !target.isAlive()) {
            setPhase(NoxslratiaAttackPhase.IDLE, 10);
            return;
        }

        double dist = this.distanceTo(target);

        // ---------------------------------------------------------
        // 技を使うチャンス（クールダウン明け & ランダム）
        // ---------------------------------------------------------
        NoxslratiaSkill skill = pickAvailableSkill(dist);
        if (skill != null && RANDOM.nextInt(100) < 40) {
            this.currentSkill = skill;
            skillCooldowns.put(skill, skill.getCooldownTicks());
            setPhase(NoxslratiaAttackPhase.CAST_SKILL,
                    NoxslratiaAttackPhase.CAST_SKILL.getBaseDurationTicks());
            lastSkill = skill;
            return;
        }

        // 通常攻撃
        if (dist > 15.0D && teleportCooldown <= 0 && RANDOM.nextInt(3) == 0) {
            setPhase(NoxslratiaAttackPhase.TELEPORT, 15);
            return;
        }

        if (dist <= SCYTHE_RANGE) {
            attackCombo = (attackCombo + 1) % 3;
            if (attackCombo == 0) setPhase(NoxslratiaAttackPhase.SCYTHE_ATTACK, 30);
            else if (attackCombo == 1) setPhase(NoxslratiaAttackPhase.SWORD_ATTACK, 30);
            else setPhase(NoxslratiaAttackPhase.SWORD_ATTACK_3, 20);
            return;
        }

        if (dist <= MAX_HIT_DIST) {
            if (RANDOM.nextBoolean()) setPhase(NoxslratiaAttackPhase.SCYTHE_ATTACK, 30);
            else setPhase(NoxslratiaAttackPhase.SWORD_ATTACK, 30);
            return;
        }

        setPhase(NoxslratiaAttackPhase.IDLE, 5);
    }

    @Nullable
    private NoxslratiaSkill pickAvailableSkill(double dist) {
        java.util.List<NoxslratiaSkill> candidates = new java.util.ArrayList<>();
        for (NoxslratiaSkill skill : NoxslratiaSkill.values()) {
            if (skill == lastSkill) continue;
            if (skillCooldowns.containsKey(skill)) continue;
            candidates.add(skill);
        }
        if (candidates.isEmpty()) return null;

        // 距離で重み付け
        float totalWeight = 0;
        for (NoxslratiaSkill s : candidates) {
            float w = s.getWeight();
            if (dist <= 4.0 && (s == NoxslratiaSkill.ABSOLUTE_ZERO
                    || s == NoxslratiaSkill.WHITE_NIGHT
                    || s == NoxslratiaSkill.MOON_ECLIPSE)) {
                w *= 1.5F;
            }
            totalWeight += w;
        }

        float roll = RANDOM.nextFloat() * totalWeight;
        float acc = 0;
        for (NoxslratiaSkill s : candidates) {
            float w = s.getWeight();
            if (dist <= 4.0 && (s == NoxslratiaSkill.ABSOLUTE_ZERO
                    || s == NoxslratiaSkill.WHITE_NIGHT
                    || s == NoxslratiaSkill.MOON_ECLIPSE)) {
                w *= 1.5F;
            }
            acc += w;
            if (roll <= acc) return s;
        }
        return candidates.get(0);
    }

    private void setPhase(NoxslratiaAttackPhase newPhase, int duration) {
        this.phase = newPhase;
        this.phaseTick = duration;
        this.entityData.set(DATA_PHASE, newPhase.ordinal());
        onPhaseStart(newPhase);
    }

    private void onPhaseStart(NoxslratiaAttackPhase newPhase) {
        ServerLevel sl = (ServerLevel) this.level();

        switch (newPhase) {
            case SCYTHE_ATTACK -> sl.playSound(null, this.getX(), this.getY(), this.getZ(),
                    SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.HOSTILE, 1.5F, 0.8F);
            case SWORD_ATTACK -> sl.playSound(null, this.getX(), this.getY(), this.getZ(),
                    SoundEvents.WARDEN_SONIC_CHARGE, SoundSource.HOSTILE, 1.5F, 1.2F);
            case SWORD_ATTACK_3 -> sl.playSound(null, this.getX(), this.getY(), this.getZ(),
                    SoundEvents.RAVAGER_ROAR, SoundSource.HOSTILE, 2.0F, 1.0F);
            case TELEPORT -> {
                sl.playSound(null, this.getX(), this.getY(), this.getZ(),
                        SoundEvents.ENDERMAN_TELEPORT, SoundSource.HOSTILE, 2.0F, 0.7F);
                sl.sendParticles(ParticleTypes.PORTAL,
                        this.getX(), this.getY() + 1.0, this.getZ(),
                        40, 0.5, 1.0, 0.5, 0.3);
            }
            default -> {}
        }
    }

    // =========================================================
    // フェーズ処理
    // =========================================================

    private void tickPhase(ServerLevel sl) {
        LivingEntity targetEntity = this.getTarget();

        switch (this.phase) {
            case SCYTHE_ATTACK -> {
                if (targetEntity != null && this.phaseTick == 15) {
                    applyScytheAttack(sl, targetEntity);
                }
            }
            case SWORD_ATTACK -> {
                if (targetEntity != null && this.phaseTick == 15) {
                    applySwordAttack(sl, targetEntity);
                }
            }
            case SWORD_ATTACK_3 -> {
                if (targetEntity != null && this.phaseTick == 10) {
                    applySword3Attack(sl, targetEntity);
                }
            }
            case TELEPORT -> {
                if (this.phaseTick == 8 && targetEntity != null) {
                    performTeleport(sl, targetEntity);
                }
            }
            case CAST_SKILL -> {
                // ★ 技発動は tickPhase 側で処理
                if (this.phaseTick == 30 && this.currentSkill != null && targetEntity != null) {
                    NoxslratiaSkillHandler.cast(this, sl, targetEntity, this.currentSkill);
                    this.currentSkill = null;
                }
            }
            default -> {}
        }
    }

    private void applyScytheAttack(ServerLevel sl, LivingEntity target) {
        if (this.distanceTo(target) > SCYTHE_RANGE) return;

        target.invulnerableTime = 0;
        target.hurtTime = 0;
        target.hurt(this.damageSources().mobAttack(this), 15.0F);

        target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, FREEZE_DURATION, 3, false, true, true));
        target.addEffect(new MobEffectInstance(MobEffects.DIG_SLOWDOWN, FREEZE_DURATION, 3, false, true, true));
        target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, SLOW_DURATION, 1, false, true, true));
        FrostbiteExecution.apply(target, FROSTBITE_DURATION);

        this.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 160, 1, false, false, false));

        sl.sendParticles(ParticleTypes.SNOWFLAKE,
                target.getX(), target.getY() + target.getBbHeight() * 0.5, target.getZ(),
                25, 0.5, 0.5, 0.5, 0.05);
        sl.playSound(null, target.getX(), target.getY(), target.getZ(),
                SoundEvents.GLASS_BREAK, SoundSource.HOSTILE, 1.0F, 1.5F);
    }

    private void applySwordAttack(ServerLevel sl, LivingEntity target) {
        if (this.distanceTo(target) > SWORD_RANGE) return;

        target.invulnerableTime = 0;
        target.hurtTime = 0;

        for (int i = 0; i < 3; i++) {
            target.invulnerableTime = 0;
            target.hurt(MoonlightDamageSource.of(sl, this), MOONLIGHT_DAMAGE);
        }

        sl.sendParticles(ParticleTypes.END_ROD,
                target.getX(), target.getY() + target.getBbHeight() * 0.5, target.getZ(),
                25, 0.4, 0.4, 0.4, 0.05);
        sl.playSound(null, target.getX(), target.getY(), target.getZ(),
                SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.HOSTILE, 1.2F, 0.8F);

        if (teleportCooldown <= 0 && RANDOM.nextInt(3) == 0) {
            teleportCooldown = TELEPORT_COOLDOWN;
            Vec3 behind = getBehindPos(target);
            this.teleportTo(behind.x, behind.y, behind.z);
            sl.sendParticles(ParticleTypes.PORTAL,
                    behind.x, behind.y + 1.0, behind.z,
                    30, 0.3, 0.8, 0.3, 0.3);
        }
    }

    private void applySword3Attack(ServerLevel sl, LivingEntity target) {
        AABB area = this.getBoundingBox().inflate(SWORD3_RANGE);
        List<LivingEntity> hits = sl.getEntitiesOfClass(
                LivingEntity.class, area,
                e -> e.isAlive() && e != this && !e.isSpectator()
        );

        for (LivingEntity hit : hits) {
            hit.invulnerableTime = 0;
            hit.hurt(MoonlightDamageSource.of(sl, this), 12.0F);
            Vec3 dir = hit.position().subtract(this.position()).normalize();
            hit.push(dir.x * 1.5D, 0.6D, dir.z * 1.5D);
        }

        sl.sendParticles(ParticleTypes.EXPLOSION_EMITTER,
                this.getX(), this.getY(), this.getZ(),
                1, 0, 0, 0, 0);
        sl.playSound(null, this.getX(), this.getY(), this.getZ(),
                SoundEvents.GENERIC_EXPLODE, SoundSource.HOSTILE, 2.0F, 0.8F);
    }

    private void performTeleport(ServerLevel sl, LivingEntity target) {
        Vec3 behind = getBehindPos(target);

        sl.sendParticles(ParticleTypes.PORTAL,
                this.getX(), this.getY() + 1.0, this.getZ(),
                40, 0.5, 1.0, 0.5, 0.3);

        this.teleportTo(behind.x, behind.y, behind.z);

        sl.sendParticles(ParticleTypes.PORTAL,
                behind.x, behind.y + 1.0, behind.z,
                40, 0.5, 1.0, 0.5, 0.3);
        sl.playSound(null, behind.x, behind.y, behind.z,
                SoundEvents.ENDERMAN_TELEPORT, SoundSource.HOSTILE, 2.0F, 0.7F);

        this.lookAt(target, 360.0F, 360.0F);
        teleportCooldown = TELEPORT_COOLDOWN;
    }

    private Vec3 getBehindPos(LivingEntity target) {
        Vec3 look = target.getLookAngle().normalize();
        Vec3 behind = target.position().subtract(look.scale(TELEPORT_BEHIND));
        return new Vec3(behind.x, target.getY(), behind.z);
    }

    private void lookAt(LivingEntity target, float maxYaw, float maxPitch) {
        double dx = target.getX() - this.getX();
        double dy = target.getEyeY() - this.getEyeY();
        double dz = target.getZ() - this.getZ();
        double horiz = Math.sqrt(dx * dx + dz * dz);
        float yaw = (float) (Mth.atan2(dz, dx) * (180.0 / Math.PI)) - 90.0F;
        float pitch = (float) -(Mth.atan2(dy, horiz) * (180.0 / Math.PI));
        this.setYRot(yaw);
        this.setYHeadRot(yaw);
        this.setXRot(pitch);
    }

    public boolean getSpawning() {
        return this.entityData.get(DATA_SPAWNING);
    }

    private void setSpawning(boolean value) {
        this.entityData.set(DATA_SPAWNING, value);
    }

    public NoxslratiaAttackPhase getPhase() {
        if (this.level().isClientSide) {
            int ord = this.entityData.get(DATA_PHASE);
            NoxslratiaAttackPhase[] values = NoxslratiaAttackPhase.values();
            if (ord >= 0 && ord < values.length) return values[ord];
            return NoxslratiaAttackPhase.IDLE;
        }
        return this.phase;
    }

    public String getCurrentAnimationName() {
        return getPhase().getAnimationName();
    }

    @Override
    public void die(DamageSource source) {
        ServerLevel sl = (ServerLevel) this.level();

        sl.sendParticles(ParticleTypes.EXPLOSION_EMITTER,
                this.getX(), this.getY() + 1.5, this.getZ(),
                3, 2.0, 2.0, 2.0, 0);
        sl.playSound(null, this.getX(), this.getY(), this.getZ(),
                SoundEvents.WARDEN_DEATH, SoundSource.HOSTILE, 3.0F, 0.5F);

        // =========================================================
        // 固定ドロップ
        // =========================================================
        dropItem(sl, new ItemStack(com.tyami.forlaism.registry.Items.NOXLUX.get(), 1));
        dropItem(sl, new ItemStack(com.tyami.forlaism.registry.Items.FRASLATIA.get(), 1));

        // =========================================================
        // 素材ドロップ
        // =========================================================
        dropRandom(sl, com.tyami.forlaism.registry.Items.SILVER_FOIL_ROLL.get(), 3, 8);
        dropRandom(sl, com.tyami.forlaism.registry.Items.PURE_GOLD_THREAD.get(), 12, 38);
        dropRandom(sl, com.tyami.forlaism.registry.Items.LIGHT_ALUMINUM_PLATE.get(), 3, 20);
        dropRandom(sl, net.minecraft.world.item.Items.DIAMOND, 2, 4);
        dropRandom(sl, net.minecraft.world.item.Items.AMETHYST_SHARD, 3, 5);
        dropRandom(sl, net.minecraft.world.item.Items.NETHERITE_INGOT, 4, 7);
        dropRandom(sl, com.tyami.forlaism.registry.Items.METAL_SCRAP.get(), 31, 203);
        dropRandom(sl, com.tyami.forlaism.registry.Items.MECHANICAL_DETERMINATION.get(), 1, 1);

        super.die(source);
    }

    /**
     * アイテムを直接 ItemEntity としてドロップする。
     * 既存の spawnAtLocation の挙動に依存しない。
     */
    private void dropItem(ServerLevel level, ItemStack stack) {
        if (stack.isEmpty()) return;

        net.minecraft.world.entity.item.ItemEntity entity =
                new net.minecraft.world.entity.item.ItemEntity(
                        level,
                        this.getX(),
                        this.getY() + this.getBbHeight() * 0.5,
                        this.getZ(),
                        stack
                );

        entity.setDefaultPickUpDelay();

        // 散らばるように速度を付ける
        double vx = (this.random.nextDouble() - 0.5) * 0.3;
        double vy = this.random.nextDouble() * 0.3 + 0.1;
        double vz = (this.random.nextDouble() - 0.5) * 0.3;
        entity.setDeltaMovement(vx, vy, vz);

        level.addFreshEntity(entity);
    }

    /**
     * 範囲ランダムでドロップする。
     * 64を超える場合は分割して複数のItemEntityにする。
     */
    private void dropRandom(ServerLevel level, net.minecraft.world.item.Item item, int min, int max) {
        int count = min + this.random.nextInt(max - min + 1);
        while (count > 0) {
            int chunk = Math.min(count, item.getMaxStackSize());
            dropItem(level, new ItemStack(item, chunk));
            count -= chunk;
        }
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt("Phase", getPhase().ordinal());
        tag.putInt("Form", getForm().ordinal());
        tag.putBoolean("Spawning", getSpawning());
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        int phaseOrd = tag.getInt("Phase");
        if (phaseOrd >= 0 && phaseOrd < NoxslratiaAttackPhase.values().length) {
            this.phase = NoxslratiaAttackPhase.values()[phaseOrd];
        }
        int formOrd = tag.getInt("Form");
        if (formOrd >= 0 && formOrd < NoxslratiaForm.values().length) {
            this.form = NoxslratiaForm.values()[formOrd];
        }
        setSpawning(tag.getBoolean("Spawning"));
    }


    // =========================================================
    // 技関連
    // =========================================================

    /** 現在撃とうとしている技。 */
    @Nullable
    private NoxslratiaSkill currentSkill = null;

    /** 技ごとのクールダウン管理。 */
    private final java.util.Map<NoxslratiaSkill, Integer> skillCooldowns = new java.util.HashMap<>();

    /** 最後に撃った技（連続選択防止）。 */
    @Nullable
    private NoxslratiaSkill lastSkill = null;
}