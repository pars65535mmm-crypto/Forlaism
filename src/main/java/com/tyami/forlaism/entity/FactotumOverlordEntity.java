package com.tyami.forlaism.entity;

import com.tyami.forlaism.registry.ModEntityTypes;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;
import java.util.List;

/**
 * Factotum Overlord（万能なる君主の上位存在）。
 *
 * - HP: 255
 * - 防御: 10以上のダメージを10に丸める
 * - 不死: persistentData の "ForlaismImmortal" フラグで管理
 *   - デフォルト true
 *   - 殴られた時に HP 1 以下なら false に解除 → 次で死ぬ
 * - デスポーンしない
 * - プレイヤー接近で起動・敵対
 * - 攻撃方法1: 眷属召喚（HP70%以上）
 * - 攻撃方法2: ビーム乱射（HP50%以下）
 */
public class FactotumOverlordEntity extends Monster {

    /** 不死フラグのキー。 */
    public static final String IMMORTAL_TAG = "ForlaismImmortal";

    /** 眷属タグのキー。 */
    public static final String MINION_TAG = "ForlaismOverlordMinion";

    /** 起動距離（ブロック）。 */
    private static final double ACTIVATION_RANGE = 32.0D;

    /** HP70%閾値。 */
    private static final float SUMMON_PHASE_THRESHOLD = 0.70F;

    /** HP50%閾値。 */
    private static final float BEAM_PHASE_THRESHOLD = 0.50F;

    /** 眷属の上限。 */
    public static final int MINION_LIMIT = 100;

    /** 一度に召喚する数。 */
    public static final int MINIONS_PER_SUMMON = 10;

    /** ビームダメージ。 */
    public static final float BEAM_DAMAGE = 5.0F;

    /** ビーム射程。 */
    public static final double BEAM_RANGE = 24.0D;

    /** 召喚クールダウン（tick）。HP満タン時は長め。 */
    private static final int SUMMON_COOLDOWN_MAX = 200;  // 10秒
    private static final int SUMMON_COOLDOWN_MIN = 60;   // 3秒

    /** ビームクールダウン（tick）。 */
    private static final int BEAM_COOLDOWN = 40;         // 2秒

    // =========================================================
    // Synched Data
    // =========================================================

    private static final EntityDataAccessor<Boolean> DATA_ACTIVATED =
            SynchedEntityData.defineId(FactotumOverlordEntity.class, EntityDataSerializers.BOOLEAN);

    private static final EntityDataAccessor<Integer> DATA_MINION_COUNT =
            SynchedEntityData.defineId(FactotumOverlordEntity.class, EntityDataSerializers.INT);

    // =========================================================
    // 内部状態
    // =========================================================

    /** 起動済みフラグ。 */
    private boolean activated = false;

    /** 眷属召喚クールダウン。 */
    private int summonCooldown = 0;

    /** ビームクールダウン。 */
    private int beamCooldown = 0;

    /** ビーム回転中のフラグ。 */
    private boolean beamSpinning = false;

    /** ビーム回転残り tick。 */
    private int beamSpinTicks = 0;

    public FactotumOverlordEntity(EntityType<? extends Monster> type, Level level) {
        super(type, level);
        this.setPersistenceRequired();  // デスポーンしない
        this.xpReward = 500;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 1048576.0D)
                .add(Attributes.ARMOR, 30.0D)
                .add(Attributes.ARMOR_TOUGHNESS, 20.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.28D)
                .add(Attributes.ATTACK_DAMAGE, 20.0D)
                .add(Attributes.FOLLOW_RANGE, 128.0D)
                .add(Attributes.KNOCKBACK_RESISTANCE, 1.0D);
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(DATA_ACTIVATED, false);
        this.entityData.define(DATA_MINION_COUNT, 0);
    }

    public boolean isActivated() {
        return this.entityData.get(DATA_ACTIVATED);
    }

    public void setActivated(boolean value) {
        this.entityData.set(DATA_ACTIVATED, value);
        this.activated = value;
    }

    public int getMinionCount() {
        return this.entityData.get(DATA_MINION_COUNT);
    }

    public void setMinionCount(int count) {
        this.entityData.set(DATA_MINION_COUNT, Math.max(0, count));
    }

    // =========================================================
    // 不死フラグ
    // =========================================================

    /**
     * 不死かどうか。
     *
     * 未設定の場合はデフォルト true を返す。
     */
    public boolean isImmortal() {
        CompoundTag data = this.getPersistentData();
        if (!data.contains(IMMORTAL_TAG)) {
            data.putBoolean(IMMORTAL_TAG, true);
            return true;
        }
        return data.getBoolean(IMMORTAL_TAG);
    }

    /**
     * 不死フラグを設定。
     */
    public void setImmortal(boolean immortal) {
        this.getPersistentData().putBoolean(IMMORTAL_TAG, immortal);
    }

    // =========================================================
    // 即死耐性
    // =========================================================

    @Override
    public void kill() {
        // 不死中は無効
        if (isImmortal()) return;

        super.kill();
    }

    @Override
    public void remove(Entity.RemovalReason reason) {
        // KILLED以外、または不死中は無効
        if (isImmortal() && reason != Entity.RemovalReason.KILLED) {
            return;
        }
        super.remove(reason);
    }

    @Override
    public boolean isInvulnerableTo(DamageSource source) {
        // 奈落・虚空・kill 系は無効
        if (source.is(net.minecraft.tags.DamageTypeTags.BYPASSES_INVULNERABILITY)) {
            return true;
        }
        return super.isInvulnerableTo(source);
    }

    /**
     * 10以上のダメージを10に丸める。
     *
     * さらに、HPが1以下になるほどのダメージを受けたら
     * 不死フラグを解除する。
     */
    @Override
    public boolean hurt(DamageSource source, float amount) {
        float rounded = Math.min(amount, 10.0F);
        boolean result = super.hurt(source, rounded);

        // =========================================================
        // 不死解除判定： HP が 1 以下なら解除
        // =========================================================
        if (!this.level().isClientSide && isImmortal() && this.getHealth() <= 1.0F) {
            setImmortal(false);

            // 演出：解除の瞬間
            if (this.level() instanceof ServerLevel serverLevel) {
                serverLevel.playSound(
                        null,
                        this.blockPosition(),
                        SoundEvents.WITHER_SPAWN,
                        SoundSource.HOSTILE,
                        1.5F,
                        0.5F
                );

                serverLevel.sendParticles(
                        ParticleTypes.SOUL_FIRE_FLAME,
                        this.getX(), this.getY() + 1, this.getZ(),
                        80, 2.0, 2.0, 2.0, 0.1
                );

                serverLevel.sendParticles(
                        ParticleTypes.SCULK_SOUL,
                        this.getX(), this.getY() + 1, this.getZ(),
                        40, 2.0, 2.0, 2.0, 0.05
                );
            }

            this.setGlowingTag(true);
        }

        // =========================================================
        // HP 0 になったら本当に死ぬ
        // =========================================================
        if (!this.level().isClientSide && this.getHealth() <= 0.0F) {
            this.die(source);
        }

        return result;
    }

    // =========================================================
    // デスポーン無効化
    // =========================================================

    @Override
    public boolean removeWhenFarAway(double distance) {
        return false;
    }

    @Override
    public void checkDespawn() {
        // デスポーンしない
    }

    // =========================================================
    // 死亡処理
    // =========================================================

    @Override
    public void die(DamageSource source) {

        // 不死フラグが ON なら死なない
        if (isImmortal()) {
            if (this.getHealth() <= 0.0F) {
                this.setHealth(1.0F);
            }
            return;
        }

        // =========================================================
        // 本当に死ぬ処理
        // =========================================================
        if (!this.level().isClientSide && this.level() instanceof ServerLevel serverLevel) {

            // 眷属を全員消す
            List<LivingEntity> minions = serverLevel.getEntitiesOfClass(
                    LivingEntity.class,
                    this.getBoundingBox().inflate(512.0D),
                    e -> e.getPersistentData().contains(MINION_TAG)
            );

            for (LivingEntity minion : minions) {
                // 眷属の光輪を外す → MinionHaloMixin の die() キャンセル回避
                minion.setItemSlot(EquipmentSlot.HEAD, ItemStack.EMPTY);
                // 不死フラグも OFF
                minion.getPersistentData().putBoolean(IMMORTAL_TAG, false);
                minion.discard();
                minion.remove(Entity.RemovalReason.KILLED);
            }

            // パラドックスをドロップ
            this.spawnAtLocation(
                    new ItemStack(com.tyami.forlaism.registry.Items.PARADOX.get())
            );

            // 死亡演出
            serverLevel.sendParticles(
                    ParticleTypes.EXPLOSION_EMITTER,
                    this.getX(), this.getY() + 1, this.getZ(),
                    1, 0, 0, 0, 0
            );

            serverLevel.playSound(
                    null,
                    this.blockPosition(),
                    SoundEvents.WARDEN_DEATH,
                    SoundSource.HOSTILE,
                    2.0F,
                    0.5F
            );
        }

        super.die(source);
    }

    // =========================================================
    // Tick
    // =========================================================

    @Override
    public void tick() {
        super.tick();

        if (this.level().isClientSide) {
            // クライアント側：パーティクル演出
            if (this.isActivated() && this.tickCount % 5 == 0) {
                this.level().addParticle(
                        ParticleTypes.ENCHANT,
                        this.getX() + (this.random.nextDouble() - 0.5) * 4,
                        this.getY() + this.random.nextDouble() * 4,
                        this.getZ() + (this.random.nextDouble() - 0.5) * 4,
                        0, 0.05, 0
                );
            }
            return;
        }

        // =========================================================
        // 起動チェック
        // =========================================================
        if (!isActivated()) {
            Player nearest = this.level().getNearestPlayer(this, ACTIVATION_RANGE);
            if (nearest != null && !nearest.isCreative() && !nearest.isSpectator()) {
                setActivated(true);
                activate();
            }
            return;
        }

        // =========================================================
        // 眷属数カウント更新
        // =========================================================
        updateMinionCount();

        // =========================================================
        // クールダウン
        // =========================================================
        if (summonCooldown > 0) summonCooldown--;
        if (beamCooldown > 0) beamCooldown--;

        // =========================================================
        // ビーム回転中
        // =========================================================
        if (beamSpinning) {
            beamSpinTicks--;
            performBeamAttack();

            if (beamSpinTicks <= 0) {
                beamSpinning = false;
            }
            return;
        }

        // =========================================================
        // 攻撃フェーズ判定
        // =========================================================
        float hpRatio = this.getHealth() / this.getMaxHealth();

        // ビームフェーズ（HP50%以下）: 優先
        if (hpRatio <= BEAM_PHASE_THRESHOLD && beamCooldown <= 0) {
            startBeamAttack();
            return;
        }

        // 眷属召喚フェーズ
        if (summonCooldown <= 0 && getMinionCount() < MINION_LIMIT) {
            summonMinions();
            return;
        }

        // 通常時：近くのプレイヤーを追いかける
        Player target = this.level().getNearestPlayer(this, 64.0D);
        if (target != null && !target.isCreative() && !target.isSpectator()) {
            this.getLookControl().setLookAt(target, 30.0F, 30.0F);
        }
    }

    /**
     * 起動時の演出。
     */
    private void activate() {
        if (!(this.level() instanceof ServerLevel serverLevel)) return;

        serverLevel.playSound(
                null,
                this.blockPosition(),
                SoundEvents.WARDEN_EMERGE,
                SoundSource.HOSTILE,
                2.0F,
                0.6F
        );

        serverLevel.sendParticles(
                ParticleTypes.SCULK_SOUL,
                this.getX(), this.getY() + 1, this.getZ(),
                100, 2.0, 2.0, 2.0, 0.1
        );

        this.setGlowingTag(true);
    }

    /**
     * 眷属数を数える。
     */
    private void updateMinionCount() {
        if (!(this.level() instanceof ServerLevel serverLevel)) return;

        List<LivingEntity> minions = serverLevel.getEntitiesOfClass(
                LivingEntity.class,
                this.getBoundingBox().inflate(256.0D),
                e -> e != this
                        && e.isAlive()
                        && e.getPersistentData().contains(MINION_TAG)
        );

        setMinionCount(minions.size());
    }

    /**
     * 眷属召喚。
     */
    private void summonMinions() {
        if (!(this.level() instanceof ServerLevel serverLevel)) return;

        int current = getMinionCount();
        int canSummon = Math.min(MINIONS_PER_SUMMON, MINION_LIMIT - current);

        for (int i = 0; i < canSummon; i++) {
            spawnMinion(serverLevel, i);
        }

        // クールダウン設定（HPが減るほど短く）
        float hpRatio = this.getHealth() / this.getMaxHealth();
        int cooldown = (int) (SUMMON_COOLDOWN_MAX * hpRatio
                + SUMMON_COOLDOWN_MIN * (1.0F - hpRatio));
        summonCooldown = cooldown;

        // 演出
        serverLevel.playSound(
                null,
                this.blockPosition(),
                SoundEvents.EVOKER_CAST_SPELL,
                SoundSource.HOSTILE,
                2.0F,
                0.7F
        );

        serverLevel.sendParticles(
                ParticleTypes.SOUL_FIRE_FLAME,
                this.getX(), this.getY() + 1, this.getZ(),
                50, 3.0, 2.0, 3.0, 0.05
        );
    }

    /**
     * 眷属1体を召喚。
     */
    private void spawnMinion(ServerLevel level, int index) {
        EntityType<?> type = switch (index % 3) {
            case 0 -> EntityType.HUSK;
            case 1 -> EntityType.SKELETON;
            default -> EntityType.WITHER_SKELETON;
        };

        Entity entity = type.create(level);
        if (!(entity instanceof Mob mob)) return;

        // スポーン位置：ボス周囲 3〜6 ブロック
        double angle = (index / (double) MINIONS_PER_SUMMON) * Math.PI * 2.0;
        double radius = 3.0 + this.random.nextDouble() * 3.0;

        double spawnX = this.getX() + Math.cos(angle) * radius;
        double spawnY = this.getY();
        double spawnZ = this.getZ() + Math.sin(angle) * radius;

        mob.moveTo(spawnX, spawnY, spawnZ, this.random.nextFloat() * 360.0F, 0.0F);

        // 性能10倍（HP・攻撃力）+ 移動速度 2倍
        applyMinionBuffs(mob);

        // 眷属の光輪を頭に装備
        ItemStack halo = new ItemStack(com.tyami.forlaism.registry.Items.MINION_HALO.get());
        mob.setItemSlot(EquipmentSlot.HEAD, halo);

        // タグ付け（ボスの眷属と識別）
        mob.getPersistentData().putBoolean(MINION_TAG, true);

        // 不死フラグを立てる（デフォルトで true が入るはずだが念のため）
        mob.getPersistentData().putBoolean(IMMORTAL_TAG, true);

        // スポーン
        level.addFreshEntity(mob);

        // スポーン演出
        level.sendParticles(
                ParticleTypes.SOUL,
                spawnX, spawnY + 1, spawnZ,
                15, 0.3, 0.5, 0.3, 0.05
        );
    }

    /**
     * 眷属に性能バフを付与。
     *
     * HP×10, 攻撃力×10, 移動速度×2
     */
    private static void applyMinionBuffs(Mob mob) {
        // HP ×10
        var maxHealthAttr = mob.getAttribute(Attributes.MAX_HEALTH);
        if (maxHealthAttr != null) {
            double base = maxHealthAttr.getBaseValue();
            maxHealthAttr.setBaseValue(base * 10.0D);
            mob.setHealth((float) maxHealthAttr.getValue());
        }

        // 攻撃力 ×10
        var attackAttr = mob.getAttribute(Attributes.ATTACK_DAMAGE);
        if (attackAttr != null) {
            attackAttr.setBaseValue(attackAttr.getBaseValue() * 10.0D);
        }

        // 移動速度 ×2
        var speedAttr = mob.getAttribute(Attributes.MOVEMENT_SPEED);
        if (speedAttr != null) {
            speedAttr.setBaseValue(speedAttr.getBaseValue() * 2.0D);
        }
    }

    /**
     * ビーム攻撃開始。
     */
    private void startBeamAttack() {
        beamSpinning = true;
        beamSpinTicks = 60;  // 3秒間
        beamCooldown = BEAM_COOLDOWN + 60;

        if (this.level() instanceof ServerLevel serverLevel) {
            serverLevel.playSound(
                    null,
                    this.blockPosition(),
                    SoundEvents.WARDEN_SONIC_CHARGE,
                    SoundSource.HOSTILE,
                    2.0F,
                    0.8F
            );
        }
    }

    /**
     * ビーム乱射。
     */
    private void performBeamAttack() {
        if (!(this.level() instanceof ServerLevel serverLevel)) return;

        // 回転速度（高速回転）
        this.setYRot(this.getYRot() + 30.0F);

        // 発射レート：2tickに1回
        if (beamSpinTicks % 2 != 0) return;

        // 全方位 12方向
        int beams = 12;
        for (int i = 0; i < beams; i++) {
            double angle = Math.toRadians(this.getYRot() + (360.0 / beams) * i);
            Vec3 dir = new Vec3(Math.cos(angle), 0, Math.sin(angle)).normalize();
            shootBeamRay(serverLevel, dir);
        }

        // 演出
        serverLevel.sendParticles(
                ParticleTypes.SONIC_BOOM,
                this.getX(), this.getY() + 1, this.getZ(),
                5, 0.5, 0.5, 0.5, 0
        );
    }

    /**
     * ビームのレイを1本飛ばす。
     *
     * 無敵時間貫通5ダメージ。
     */
    private void shootBeamRay(ServerLevel level, Vec3 dir) {
        Vec3 start = this.position().add(0, 1.0, 0);
        double step = 0.5;

        for (double d = 0; d < BEAM_RANGE; d += step) {
            Vec3 pos = start.add(dir.scale(d));

            // パーティクル
            if (d % 1.0 < step) {
                level.sendParticles(
                        ParticleTypes.SONIC_BOOM,
                        pos.x, pos.y, pos.z,
                        1, 0, 0, 0, 0
                );
            }

            // 当たり判定
            AABB box = new AABB(
                    pos.x - 0.5, pos.y - 0.5, pos.z - 0.5,
                    pos.x + 0.5, pos.y + 0.5, pos.z + 0.5
            );

            List<LivingEntity> hits = level.getEntitiesOfClass(
                    LivingEntity.class,
                    box,
                    e -> e != this
                            && e.isAlive()
                            && !(e instanceof FactotumOverlordEntity)
            );

            for (LivingEntity hit : hits) {
                applyBeamDamage(hit);
            }

            // ブロックに当たったら終了
            if (!level.getBlockState(BlockPos.containing(pos)).isAir()) {
                break;
            }
        }
    }

    /**
     * ビームダメージを適用（無敵時間貫通）。
     */
    private void applyBeamDamage(LivingEntity target) {
        // 無敵時間リセット
        target.invulnerableTime = 0;
        target.hurtTime = 0;

        // 直接HPを削る（防御貫通）
        float current = target.getHealth();
        float next = Math.max(0.0F, current - BEAM_DAMAGE);
        target.setHealth(next);

        // 被弾演出
        target.hurtTime = 10;
        target.hurtDuration = 10;

        if (next <= 0.0F && !target.isDeadOrDying()) {
            target.die(this.damageSources().mobAttack(this));
        }
    }

    // =========================================================
    // NBT保存
    // =========================================================

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putBoolean("Activated", isActivated());
        tag.putBoolean(IMMORTAL_TAG, isImmortal());
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        if (tag.contains("Activated")) {
            setActivated(tag.getBoolean("Activated"));
        }
        if (tag.contains(IMMORTAL_TAG)) {
            setImmortal(tag.getBoolean(IMMORTAL_TAG));
        }
    }
}