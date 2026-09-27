package com.tyami.forlaism.entity;

import com.tyami.forlaism.registry.ModEntityTypes;

import net.minecraft.core.BlockPos;
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
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.EnderMan;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;
import java.util.List;

/**
 * エンダーマンロード。
 *
 * 中ボス。
 *
 * - エンダーマンスポーン時に 1% で置換出現
 * - HP300 / 防御30 / 移動速度1.5倍 / 段差上限3倍
 * - 周囲の水を空気に変える（水没対策）
 * - プレイヤーから10マス離れると背後にTP
 * - プレイヤーが盾を構えるとクリーパーを背後に置いて離脱
 * - 飛来する矢を回避
 * - プレイヤーがエンダーパールを使うと激怒 → 背後に回り込み「断罪」自爆
 * - HP50%以下: 10秒ごとに1秒拘束
 * - HP25%以下: 8秒ごとにプレイヤーを爆破
 * - HP15%以下: 6秒ごとにプレイヤーを10m上空へTP
 * - 儀式の短剣でエンダーマンを殺すと確定召喚
 */
public class EndermanLordEntity extends EnderMan {

    // =========================================================
    // 定数
    // =========================================================

    /** 背後TPを開始する距離。 */
    private static final double TELEPORT_DISTANCE = 10.0D;

    /** TPクールダウン (tick)。 */
    private static final int TELEPORT_COOLDOWN = 40;

    /** 激怒（断罪）のクールダウン (tick)。 */
    private static final int WRATH_COOLDOWN = 200;

    /** 水を消す半径。 */
    private static final int WATER_CLEAR_RADIUS = 4;

    /** 水消し処理の間隔 (tick)。 */
    private static final int WATER_CLEAR_INTERVAL = 10;

    /** 拘束時間 (tick)。1秒 = 20 tick。 */
    private static final int BIND_DURATION = 20;

    /** HP50%以下の拘束間隔。 */
    private static final int BIND_INTERVAL = 200;

    /** HP25%以下の爆破間隔。 */
    private static final int EXPLODE_INTERVAL = 160;

    /** HP15%以下の上空TP間隔。 */
    private static final int SKY_TP_INTERVAL = 120;

    /** 上空TPの高さ。 */
    private static final double SKY_TP_HEIGHT = 10.0D;

    /** 爆破ダメージ。 */
    private static final float EXPLODE_DAMAGE = 20.0F;

    /** 断罪自爆ダメージ（確定殺）。 */
    private static final float WRATH_DAMAGE = Float.MAX_VALUE;

    /** 眷属召喚（クリーパー）時に離れる距離。 */
    private static final double CREEPER_PLACE_DISTANCE = 2.5D;

    // =========================================================
    // 状態
    // =========================================================

    private int teleportCooldown = 0;
    private int wrathCooldown = 0;
    private int waterClearTimer = 0;

    private int bindTimer = BIND_INTERVAL;
    private int explodeTimer = EXPLODE_INTERVAL;
    private int skyTpTimer = SKY_TP_INTERVAL;

    /** 断罪モード中フラグ。 */
    private boolean wrathMode = false;
    private int wrathTicks = 0;

    /** 召喚時に1回だけ実行するためのタグ。 */
    private static final String TAG_SUMMONED = "ForlaismEndermanLordSummoned";

    public EndermanLordEntity(EntityType<? extends EnderMan> type, Level level) {
        super(type, level);
        this.xpReward = 150;
        this.setPersistenceRequired();
    }

    // =========================================================
    // 属性
    // =========================================================

    public static AttributeSupplier.Builder createAttributes() {
        return EnderMan.createAttributes()
                .add(Attributes.MAX_HEALTH, 300.0D)
                .add(Attributes.ARMOR, 30.0D)
                .add(Attributes.ARMOR_TOUGHNESS, 12.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.45D) // 0.3 * 1.5
                .add(Attributes.ATTACK_DAMAGE, 12.0D)
                .add(Attributes.KNOCKBACK_RESISTANCE, 1.0D)
                .add(Attributes.FOLLOW_RANGE, 128.0D);
    }

    // =========================================================
    // AI
    // =========================================================

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(1, new MeleeAttackGoal(this, 1.0D, false));
        this.goalSelector.addGoal(7, new LookAtPlayerGoal(this, Player.class, 64.0F));

        this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
    }

    @Override
    public boolean isBaby() {
        return false;
    }

    // =========================================================
    // スポーン時: 段差上限3倍
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

        // 段差上限3倍
        this.setMaxUpStep(this.maxUpStep() * 3.0F);

        return result;
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

        // クールダウン
        if (teleportCooldown > 0) teleportCooldown--;
        if (wrathCooldown > 0) wrathCooldown--;

        // ---- 水を空気に ----
        if (++waterClearTimer >= WATER_CLEAR_INTERVAL) {
            waterClearTimer = 0;
            clearNearbyWater();
        }

        // ---- 断罪モード中 ----
        if (wrathMode) {
            tickWrathMode();
            return;
        }

        LivingEntity target = this.getTarget();
        if (target == null) return;

        double distSqr = this.distanceToSqr(target);
        float hpRatio = this.getHealth() / this.getMaxHealth();

        // ---- 盾を構えられたらクリーパーを後ろに置いて離脱 ----
        if (target instanceof Player player && player.isBlocking()) {
            if (teleportCooldown <= 0) {
                placeCreeperBehindAndRetreat(player);
                teleportCooldown = TELEPORT_COOLDOWN;
                return;
            }
        }

        // ---- 10マス離れたら背後にTP ----
        if (distSqr >= TELEPORT_DISTANCE * TELEPORT_DISTANCE && teleportCooldown <= 0) {
            teleportBehindTarget(target);
            teleportCooldown = TELEPORT_COOLDOWN;
            return;
        }

        // ---- HP50%以下: 10秒ごとに1秒拘束 ----
        if (hpRatio <= 0.50F && --bindTimer <= 0) {
            bindTimer = BIND_INTERVAL;
            if (target instanceof Player player) {
                bindPlayer(player, BIND_DURATION);
            }
        }

        // ---- HP25%以下: 8秒ごとに爆破 ----
        if (hpRatio <= 0.25F && --explodeTimer <= 0) {
            explodeTimer = EXPLODE_INTERVAL;
            explodePlayer(target);
        }

        // ---- HP15%以下: 6秒ごとに上空TP ----
        if (hpRatio <= 0.15F && --skyTpTimer <= 0) {
            skyTpTimer = SKY_TP_INTERVAL;
            if (target instanceof Player player) {
                teleportPlayerToSky(player);
            }
        }
    }

    // =========================================================
    // クライアント演出
    // =========================================================

    private void clientParticles() {
        if (this.tickCount % 3 == 0) {
            this.level().addParticle(
                    ParticleTypes.PORTAL,
                    this.getX() + (this.random.nextDouble() - 0.5) * 1.5,
                    this.getY() + this.random.nextDouble() * 2.5,
                    this.getZ() + (this.random.nextDouble() - 0.5) * 1.5,
                    0, 0.05, 0
            );
        }
    }

    // =========================================================
    // 水を空気に
    // =========================================================

    private void clearNearbyWater() {
        BlockPos center = this.blockPosition();

        for (int dx = -WATER_CLEAR_RADIUS; dx <= WATER_CLEAR_RADIUS; dx++) {
            for (int dy = -WATER_CLEAR_RADIUS; dy <= WATER_CLEAR_RADIUS; dy++) {
                for (int dz = -WATER_CLEAR_RADIUS; dz <= WATER_CLEAR_RADIUS; dz++) {
                    BlockPos pos = center.offset(dx, dy, dz);
                    FluidState fluid = this.level().getFluidState(pos);

                    if (fluid.getType() == Fluids.WATER
                            || fluid.getType() == Fluids.FLOWING_WATER) {
                        this.level().setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
                    }
                }
            }
        }
    }

    // =========================================================
    // 背後TP
    // =========================================================

    private void teleportBehindTarget(LivingEntity target) {
        Vec3 look = target.getLookAngle().normalize();
        Vec3 behind = target.position().subtract(look.scale(2.0));

        BlockPos behindPos = BlockPos.containing(behind);
        boolean safe = this.level().getBlockState(behindPos).isAir()
                && this.level().getBlockState(behindPos.above()).isAir();

        Vec3 dest = safe ? behind : target.position();

        spawnTeleportParticles(this.getX(), this.getY(), this.getZ());
        this.teleportTo(dest.x, dest.y, dest.z);
        spawnTeleportParticles(dest.x, dest.y, dest.z);

        this.lookAt(target, 360.0F, 360.0F);
    }

    private void spawnTeleportParticles(double x, double y, double z) {
        if (this.level() instanceof ServerLevel sl) {
            sl.sendParticles(
                    ParticleTypes.PORTAL,
                    x, y + 1.0, z,
                    30, 0.5, 0.5, 0.5, 0.2
            );
            sl.playSound(null, x, y, z,
                    SoundEvents.ENDERMAN_TELEPORT,
                    SoundSource.HOSTILE, 1.0F, 1.4F);
        }
    }

    // =========================================================
    // 盾を構えられたらクリーパー配置 + 離脱
    // =========================================================

    private void placeCreeperBehindAndRetreat(Player player) {
        if (!(this.level() instanceof ServerLevel sl)) return;

        Vec3 look = player.getLookAngle().normalize();
        Vec3 behind = player.position().subtract(look.scale(CREEPER_PLACE_DISTANCE));

        var creeper = EntityType.CREEPER.create(sl);
        if (creeper != null) {
            creeper.moveTo(behind.x, behind.y, behind.z, 0, 0);
            creeper.setTarget(player);
            sl.addFreshEntity(creeper);

            sl.sendParticles(ParticleTypes.POOF,
                    behind.x, behind.y + 0.5, behind.z,
                    20, 0.3, 0.3, 0.3, 0.05);
        }

        // 自分は離脱（前方へ大きくTP）
        Vec3 away = this.position().subtract(player.position()).normalize();
        Vec3 dest = this.position().add(away.scale(6.0));

        spawnTeleportParticles(this.getX(), this.getY(), this.getZ());
        this.teleportTo(dest.x, dest.y, dest.z);
        spawnTeleportParticles(dest.x, dest.y, dest.z);
    }

    // =========================================================
    // 矢の回避
    // =========================================================

    @Override
    public boolean hurt(DamageSource source, float amount) {
        // 矢を受けたら回避（無効化 + TP）
        if (source.getDirectEntity() instanceof AbstractArrow) {
            if (!this.level().isClientSide && teleportCooldown <= 0) {
                dodgeArrow(source.getDirectEntity());
                teleportCooldown = TELEPORT_COOLDOWN / 2;
            }
            return false;
        }

        // エンダーパール使用を検知（プレイヤーがパールを持って投げた場合）
        if (source.getEntity() instanceof Player player
                && player.isUsingItem()
                && player.getUseItem().is(net.minecraft.world.item.Items.ENDER_PEARL)) {
            triggerWrath(player);
        }

        return super.hurt(source, amount);
    }

    private void dodgeArrow(Entity arrow) {
        Vec3 arrowDir = arrow.getDeltaMovement().normalize();
        // 矢の直交方向へランダムにTP
        Vec3 perp = new Vec3(-arrowDir.z, 0, arrowDir.x).normalize();
        double sign = this.random.nextBoolean() ? 1 : -1;
        Vec3 dest = this.position().add(perp.scale(2.0 * sign));

        spawnTeleportParticles(this.getX(), this.getY(), this.getZ());
        this.teleportTo(dest.x, dest.y, dest.z);
        spawnTeleportParticles(dest.x, dest.y, dest.z);
    }

    // =========================================================
    // 激怒（断罪）
    // =========================================================

    /**
     * プレイヤーがエンダーパールを使うと激怒。
     */
    public void triggerWrath(Player player) {
        if (wrathMode || wrathCooldown > 0) return;

        wrathMode = true;
        wrathTicks = 40; // 2秒の猶予
        wrathCooldown = WRATH_COOLDOWN;

        if (player.level() instanceof ServerLevel sl) {
            sl.playSound(null, this.getX(), this.getY(), this.getZ(),
                    SoundEvents.ENDER_DRAGON_GROWL,
                    SoundSource.HOSTILE, 3.0F, 0.5F);

            sl.sendParticles(ParticleTypes.DRAGON_BREATH,
                    this.getX(), this.getY() + 1.5, this.getZ(),
                    100, 2.0, 1.5, 2.0, 0.1);
        }

        player.displayClientMessage(
                Component.literal("§5§lエンダーマンロードが激怒した… §c「断罪」"),
                true
        );
    }

    private void tickWrathMode() {
        wrathTicks--;

        if (wrathTicks <= 0) {
            // 背後に回り込んで断罪自爆
            LivingEntity target = this.getTarget();

            if (target instanceof Player player && player.isAlive()) {
                // 背後へTP
                Vec3 look = player.getLookAngle().normalize();
                Vec3 behind = player.position().subtract(look.scale(1.5));
                this.teleportTo(behind.x, behind.y, behind.z);

                // 断罪自爆
                if (player.level() instanceof ServerLevel sl) {
                    sl.explode(this,
                            this.getX(), this.getY(), this.getZ(),
                            5.0F, Level.ExplosionInteraction.MOB);

                    // 無敵貫通確定殺
                    player.invulnerableTime = 0;
                    player.hurtTime = 0;
                    player.setHealth(0.0F);
                    if (!player.isDeadOrDying()) {
                        player.die(player.damageSources().explosion(this, this));
                    }

                    player.displayClientMessage(
                            Component.literal("§4§l断罪… 汝の魂は消え去った…"),
                            true
                    );
                }
            }

            // 自分も死ぬ
            this.setHealth(0.0F);
            if (!this.isDeadOrDying()) {
                this.die(this.damageSources().genericKill());
            }

            wrathMode = false;
        }
    }

    // =========================================================
    // HP50%: 拘束
    // =========================================================

    private void bindPlayer(Player player, int duration) {
        player.addEffect(new MobEffectInstance(
                MobEffects.MOVEMENT_SLOWDOWN,
                duration,
                254, // レベル255
                false, true, true
        ));
        player.addEffect(new MobEffectInstance(
                MobEffects.JUMP,
                duration,
                250, // ジャンプ不能
                false, true, true
        ));

        if (this.level() instanceof ServerLevel sl) {
            sl.playSound(null, player.getX(), player.getY(), player.getZ(),
                    SoundEvents.ELDER_GUARDIAN_CURSE,
                    SoundSource.HOSTILE, 1.0F, 0.5F);

            sl.sendParticles(ParticleTypes.SCULK_SOUL,
                    player.getX(), player.getY() + 1.0, player.getZ(),
                    50, 1.0, 1.0, 1.0, 0.05);
        }

        player.displayClientMessage(
                Component.literal("§5エンダーマンロードに §d拘束 §fされた…"),
                true
        );
    }

    // =========================================================
    // HP25%: 爆破
    // =========================================================

    private void explodePlayer(LivingEntity target) {
        if (!(target.level() instanceof ServerLevel sl)) return;

        Vec3 pos = target.position();

        sl.explode(this,
                pos.x, pos.y, pos.z,
                4.0F, Level.ExplosionInteraction.MOB);

        // 無敵貫通ダメージ
        target.invulnerableTime = 0;
        target.hurtTime = 0;
        target.hurt(target.damageSources().explosion(this, this), EXPLODE_DAMAGE);

        if (target instanceof Player player) {
            player.displayClientMessage(
                    Component.literal("§4エンダーマンロードが §c爆破 §fを起こした！"),
                    true
            );
        }
    }

    // =========================================================
    // HP15%: 上空TP
    // =========================================================

    private void teleportPlayerToSky(Player player) {
        if (!(player.level() instanceof ServerLevel sl)) return;

        double x = player.getX();
        double y = player.getY() + SKY_TP_HEIGHT;
        double z = player.getZ();

        // 上空の安全位置を探す
        player.teleportTo(x, y, z);

        sl.playSound(null, x, y, z,
                SoundEvents.ENDERMAN_TELEPORT,
                SoundSource.HOSTILE, 1.5F, 0.5F);

        sl.sendParticles(ParticleTypes.PORTAL,
                x, y, z,
                50, 0.5, 0.5, 0.5, 0.3);

        player.displayClientMessage(
                Component.literal("§5エンダーマンロードに §d上空へ飛ばされた…"),
                true
        );
    }

    // =========================================================
    // 死亡ドロップ
    // =========================================================

    @Override
    public void die(DamageSource source) {
        if (!this.level().isClientSide && this.level() instanceof ServerLevel sl) {
            // エンダーパール大量ドロップ
            this.spawnAtLocation(new ItemStack(net.minecraft.world.item.Items.ENDER_PEARL, 32));

            sl.sendParticles(ParticleTypes.EXPLOSION_EMITTER,
                    this.getX(), this.getY() + 1, this.getZ(),
                    1, 0, 0, 0, 0);
        }
        super.die(source);
    }

    // =========================================================
    // NBT
    // =========================================================

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putBoolean("WrathMode", wrathMode);
        tag.putInt("WrathTicks", wrathTicks);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        wrathMode = tag.getBoolean("WrathMode");
        wrathTicks = tag.getInt("WrathTicks");
    }
}