package com.tyami.forlaism.entity;

import com.tyami.forlaism.registry.ModEntityTypes;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
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
import net.minecraft.world.entity.monster.WitherSkeleton;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;
import java.util.List;

/**
 * ウィザースケルトンロード。
 *
 * 中ボス。
 *
 * - ウィザースケルトンスポーン時に 1% で置換出現
 * - フルネザライトエンチャ装備 + ネザライト剣二刀流
 * - HP250 / 攻撃力3倍 / 移動速度1.6倍
 * - 15マス以上離れていると切りながら接近（突進）
 * - プレイヤーが盾持ち かつ Yが低い と上に飛ぶ
 * - 32マス以上離れるとプレイヤーの背後にTP（壁なら同位置）
 * - HP75%でウィザスケ×10召喚
 * - HP50%で周囲に火炎 + 自身も炎 + 攻撃力2倍
 * - HP25%で周囲を切りまくり & ブロック破壊
 * - 倒すとウィザースケルトンの頭×64
 * - 儀式の短剣で確定召喚
 */
public class WitherSkeletonLordEntity extends WitherSkeleton {

    // =========================================================
    // 定数
    // =========================================================

    /** 突進接近を開始する距離。 */
    private static final double CHARGE_DISTANCE = 15.0D;

    /** 突進の速度ブースト。 */
    private static final double CHARGE_SPEED = 1.4D;

    /** 盾持ちプレイヤーの上へ飛ぶ時の上昇速度。 */
    private static final double SHIELD_JUMP_UP = 0.9D;

    /** 盾持ちプレイヤー判定を行うY差。 */
    private static final double SHIELD_Y_DIFF = 2.0D;

    /** TPを開始する距離。 */
    private static final double TELEPORT_DISTANCE = 32.0D;

    /** フェーズ移行のHP閾値。 */
    private static final float PHASE_75 = 0.75F;
    private static final float PHASE_50 = 0.50F;
    private static final float PHASE_25 = 0.25F;

    /** HP75%時に召喚するウィザスケ数。 */
    private static final int SUMMON_COUNT_75 = 10;

    /** HP50%時の火炎付与 tick。 */
    private static final int FIRE_TICKS = 200;

    /** HP25%時の周囲切り範囲。 */
    private static final double PHASE25_SWEEP_RADIUS = 4.5D;

    /** HP25%時の周囲切りダメージ。 */
    private static final float PHASE25_SWEEP_DAMAGE = 8.0F;

    /** HP25%時のブロック破壊範囲。 */
    private static final int PHASE25_BREAK_RADIUS = 2;

    /** TPクールダウン (tick)。 */
    private static final int TELEPORT_COOLDOWN = 60;

    // =========================================================
    // 状態
    // =========================================================

    /** フェーズ75 発動済み。 */
    private boolean phase75Done = false;

    /** フェーズ50 発動済み。 */
    private boolean phase50Done = false;

    /** フェーズ25 発動済み。 */
    private boolean phase25Done = false;

    /** TPクールダウン残り。 */
    private int teleportCooldown = 0;

    /** 突進tick残り。 */
    private int chargeTicks = 0;

    public WitherSkeletonLordEntity(EntityType<? extends WitherSkeleton> type, Level level) {
        super(type, level);
        this.xpReward = 200;
        this.setPersistenceRequired();
    }

    // =========================================================
    // 属性
    // =========================================================

    public static AttributeSupplier.Builder createAttributes() {
        // バニラWitherSkeleton: HP20, 攻撃力4, 移動速度0.25
        // → HP250 / 攻撃力3倍(12) / 移動速度1.6倍(0.4)
        return WitherSkeleton.createAttributes()
                .add(Attributes.MAX_HEALTH, 250.0D)
                .add(Attributes.ATTACK_DAMAGE, 12.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.4D)
                .add(Attributes.ARMOR, 20.0D)
                .add(Attributes.ARMOR_TOUGHNESS, 12.0D)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.7D)
                .add(Attributes.FOLLOW_RANGE, 96.0D);
    }

    // =========================================================
    // AI
    // =========================================================

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(1, new WitherSkeletonLordAttackGoal(this));
        this.goalSelector.addGoal(7, new LookAtPlayerGoal(this, Player.class, 64.0F));

        this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
    }

    @Override
    public boolean isBaby() {
        return false;
    }

    // =========================================================
    // 装備
    // =========================================================

    @Override
    protected void populateDefaultEquipmentSlots(net.minecraft.util.RandomSource random, DifficultyInstance difficulty) {
        super.populateDefaultEquipmentSlots(random, difficulty);

        // メインハンド: ネザライト剣
        ItemStack mainSword = new ItemStack(Items.NETHERITE_SWORD);
        fullEnchantWeapon(mainSword);
        this.setItemSlot(EquipmentSlot.MAINHAND, mainSword);

        // オフハンド: ネザライト剣（二刀流）
        ItemStack offSword = new ItemStack(Items.NETHERITE_SWORD);
        fullEnchantWeapon(offSword);
        this.setItemSlot(EquipmentSlot.OFFHAND, offSword);

        // フルネザライト防具
        equipArmor(EquipmentSlot.HEAD, new ItemStack(Items.NETHERITE_HELMET));
        equipArmor(EquipmentSlot.CHEST, new ItemStack(Items.NETHERITE_CHESTPLATE));
        equipArmor(EquipmentSlot.LEGS, new ItemStack(Items.NETHERITE_LEGGINGS));
        equipArmor(EquipmentSlot.FEET, new ItemStack(Items.NETHERITE_BOOTS));

        // ドロップ無効化（剣・防具は落とさない）
        for (EquipmentSlot slot : EquipmentSlot.values()) {
            this.setDropChance(slot, 0.0F);
        }
    }

    private void equipArmor(EquipmentSlot slot, ItemStack stack) {
        fullEnchantArmor(stack);
        this.setItemSlot(slot, stack);
    }

    // =========================================================
    // フルエンチャント
    // =========================================================

    private static void fullEnchantWeapon(ItemStack stack) {
        stack.enchant(Enchantments.SHARPNESS, 10);
        stack.enchant(Enchantments.SMITE, 10);
        stack.enchant(Enchantments.BANE_OF_ARTHROPODS, 10);
        stack.enchant(Enchantments.KNOCKBACK, 10);
        stack.enchant(Enchantments.FIRE_ASPECT, 10);
        stack.enchant(Enchantments.MOB_LOOTING, 10);
        stack.enchant(Enchantments.SWEEPING_EDGE, 10);
        stack.enchant(Enchantments.UNBREAKING, 10);
        stack.enchant(Enchantments.MENDING, 1);
        stack.enchant(Enchantments.VANISHING_CURSE, 1);
    }

    private static void fullEnchantArmor(ItemStack stack) {
        stack.enchant(Enchantments.ALL_DAMAGE_PROTECTION, 10);
        stack.enchant(Enchantments.FIRE_PROTECTION, 10);
        stack.enchant(Enchantments.BLAST_PROTECTION, 10);
        stack.enchant(Enchantments.PROJECTILE_PROTECTION, 10);
        stack.enchant(Enchantments.FALL_PROTECTION, 10);
        stack.enchant(Enchantments.THORNS, 10);
        stack.enchant(Enchantments.RESPIRATION, 10);
        stack.enchant(Enchantments.AQUA_AFFINITY, 10);
        stack.enchant(Enchantments.DEPTH_STRIDER, 10);
        stack.enchant(Enchantments.FROST_WALKER, 10);
        stack.enchant(Enchantments.BINDING_CURSE, 1);
        stack.enchant(Enchantments.UNBREAKING, 10);
        stack.enchant(Enchantments.MENDING, 1);
        stack.enchant(Enchantments.VANISHING_CURSE, 1);
    }

    // =========================================================
    // スポーン
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
        this.populateDefaultEquipmentSlots(this.random, difficulty);
        this.setCanPickUpLoot(false);
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

        // 常時炎上演出
        if (this.isOnFire()) {
            // 何もしない (バニラに任せる)
        }

        LivingEntity target = this.getTarget();
        if (target == null) return;

        double distSqr = this.distanceToSqr(target);

        // ---- フェーズ判定 ----
        float hpRatio = this.getHealth() / this.getMaxHealth();

        // HP75%: ウィザスケ召喚
        if (!phase75Done && hpRatio <= PHASE_75) {
            phase75Done = true;
            summonWitherSkeletons();
        }

        // HP50%: 火炎ばら撒き + 自身炎上 + 攻撃力2倍
        if (!phase50Done && hpRatio <= PHASE_50) {
            phase50Done = true;
            phase50Activate();
        }

        // HP25%: 周囲切りまくり
        if (!phase25Done && hpRatio <= PHASE_25) {
            phase25Done = true;
            this.addEffect(new net.minecraft.world.effect.MobEffectInstance(
                    net.minecraft.world.effect.MobEffects.MOVEMENT_SPEED,
                    Integer.MAX_VALUE, 2, false, false, false
            ));
        }

        // ---- HP25% フェーズ: 周囲を切りまくり ----
        if (phase25Done) {
            tickPhase25Sweep();
        }

        // ---- 32マス以上 → 背後TP ----
        if (distSqr >= TELEPORT_DISTANCE * TELEPORT_DISTANCE && teleportCooldown <= 0) {
            teleportBehindTarget(target);
            teleportCooldown = TELEPORT_COOLDOWN;
            return;
        }

        // ---- 盾持ちプレイヤー判定 ----
        if (target instanceof Player player && player.isBlocking()) {
            if (this.getY() < player.getY() - SHIELD_Y_DIFF) {
                // 上に飛ぶ
                this.setDeltaMovement(
                        this.getDeltaMovement().x,
                        SHIELD_JUMP_UP,
                        this.getDeltaMovement().z
                );
                this.hurtMarked = true;
            }
        }

        // ---- 15マス以上 → 突進接近 ----
        if (distSqr >= CHARGE_DISTANCE * CHARGE_DISTANCE && chargeTicks <= 0) {
            startCharge(target);
        }

        // ---- 突進中 ----
        if (chargeTicks > 0) {
            tickCharge(target);
        }
    }

    // =========================================================
    // 突進接近
    // =========================================================

    private void startCharge(LivingEntity target) {
        this.chargeTicks = 40;

        Vec3 dir = target.position().subtract(this.position()).normalize();
        this.setDeltaMovement(
                dir.x * CHARGE_SPEED,
                0.2D,
                dir.z * CHARGE_SPEED
        );
        this.hurtMarked = true;

        // 斬撃パーティクル
        if (this.level() instanceof ServerLevel sl) {
            sl.sendParticles(
                    ParticleTypes.SWEEP_ATTACK,
                    this.getX(), this.getY() + 1.0, this.getZ(),
                    15, 1.0, 0.5, 1.0, 0.05
            );
            sl.playSound(
                    null, this.getX(), this.getY(), this.getZ(),
                    SoundEvents.PLAYER_ATTACK_SWEEP,
                    SoundSource.HOSTILE,
                    1.2F, 0.7F
            );
        }
    }

    private void tickCharge(LivingEntity target) {
        chargeTicks--;

        Vec3 dir = target.position().subtract(this.position()).normalize();
        Vec3 v = this.getDeltaMovement();
        this.setDeltaMovement(
                v.x * 0.85D + dir.x * 0.3D,
                v.y,
                v.z * 0.85D + dir.z * 0.3D
        );
        this.hurtMarked = true;

        // 突進中の斬撃パーティクル
        if (this.level() instanceof ServerLevel sl && this.tickCount % 2 == 0) {
            sl.sendParticles(
                    ParticleTypes.SWEEP_ATTACK,
                    this.getX(), this.getY() + 1.0, this.getZ(),
                    3, 0.8, 0.5, 0.8, 0.02
            );
        }
    }

    // =========================================================
    // 背後TP
    // =========================================================

    private void teleportBehindTarget(LivingEntity target) {
        if (!(this.level() instanceof ServerLevel sl)) return;

        // プレイヤーの視線方向を取得
        Vec3 look = target.getLookAngle().normalize();

        // 背後 = 視線の逆方向へ 2 マス
        Vec3 behindPos = target.position().subtract(look.scale(2.0));

        // ブロックチェック: その位置の足元と頭が空気かどうか
        BlockPos behindBlock = BlockPos.containing(behindPos);

        boolean canPlace = sl.getBlockState(behindBlock).isAir()
                && sl.getBlockState(behindBlock.above()).isAir();

        // 壁なら同位置、空きなら背後
        Vec3 teleportPos = canPlace ? behindPos : target.position();

        // 演出
        sl.sendParticles(
                ParticleTypes.PORTAL,
                this.getX(), this.getY() + 1.0, this.getZ(),
                30, 0.5, 0.5, 0.5, 0.2
        );

        this.teleportTo(
                teleportPos.x,
                teleportPos.y,
                teleportPos.z
        );

        sl.sendParticles(
                ParticleTypes.PORTAL,
                teleportPos.x, teleportPos.y + 1.0, teleportPos.z,
                30, 0.5, 0.5, 0.5, 0.2
        );

        sl.playSound(
                null,
                teleportPos.x, teleportPos.y, teleportPos.z,
                SoundEvents.ENDERMAN_TELEPORT,
                SoundSource.HOSTILE,
                1.0F, 1.4F
        );

        // 視線をプレイヤーへ
        this.lookAt(target, 360.0F, 360.0F);
    }

    // =========================================================
    // フェーズ75: ウィザスケ召喚
    // =========================================================

    private void summonWitherSkeletons() {
        if (!(this.level() instanceof ServerLevel sl)) return;

        for (int i = 0; i < SUMMON_COUNT_75; i++) {
            WitherSkeleton skeleton = EntityType.WITHER_SKELETON.create(sl);
            if (skeleton == null) continue;

            double angle = (i / (double) SUMMON_COUNT_75) * Math.PI * 2.0;
            double radius = 3.0;

            double sx = this.getX() + Math.cos(angle) * radius;
            double sy = this.getY();
            double sz = this.getZ() + Math.sin(angle) * radius;

            skeleton.moveTo(sx, sy, sz, this.random.nextFloat() * 360.0F, 0.0F);

            // 装備
            ItemStack sword = new ItemStack(Items.NETHERITE_SWORD);
            sword.enchant(Enchantments.SHARPNESS, 10);
            skeleton.setItemSlot(EquipmentSlot.MAINHAND, sword);

            skeleton.setTarget(this.getTarget());

            sl.addFreshEntity(skeleton);

            sl.sendParticles(
                    ParticleTypes.SOUL_FIRE_FLAME,
                    sx, sy + 1.0, sz,
                    20, 0.3, 0.5, 0.3, 0.05
            );
        }

        sl.playSound(
                null,
                this.getX(), this.getY(), this.getZ(),
                SoundEvents.EVOKER_CAST_SPELL,
                SoundSource.HOSTILE,
                2.0F, 0.6F
        );

        // 通知
        if (this.getTarget() instanceof Player player) {
            player.displayClientMessage(
                    Component.literal("§4§lウィザースケルトンロードが配下を召喚した！"),
                    true
            );
        }
    }

    // =========================================================
    // フェーズ50: 火炎ばら撒き + 自身炎上 + 攻撃力2倍
    // =========================================================

    private void phase50Activate() {
        if (!(this.level() instanceof ServerLevel sl)) return;

        // 自身を炎上
        this.setSecondsOnFire(FIRE_TICKS / 20);

        // 周囲に火を撒く
        for (int i = 0; i < 24; i++) {
            double angle = this.random.nextDouble() * Math.PI * 2.0;
            double radius = 2.0 + this.random.nextDouble() * 4.0;

            double fx = this.getX() + Math.cos(angle) * radius;
            double fz = this.getZ() + Math.sin(angle) * radius;

            BlockPos groundPos = BlockPos.containing(fx, this.getY(), fz);

            // 地面を探す
            for (int dy = 2; dy >= -3; dy--) {
                BlockPos candidate = groundPos.offset(0, dy, 0);
                if (!sl.getBlockState(candidate).isAir()
                        && sl.getBlockState(candidate.above()).isAir()) {

                    sl.setBlock(
                            candidate.above(),
                            Blocks.FIRE.defaultBlockState(),
                            3
                    );
                    break;
                }
            }
        }

        // 攻撃力2倍
        var attackAttr = this.getAttribute(Attributes.ATTACK_DAMAGE);
        if (attackAttr != null) {
            attackAttr.setBaseValue(attackAttr.getBaseValue() * 2.0D);
        }

        sl.playSound(
                null,
                this.getX(), this.getY(), this.getZ(),
                SoundEvents.BLAZE_SHOOT,
                SoundSource.HOSTILE,
                2.0F, 0.5F
        );

        if (this.getTarget() instanceof Player player) {
            player.displayClientMessage(
                    Component.literal("§c§lウィザースケルトンロードが炎を纏った！攻撃力上昇！"),
                    true
            );
        }
    }

    // =========================================================
    // フェーズ25: 周囲切りまくり + ブロック破壊
    // =========================================================

    private void tickPhase25Sweep() {
        if (!(this.level() instanceof ServerLevel sl)) return;

        // 2tickに1回スイープ
        if (this.tickCount % 2 != 0) return;

        // 周囲のLivingEntityにダメージ
        List<LivingEntity> targets = sl.getEntitiesOfClass(
                LivingEntity.class,
                this.getBoundingBox().inflate(PHASE25_SWEEP_RADIUS),
                e -> e.isAlive() && e != this && !e.isSpectator()
        );

        for (LivingEntity t : targets) {
            t.invulnerableTime = 0;
            t.hurtTime = 0;
            t.hurt(this.damageSources().mobAttack(this), PHASE25_SWEEP_DAMAGE);

            // ノックバック
            Vec3 away = t.position().subtract(this.position()).normalize();
            t.push(away.x * 1.5D, 0.4D, away.z * 1.5D);
        }

        // ブロック破壊
        breakNearbyBlocks(sl);

        // 斬撃パーティクル
        sl.sendParticles(
                ParticleTypes.SWEEP_ATTACK,
                this.getX(), this.getY() + 1.0, this.getZ(),
                10, PHASE25_SWEEP_RADIUS * 0.6, 0.6, PHASE25_SWEEP_RADIUS * 0.6, 0.05
        );

        if (this.tickCount % 10 == 0) {
            sl.playSound(
                    null,
                    this.getX(), this.getY(), this.getZ(),
                    SoundEvents.PLAYER_ATTACK_SWEEP,
                    SoundSource.HOSTILE,
                    1.5F,
                    0.6F + this.random.nextFloat() * 0.3F
            );
        }
    }

    private void breakNearbyBlocks(ServerLevel sl) {
        BlockPos center = this.blockPosition();

        for (int dx = -PHASE25_BREAK_RADIUS; dx <= PHASE25_BREAK_RADIUS; dx++) {
            for (int dy = -1; dy <= PHASE25_BREAK_RADIUS; dy++) {
                for (int dz = -PHASE25_BREAK_RADIUS; dz <= PHASE25_BREAK_RADIUS; dz++) {

                    BlockPos pos = center.offset(dx, dy, dz);
                    BlockState state = sl.getBlockState(pos);

                    if (state.isAir()) continue;
                    if (state.getDestroySpeed(sl, pos) < 0) continue; // 岩盤除外

                    // チェスト等のコンテナも壊す（中身は消える）
                    sl.destroyBlock(pos, true, this);
                }
            }
        }
    }

    // =========================================================
    // クライアント演出
    // =========================================================

    private void clientParticles() {
        // 常時紫オーラ
        if (this.tickCount % 3 == 0) {
            this.level().addParticle(
                    ParticleTypes.SOUL_FIRE_FLAME,
                    this.getX() + (this.random.nextDouble() - 0.5) * 1.0,
                    this.getY() + this.random.nextDouble() * 2.0,
                    this.getZ() + (this.random.nextDouble() - 0.5) * 1.0,
                    0, 0.02, 0
            );
        }
    }

    // =========================================================
    // 死亡ドロップ
    // =========================================================

    @Override
    public void die(DamageSource source) {
        if (!this.level().isClientSide && this.level() instanceof ServerLevel sl) {

            // ウィザースケルトンの頭 ×64
            ItemStack skull = new ItemStack(Items.WITHER_SKELETON_SKULL, 64);
            this.spawnAtLocation(skull);

            // 死亡演出
            sl.sendParticles(
                    ParticleTypes.EXPLOSION_EMITTER,
                    this.getX(), this.getY() + 1.0, this.getZ(),
                    1, 0, 0, 0, 0
            );
            sl.sendParticles(
                    ParticleTypes.SOUL_FIRE_FLAME,
                    this.getX(), this.getY() + 1.0, this.getZ(),
                    60, 1.5, 1.0, 1.5, 0.1
            );
            sl.playSound(
                    null,
                    this.getX(), this.getY(), this.getZ(),
                    SoundEvents.WITHER_DEATH,
                    SoundSource.HOSTILE,
                    2.0F, 0.8F
            );
        }

        super.die(source);
    }

    // =========================================================
    // NBT
    // =========================================================

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putBoolean("Phase75Done", phase75Done);
        tag.putBoolean("Phase50Done", phase50Done);
        tag.putBoolean("Phase25Done", phase25Done);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        phase75Done = tag.getBoolean("Phase75Done");
        phase50Done = tag.getBoolean("Phase50Done");
        phase25Done = tag.getBoolean("Phase25Done");
    }

    // =========================================================
    // 内部 Goal
    // =========================================================

    private static class WitherSkeletonLordAttackGoal extends MeleeAttackGoal {
        private final WitherSkeletonLordEntity lord;

        public WitherSkeletonLordAttackGoal(WitherSkeletonLordEntity lord) {
            super(lord, 1.0D, true);
            this.lord = lord;
        }

        @Override
        public boolean canUse() {
            // 突進中は通常攻撃を止める
            if (lord.chargeTicks > 0) return false;
            return super.canUse();
        }
    }
}