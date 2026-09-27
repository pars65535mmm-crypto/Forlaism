package com.tyami.forlaism.entity;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;
import java.util.EnumSet;
import java.util.List;

/**
 * クリーパーロード。
 *
 * 中ボス。
 *
 * - クリーパースポーン時に 1% で置換出現
 * - 耐性IV / 再生IV / 移動速度上昇IV を永続付与
 * - 攻撃対象がいると 1秒 (20tick) ごとに
 *   「移動速度3倍 / 段差上限4倍」の爆撃クリーパーを召喚しプレイヤーに敵対させる
 * - 15マス以内にプレイヤーが入るとバックステップで逃げる
 * - 自身が爆発した場合:
 *     ・範囲3倍
 *     ・プレイヤーに鈍足255 + ウィザー255 を 10秒
 *     ・120貫通ダメージ
 * - 儀式の短剣でクリーパーを殺すと確定召喚
 */
public class CreeperLordEntity extends Creeper {

    /** 攻撃対象がいる時にクリーパーを召喚する間隔 (tick)。 */
    private static final int SUMMON_INTERVAL = 20;

    /** 召喚クリーパーの移動速度倍率。 */
    private static final double SUMMON_SPEED_MULTIPLIER = 3.0D;

    /** 召喚クリーパーの段差上限倍率。 */
    private static final double SUMMON_STEP_MULTIPLIER = 4.0D;

    /** バックステップを開始する距離（マス）。 */
    private static final double BACKSTEP_DISTANCE = 15.0D;

    /** バックステップの継続 tick。 */
    private static final int BACKSTEP_DURATION = 12;

    /** バックステップの勢い。 */
    private static final double BACKSTEP_POWER = 1.2D;

    /** 自爆時の爆破範囲倍率。 */
    private static final float EXPLOSION_RADIUS_MULTIPLIER = 3.0F;

    /** 自爆時のプレイヤーへの貫通ダメージ。 */
    private static final float EXPLOSION_PIERCE_DAMAGE = 120.0F;

    /** 自爆時のデバフ時間 (tick)。10秒 = 200 tick。 */
    private static final int EXPLOSION_DEBUFF_DURATION = 200;

    /** 召喚した爆撃クリーパー識別タグ。 */
    public static final String BOMBER_TAG = "ForlaismCreeperLordBomber";

    /** 爆破済みフラグ。 */
    private static final String TAG_DETONATED = "ForlaismCreeperLordDetonated";

    /** 召喚カウンター。 */
    private int summonCooldown = SUMMON_INTERVAL;

    /** バックステップ残り tick。 */
    private int backstepTicks = 0;

    public CreeperLordEntity(EntityType<? extends Creeper> type, Level level) {
        super(type, level);
        this.xpReward = 100;
        this.setPersistenceRequired();
    }

    // =========================================================
    // 属性
    // =========================================================

    public static AttributeSupplier.Builder createAttributes() {
        return Creeper.createAttributes()
                .add(Attributes.MAX_HEALTH, 100.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.30D)
                .add(Attributes.ARMOR, 20.0D)
                .add(Attributes.ARMOR_TOUGHNESS, 12.0D)
                .add(Attributes.KNOCKBACK_RESISTANCE, 1.0D)
                .add(Attributes.FOLLOW_RANGE, 64.0D);
    }

    // =========================================================
    // AI
    // =========================================================

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(1, new CreeperLordSwellGoal(this));
        this.goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.0D, false));
        this.goalSelector.addGoal(7, new LookAtPlayerGoal(this, Player.class, 32.0F));

        this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
    }

    // =========================================================
    // スポーン時: 永続バフ
    // =========================================================

    @Nullable
    @Override
    public SpawnGroupData finalizeSpawn(
            ServerLevelAccessor level,
            DifficultyInstance difficulty,
            MobSpawnType reason,
            @Nullable SpawnGroupData spawnData,
            @Nullable CompoundTag dataTag
    ) {
        SpawnGroupData result = super.finalizeSpawn(level, difficulty, reason, spawnData, dataTag);

        applyLordBuffs();

        return result;
    }

    /**
     * 耐性IV / 再生IV / 移動速度上昇IV を永続付与。
     * 効果時間は実質無限 (Integer.MAX_VALUE)。
     */
    private void applyLordBuffs() {
        // 耐性 IV (Resistance, amplifier 3 = Lv4)
        this.addEffect(new MobEffectInstance(
                MobEffects.DAMAGE_RESISTANCE,
                Integer.MAX_VALUE,
                3,
                false,
                false,
                false
        ));

        // 再生 IV (Regeneration, amplifier 3 = Lv4)
        this.addEffect(new MobEffectInstance(
                MobEffects.REGENERATION,
                Integer.MAX_VALUE,
                3,
                false,
                false,
                false
        ));

        // 移動速度上昇 IV (Movement Speed, amplifier 3 = Lv4)
        this.addEffect(new MobEffectInstance(
                MobEffects.MOVEMENT_SPEED,
                Integer.MAX_VALUE,
                3,
                false,
                false,
                false
        ));
    }

    // =========================================================
    // Tick
    // =========================================================

    @Override
    public void tick() {
        // バニラCreeperの自爆処理が走らないよう、swellを常にリセット
        this.setSwellDir(-1);

        super.tick();

        if (this.level().isClientSide) {
            clientParticles();
            return;
        }

        // 念のためバフが消えていたら再付与
        ensureBuffs();

        // ---- バックステップ中 ----
        if (backstepTicks > 0) {
            tickBackstep();
            return;
        }

        LivingEntity target = this.getTarget();

        // ---- バックステップ判定 ----
        if (target != null && this.distanceToSqr(target) <= BACKSTEP_DISTANCE * BACKSTEP_DISTANCE) {
            startBackstep(target);
            return;
        }

        // ---- 攻撃対象がいる時のクリーパー召喚 ----
        if (target != null) {
            if (summonCooldown > 0) {
                summonCooldown--;
            } else {
                summonBomberCreeper(target);
                summonCooldown = SUMMON_INTERVAL;
            }
        }
    }

    private void ensureBuffs() {
        if (!this.hasEffect(MobEffects.DAMAGE_RESISTANCE)
                || this.getEffect(MobEffects.DAMAGE_RESISTANCE).getDuration() < 100) {
            applyLordBuffs();
        }
    }

    // =========================================================
    // 爆撃クリーパー召喚
    // =========================================================

    private void summonBomberCreeper(LivingEntity target) {
        if (!(this.level() instanceof ServerLevel sl)) return;

        Creeper bomber = EntityType.CREEPER.create(sl);
        if (bomber == null) return;

        // ロードの周囲 2〜4 ブロックにランダムスポーン
        double angle = this.random.nextDouble() * Math.PI * 2.0;
        double radius = 2.0 + this.random.nextDouble() * 2.0;
        double sx = this.getX() + Math.cos(angle) * radius;
        double sy = this.getY();
        double sz = this.getZ() + Math.sin(angle) * radius;

        bomber.moveTo(sx, sy, sz, this.random.nextFloat() * 360.0F, 0.0F);

        // ---- 移動速度 3倍 ----
        var speedAttr = bomber.getAttribute(Attributes.MOVEMENT_SPEED);
        if (speedAttr != null) {
            speedAttr.setBaseValue(speedAttr.getBaseValue() * SUMMON_SPEED_MULTIPLIER);
        }

        // ---- 段差上限 4倍 ----
        bomber.setMaxUpStep((float) (bomber.maxUpStep() * SUMMON_STEP_MULTIPLIER));

        // ---- プレイヤーに敵対 ----
        bomber.setTarget(target);

        // 召喚タグ
        bomber.getPersistentData().putBoolean(BOMBER_TAG, true);


        sl.addFreshEntity(bomber);

        // 召喚演出
        sl.sendParticles(
                ParticleTypes.LARGE_SMOKE,
                sx, sy + 0.5, sz,
                20, 0.3, 0.3, 0.3, 0.02
        );

        sl.playSound(
                null,
                this.getX(), this.getY(), this.getZ(),
                SoundEvents.CREEPER_PRIMED,
                SoundSource.HOSTILE,
                1.0F,
                1.4F
        );
    }

    // =========================================================
    // バックステップ
    // =========================================================

    private void startBackstep(LivingEntity target) {
        this.backstepTicks = BACKSTEP_DURATION;

        // ターゲットから離れる方向へ
        Vec3 away = this.position().subtract(target.position()).normalize();
        this.setDeltaMovement(
                away.x * BACKSTEP_POWER,
                0.35D,
                away.z * BACKSTEP_POWER
        );
        this.hurtMarked = true;

        // 演出
        if (this.level() instanceof ServerLevel sl) {
            sl.sendParticles(
                    ParticleTypes.CLOUD,
                    this.getX(), this.getY() + 0.3, this.getZ(),
                    15, 0.3, 0.2, 0.3, 0.05
            );
            sl.playSound(
                    null,
                    this.getX(), this.getY(), this.getZ(),
                    SoundEvents.ENDERMAN_TELEPORT,
                    SoundSource.HOSTILE,
                    0.8F,
                    1.5F
            );
        }
    }

    private void tickBackstep() {
        backstepTicks--;
        this.fallDistance = 0.0F;

        // 少しずつ減速
        Vec3 v = this.getDeltaMovement();
        this.setDeltaMovement(v.x * 0.85D, v.y, v.z * 0.85D);
        this.hurtMarked = true;
    }

    // =========================================================
    // 死亡時: 自爆
    // =========================================================
    //
    // Creeper の自爆処理は private でフックできないため、
    // 死亡時に自前で爆発処理を実行する。
    //
    // これにより「倒した瞬間に大爆発 + 追加効果」を実現する。

    @Override
    public void die(DamageSource source) {

        // 二重起爆防止
        if (!this.level().isClientSide
                && !this.getPersistentData().getBoolean(TAG_DETONATED)) {

            this.getPersistentData().putBoolean(TAG_DETONATED, true);

            if (this.level() instanceof ServerLevel sl) {
                performLordExplosion(sl);
            }
        }

        // 死亡前にバフ解除（見た目リセット）
        this.removeAllEffects();

        // 特別ドロップ
        if (!this.level().isClientSide) {
            this.spawnAtLocation(new ItemStack(net.minecraft.world.item.Items.DIAMOND, 3));
        }

        super.die(source);
    }

    /**
     * ロード独自の爆発処理。
     */
    private void performLordExplosion(ServerLevel sl) {

        // =========================================================
        // 1. 爆破範囲3倍 (デフォルト3.0F → 9.0F / 帯電6.0F → 18.0F)
        // =========================================================
        float radius = this.isPowered() ? 6.0F : 3.0F;
        radius *= EXPLOSION_RADIUS_MULTIPLIER;

        // =========================================================
        // 2. 通常爆発を実行
        // =========================================================
        sl.explode(
                this,
                this.getX(),
                this.getY(),
                this.getZ(),
                radius,
                Level.ExplosionInteraction.MOB
        );

        // =========================================================
        // 3. プレイヤーへ追加効果
        // =========================================================
        double r = radius + 2.0D;
        List<Player> players = sl.getEntitiesOfClass(
                Player.class,
                this.getBoundingBox().inflate(r),
                p -> p.isAlive() && !p.isCreative() && !p.isSpectator()
        );

        for (Player player : players) {

            // 貫通120ダメージ
            player.invulnerableTime = 0;
            player.hurtTime = 0;
            player.hurt(
                    player.damageSources().explosion(this, this),
                    EXPLOSION_PIERCE_DAMAGE
            );

            // 鈍足255 + ウィザー255 を10秒
            player.addEffect(new MobEffectInstance(
                    MobEffects.MOVEMENT_SLOWDOWN,
                    EXPLOSION_DEBUFF_DURATION,
                    254,
                    false,
                    true,
                    true
            ));
            player.addEffect(new MobEffectInstance(
                    MobEffects.WITHER,
                    EXPLOSION_DEBUFF_DURATION,
                    254,
                    false,
                    true,
                    true
            ));

            player.displayClientMessage(
                    Component.literal("§4§lクリーパーロードの自爆… §c魂を焼かれた…"),
                    true
            );
        }

        // =========================================================
        // 4. 演出
        // =========================================================
        sl.sendParticles(
                ParticleTypes.EXPLOSION_EMITTER,
                this.getX(), this.getY() + 0.5, this.getZ(),
                3, 1.0, 0.5, 1.0, 0
        );
        sl.sendParticles(
                ParticleTypes.SOUL_FIRE_FLAME,
                this.getX(), this.getY() + 0.5, this.getZ(),
                80, 2.0, 1.0, 2.0, 0.1
        );
        sl.playSound(
                null,
                this.getX(), this.getY(), this.getZ(),
                SoundEvents.WITHER_SPAWN,
                SoundSource.HOSTILE,
                2.0F,
                0.6F
        );
    }

    // =========================================================
    // クライアント演出
    // =========================================================

    private void clientParticles() {
        // ロード自身は常に紫オーラを纏う
        if (this.tickCount % 4 == 0) {
            this.level().addParticle(
                    ParticleTypes.SOUL,
                    this.getX() + (this.random.nextDouble() - 0.5) * 1.2,
                    this.getY() + this.random.nextDouble() * 1.8,
                    this.getZ() + (this.random.nextDouble() - 0.5) * 1.2,
                    0, 0.02, 0
            );
        }
    }

    // =========================================================
    // 内部 Goal: swell制御（今回は常に -1 で自爆しない）
    // =========================================================

    private static class CreeperLordSwellGoal extends Goal {
        private final CreeperLordEntity creeper;

        public CreeperLordSwellGoal(CreeperLordEntity creeper) {
            this.creeper = creeper;
            this.setFlags(EnumSet.of(Flag.MOVE));
        }

        @Override
        public boolean canUse() {
            // 常に false にして swell を進めさせない
            // （自爆は die() 側で自前処理する）
            return false;
        }

        @Override
        public void tick() {
            this.creeper.setSwellDir(-1);
        }
    }
}