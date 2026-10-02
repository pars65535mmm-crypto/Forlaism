package com.tyami.forlaism.entity;

import com.tyami.forlaism.damage.RinneDamageSource;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/**
 * エンドウォーデン・リバース。
 *
 * 強化版 EndWarden。
 *
 * ─ 強化内容 ─────────────────────────────────
 *   攻撃力         : 24 → 48（2倍）
 *   ソニックブーム : 輪廻ダメージ追加（+20/hit）
 *   第二形態       : 1回目のdie で怒りモードへ
 *   奥義（ULTIMATE）:
 *     ・メテオフォール（上空からソニックブーム柱）
 *     ・レーザー乱射
 *     ・テレポートを繰り返しながらレーザーを打ちまくる
 *   奥義トリガー   : 10秒ごと or ターゲットが30マス以上離れたら即
 */
public class EndWardenReverseEntity extends EndWardenEntity {

    // =========================================================
    // 同期データ
    // =========================================================

    /** 怒りモード（第二形態）フラグ。 */
    public static final EntityDataAccessor<Boolean> DATA_ANGER =
            SynchedEntityData.defineId(EndWardenReverseEntity.class, EntityDataSerializers.BOOLEAN);

    // =========================================================
    // 定数
    // =========================================================

    /** ソニックブーム1ヒットの輪廻ダメージ。 */
    private static final float RINNE_HIT_DAMAGE = 20.0F;

    /** 奥義のトリガー距離（これ以上離れたら即発動）。 */
    private static final double ULTIMATE_TRIGGER_DISTANCE = 30.0D;

    /** 怒りモード中の奥義クールダウン（tick）。10秒 = 200。 */
    private static final int ANGER_ULTIMATE_COOLDOWN = 200;

    /** メテオの数（1回の奥義で）。 */
    private static final int METEOR_COUNT = 8;

    /** メテオの落下高さ。 */
    private static final double METEOR_HEIGHT = 20.0D;

    /** メテオの柱の太さ。 */
    private static final double METEOR_RADIUS = 2.0D;

    /** メテオ1ヒットのダメージ（輪廻）。 */
    private static final float METEOR_DAMAGE = 15.0F;

    /** テレポート間隔（tick）。 */
    private static final int TELEPORT_INTERVAL = 15;

    // =========================================================
    // 状態
    // =========================================================

    /** 怒りモード中の奥義クールダウン。 */
    private int ultimateCooldown = 0;

    /** 奥義中：メテオを落とし終わったか。 */
    private boolean ultimateMeteorFired = false;

    /** 奥義中：テレポートカウンタ。 */
    private int ultimateTeleportCounter = 0;

    // =========================================================
    // コンストラクタ
    // =========================================================

    public EndWardenReverseEntity(EntityType<? extends PathfinderMob> type, Level level) {
        super(type, level);
        this.xpReward = 2048;
    }

    // =========================================================
    // 属性（攻撃力2倍）
    // =========================================================

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 2048.0D)   // HPも2倍（強化）
                .add(Attributes.ARMOR, 24.0D)
                .add(Attributes.ATTACK_DAMAGE, 48.0D)  // ★ 2倍
                .add(Attributes.MOVEMENT_SPEED, 0.45D) // 少しだけ速く
                .add(Attributes.FLYING_SPEED, 0.55D)
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
        this.entityData.define(DATA_ANGER, false);
    }

    // =========================================================
    // ゲッター/セッター
    // =========================================================

    public boolean isAngerMode() {
        return this.entityData.get(DATA_ANGER);
    }

    private void setAngerMode(boolean value) {
        if (this.entityData.get(DATA_ANGER) != value) {
            this.entityData.set(DATA_ANGER, value);
        }
    }

    // =========================================================
    // tick
    // =========================================================

    @Override
    public void tick() {
        super.tick();

        // サーバー側のみ
        if (this.level().isClientSide) {
            // クライアント：怒りモード中は赤いオーラを出す
            if (isAngerMode() && this.tickCount % 3 == 0) {
                this.level().addParticle(
                        ParticleTypes.SOUL_FIRE_FLAME,
                        this.getX() + (this.random.nextDouble() - 0.5) * 2.0,
                        this.getY() + this.random.nextDouble() * 3.0,
                        this.getZ() + (this.random.nextDouble() - 0.5) * 2.0,
                        0, 0.02, 0
                );
            }
            return;
        }

        // 奥義クールダウン
        if (this.ultimateCooldown > 0) {
            this.ultimateCooldown--;
        }

        // 怒りモード中の挙動
        if (isAngerMode()) {
            tickAngerMode((ServerLevel) this.level());
        }
    }

    // =========================================================
    // 怒りモード
    // =========================================================

    /**
     * 怒りモード中の追加挙動。
     *
     * - 常時、赤いオーラパーティクル
     * - 10秒ごと or ターゲット30マス以上離れたら奥義発動
     */
    private void tickAngerMode(ServerLevel level) {
        // 常時オーラ
        if (this.tickCount % 5 == 0) {
            level.sendParticles(
                    ParticleTypes.SOUL_FIRE_FLAME,
                    this.getX(), this.getY() + 1.0, this.getZ(),
                    5,
                    1.0, 1.5, 1.0,
                    0.02
            );
            level.sendParticles(
                    ParticleTypes.DRAGON_BREATH,
                    this.getX(), this.getY() + 1.0, this.getZ(),
                    2,
                    0.8, 1.2, 0.8,
                    0.01
            );
        }

        // 奥義発動判定
        LivingEntity target = this.getTarget();
        if (target == null || !target.isAlive()) return;

        double dist = this.distanceTo(target);

        boolean shouldTrigger =
                this.ultimateCooldown <= 0
                        || dist >= ULTIMATE_TRIGGER_DISTANCE;

        // 現在 ULTIMATE 中でなければ発動可能
        if (shouldTrigger && this.getPhase() != EndWardenAttackPhase.ULTIMATE) {
            startUltimate(level, target);
        }
    }

    /**
     * 奥義を開始する。
     */
    private void startUltimate(ServerLevel level, LivingEntity target) {
        this.ultimateCooldown = ANGER_ULTIMATE_COOLDOWN;
        this.ultimateMeteorFired = false;
        this.ultimateTeleportCounter = 0;

        // フェーズを ULTIMATE に
        forceSetPhase(EndWardenAttackPhase.ULTIMATE,
                EndWardenAttackPhase.ULTIMATE.getBaseDurationTicks());

        // 演出
        level.playSound(
                null,
                this.getX(), this.getY(), this.getZ(),
                SoundEvents.WARDEN_ROAR,
                SoundSource.HOSTILE,
                3.0F,
                0.5F
        );
        level.playSound(
                null,
                this.getX(), this.getY(), this.getZ(),
                SoundEvents.ENDER_DRAGON_GROWL,
                SoundSource.HOSTILE,
                2.0F,
                0.7F
        );

        level.sendParticles(
                ParticleTypes.EXPLOSION_EMITTER,
                this.getX(), this.getY() + 1.0, this.getZ(),
                1,
                0, 0, 0,
                0
        );

        level.sendParticles(
                ParticleTypes.SCULK_SOUL,
                this.getX(), this.getY() + 1.0, this.getZ(),
                100,
                2.0, 2.0, 2.0,
                0.2
        );

        // メッセージ
        level.getPlayers(p -> p.distanceToSqr(this) < 128 * 128)
                .forEach(p -> p.displayClientMessage(
                        net.minecraft.network.chat.Component.literal("§4§l終理の奥義… §c「万世の終焉」")
                                .withStyle(net.minecraft.ChatFormatting.DARK_RED),
                        true
                ));
    }

    /**
     * フェーズを強制変更する（親クラスの private を突破）。
     */
    private void forceSetPhase(EndWardenAttackPhase newPhase, int duration) {
        // 親クラスの setPhase は private なので、
        // リフレクションではなく、親クラスに protected setter を追加する必要がある。
        // → ここでは親クラスに protected メソッドを追加した前提で呼ぶ。
        this.applyPhase(newPhase, duration);
    }

    // =========================================================
    // 奥義本体の tick 処理（親の tickPhase をオーバーライド）
    // =========================================================

    @Override
    protected void tickPhase(ServerLevel level) {
        // ULTIMATE 以外は親に任せる
        if (this.getPhase() != EndWardenAttackPhase.ULTIMATE) {
            super.tickPhase(level);
            return;
        }

        tickUltimate(level);
    }

    /**
     * 奥義フェーズの tick。
     *
     * 0 ～ 40   : メテオフォール（上空から8本のソニックブーム柱）
     * 40 ～ 200 : テレポート + レーザー乱射
     */
    private void tickUltimate(ServerLevel level) {
        LivingEntity target = this.getTarget();
        if (target == null || !target.isAlive()) {
            return;
        }

        int elapsed = EndWardenAttackPhase.ULTIMATE.getBaseDurationTicks() - getPhaseTick();

        // ---- メテオフォール ----
        if (!this.ultimateMeteorFired && elapsed >= 20 && elapsed <= 25) {
            this.ultimateMeteorFired = true;
            fireMeteors(level, target);
        }

        // ---- テレポート + レーザー乱射 ----
        if (elapsed >= 40) {
            this.ultimateTeleportCounter++;

            if (this.ultimateTeleportCounter % TELEPORT_INTERVAL == 0) {
                teleportAroundTarget(level, target);
            }

            // テレポート後、レーザーを発射
            if (this.ultimateTeleportCounter % TELEPORT_INTERVAL == 0) {
                fireLaserAt(level, target);
            }
        }

        // 常時、ターゲット方向を見る
        this.getLookControl().setLookAt(target, 60.0F, 60.0F);
    }

    // =========================================================
    // メテオフォール
    // =========================================================

    /**
     * 上空から8本のソニックブーム柱を落とす。
     */
    private void fireMeteors(ServerLevel level, LivingEntity target) {
        Vec3 targetPos = target.position();

        for (int i = 0; i < METEOR_COUNT; i++) {
            // ターゲット周囲に散らす
            double angle = (i / (double) METEOR_COUNT) * Math.PI * 2.0;
            double radius = 2.0 + this.random.nextDouble() * 4.0;
            double dx = Math.cos(angle) * radius;
            double dz = Math.sin(angle) * radius;

            final double x = targetPos.x + dx;
            final double z = targetPos.z + dz;
            final double startY = targetPos.y + METEOR_HEIGHT;

            // 落下エフェクト
            level.sendParticles(
                    ParticleTypes.SONIC_BOOM,
                    x, startY, z,
                    5,
                    0.5, 0.5, 0.5,
                    0.0
            );

            // 1tick後に着弾（即時落下）
            level.getServer().execute(() -> {
                // 柱状にパーティクルを描画
                for (double y = startY; y >= targetPos.y - 1; y -= 0.5) {
                    level.sendParticles(
                            ParticleTypes.SONIC_BOOM,
                            x, y, z,
                            2,
                            0.3, 0.3, 0.3,
                            0.0
                    );
                    level.sendParticles(
                            ParticleTypes.SCULK_SOUL,
                            x, y, z,
                            1,
                            0.3, 0.3, 0.3,
                            0.0
                    );
                }

                // 着弾地点のダメージ判定
                Vec3 impactPos = new Vec3(x, targetPos.y, z);
                AABB box = new AABB(impactPos, impactPos).inflate(METEOR_RADIUS);

                List<LivingEntity> hits = level.getEntitiesOfClass(
                        LivingEntity.class,
                        box,
                        e -> e.isAlive() && e != this && !e.isSpectator()
                );

                for (LivingEntity hit : hits) {
                    hit.invulnerableTime = 0;
                    hit.hurt(RinneDamageSource.of(level, this), METEOR_DAMAGE);
                    hit.addEffect(new MobEffectInstance(
                            MobEffects.DARKNESS, 120, 2, false, false
                    ));
                }

                // 着弾音
                level.playSound(
                        null, x, targetPos.y, z,
                        SoundEvents.WARDEN_SONIC_BOOM,
                        SoundSource.HOSTILE,
                        2.0F, 1.2F
                );
            });
        }

        level.playSound(
                null,
                this.getX(), this.getY(), this.getZ(),
                SoundEvents.ENDER_DRAGON_FLAP,
                SoundSource.HOSTILE,
                3.0F,
                0.7F
        );
    }

    // =========================================================
    // テレポート
    // =========================================================

    /**
     * ターゲットの周囲にテレポートする。
     */
    private void teleportAroundTarget(ServerLevel level, LivingEntity target) {
        double angle = this.random.nextDouble() * Math.PI * 2.0;
        double radius = 6.0 + this.random.nextDouble() * 6.0;

        double dx = target.getX() + Math.cos(angle) * radius;
        double dy = target.getY() + 2.0 + this.random.nextDouble() * 3.0;
        double dz = target.getZ() + Math.sin(angle) * radius;

        // テレポート前演出
        level.sendParticles(
                ParticleTypes.PORTAL,
                this.getX(), this.getY() + 1.0, this.getZ(),
                30,
                0.5, 1.0, 0.5,
                0.5
        );

        // テレポート
        this.teleportTo(dx, dy, dz);

        // テレポート後演出
        level.sendParticles(
                ParticleTypes.PORTAL,
                dx, dy + 1.0, dz,
                30,
                0.5, 1.0, 0.5,
                0.5
        );
        level.sendParticles(
                ParticleTypes.SOUL_FIRE_FLAME,
                dx, dy + 1.0, dz,
                15,
                0.5, 0.5, 0.5,
                0.1
        );

        level.playSound(
                null,
                dx, dy, dz,
                SoundEvents.ENDERMAN_TELEPORT,
                SoundSource.HOSTILE,
                2.0F,
                0.7F
        );
    }

    // =========================================================
    // レーザー発射
    // =========================================================

    /**
     * ターゲット方向へ即時レーザーを発射する。
     */
    private void fireLaserAt(ServerLevel level, LivingEntity target) {
        Vec3 start = this.getEyePosition();
        Vec3 targetPos = target.getEyePosition();
        Vec3 dir = targetPos.subtract(start).normalize();

        // 視線を合わせる
        double dx = target.getX() - this.getX();
        double dy = target.getEyeY() - this.getEyeY();
        double dz = target.getZ() - this.getZ();
        double horizontal = Math.sqrt(dx * dx + dz * dz);
        float yaw = (float) (Math.atan2(dz, dx) * (180.0F / Math.PI)) - 90.0F;
        float pitch = (float) -(Math.atan2(dy, horizontal) * (180.0F / Math.PI));
        this.setYRot(yaw);
        this.setYHeadRot(yaw);
        this.setXRot(pitch);

        // ビーム描画
        double beamLength = 30.0;
        for (double t = 0; t < beamLength; t += 0.5) {
            Vec3 p = start.add(dir.scale(t));
            level.sendParticles(
                    ParticleTypes.SONIC_BOOM,
                    p.x, p.y, p.z,
                    2,
                    0.1, 0.1, 0.1,
                    0.0
            );
            level.sendParticles(
                    ParticleTypes.SCULK_SOUL,
                    p.x, p.y, p.z,
                    1,
                    0.1, 0.1, 0.1,
                    0.0
            );

            // ビーム上の敵にダメージ
            AABB box = new AABB(p, p).inflate(1.2);
            List<LivingEntity> hits = level.getEntitiesOfClass(
                    LivingEntity.class,
                    box,
                    e -> e.isAlive() && e != this && !e.isSpectator()
            );
            for (LivingEntity hit : hits) {
                hit.invulnerableTime = 0;
                hit.hurt(RinneDamageSource.of(level, this), RINNE_HIT_DAMAGE);
                hit.addEffect(new MobEffectInstance(
                        MobEffects.DARKNESS, 120, 2, false, false
                ));
                hit.addEffect(new MobEffectInstance(
                        MobEffects.MOVEMENT_SLOWDOWN, 120, 2, false, false
                ));
            }
        }

        level.playSound(
                null,
                this.getX(), this.getY(), this.getZ(),
                SoundEvents.WARDEN_SONIC_BOOM,
                SoundSource.HOSTILE,
                2.0F,
                1.5F
        );
    }

    // =========================================================
    // ソニックブームに輪廻ダメージを追加（親の applyBeamDamage を拡張）
    // =========================================================

    @Override
    protected void applyBeamDamage(ServerLevel level, Vec3 point) {
        // まず親の処理（abyssal + sonic + dual）
        super.applyBeamDamage(level, point);

        // 追加で輪廻ダメージ
        AABB box = new AABB(
                point.x - 1.5, point.y - 1.5, point.z - 1.5,
                point.x + 1.5, point.y + 1.5, point.z + 1.5
        );

        List<LivingEntity> hits = level.getEntitiesOfClass(
                LivingEntity.class,
                box,
                e -> e.isAlive() && e != this && !e.isSpectator()
        );

        for (LivingEntity hit : hits) {
            hit.invulnerableTime = 0;
            hit.hurt(RinneDamageSource.of(level, this), RINNE_HIT_DAMAGE);
        }
    }

    // =========================================================
    // 死亡演出（1回目で怒りモード移行）
    // =========================================================

    @Override
    public void die(DamageSource source) {

        // 1回目：HP2048で復活 + 怒りモード
        if (getDeathStage() == 0) {
            setDeathStage(1);
            this.setHealth(2048.0F);

            // ★ 怒りモード移行
            setAngerMode(true);

            if (this.level() instanceof ServerLevel sl) {
                EndWardenParticleHelper.spawnDeathBurst(sl, this.position());

                sl.playSound(
                        null,
                        this.getX(), this.getY(), this.getZ(),
                        SoundEvents.WARDEN_ROAR,
                        SoundSource.HOSTILE,
                        3.0F,
                        0.5F
                );

                sl.sendParticles(
                        ParticleTypes.SOUL_FIRE_FLAME,
                        this.getX(), this.getY() + 1.0, this.getZ(),
                        150,
                        2.0, 3.0, 2.0,
                        0.2
                );

                // 近くのプレイヤーにメッセージ
                sl.getPlayers(p -> p.distanceToSqr(this) < 128 * 128)
                        .forEach(p -> p.displayClientMessage(
                                net.minecraft.network.chat.Component.literal("§4§lエンドウォーデン・リバースが §c真の姿 §4を現した…")
                                        .withStyle(net.minecraft.ChatFormatting.DARK_RED),
                                true
                        ));
            }

            // 親の die は呼ばない（setDeathStage で制御）
            return;
        }

        // 2回目：本当に死ぬ
        super.die(source);

        // 追加ドロップ
        if (this.level() instanceof ServerLevel sl) {
            this.spawnAtLocation(com.tyami.forlaism.registry.Items.MADOROMU.get(), 3);
            this.spawnAtLocation(net.minecraft.world.item.Items.NETHERITE_INGOT, 8);
        }
    }

    // =========================================================
    // NBT
    // =========================================================

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putBoolean("AngerMode", isAngerMode());
        tag.putInt("UltimateCooldown", this.ultimateCooldown);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        setAngerMode(tag.getBoolean("AngerMode"));
        this.ultimateCooldown = tag.getInt("UltimateCooldown");
    }
}