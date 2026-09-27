package com.tyami.forlaism.entity;

import com.tyami.forlaism.registry.ModEntityTypes;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
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
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Skeleton;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Arrow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;
import java.util.List;

/**
 * スケルトンロード。
 *
 * 中ボス。弓の使い手。
 *
 * - スケルトンスポーン時に1%で出現
 * - フルコミメタルフルエンチャ + 弓 + 盾
 * - HP100、速度1.7倍
 * - プレイヤーが盾構え → 円運動しながらショットガン
 * - 5マス以内 → バックステップ + 3発
 * - 15マス以上 → 上空からアローレイン
 * - プレイヤーが弓持ち → マシンガン連射
 * - 死亡時「シルクボウ」確定ドロップ
 */
public class SkeletonLordEntity extends Skeleton {

    // =========================================================
    // 定数
    // =========================================================

    private static final double SHIELD_ORBIT_RADIUS = 5.0D;
    private static final double SHIELD_ORBIT_SPEED = 0.25D;

    private static final int SHOTGUN_INTERVAL = 3;   // 3tickごと
    private static final int SHOTGUN_PELLETS = 3;    // 3本

    private static final int BACKSTEP_ARROWS = 3;
    private static final int BACKSTEP_INTERVAL = 4;
    private static final int BACKSTEP_DURATION = 20;

    private static final int ARROW_RAIN_INTERVAL = 5;
    private static final int ARROW_RAIN_DURATION = 60;

    private static final int MACHINEGUN_INTERVAL = 2;

    // =========================================================
    // 状態
    // =========================================================

    /** 攻撃モード。 */
    private enum Mode {
        IDLE,
        ORBIT_SHOTGUN,
        BACKSTEP_SHOT,
        ARROW_RAIN,
        MACHINEGUN
    }

    private Mode mode = Mode.IDLE;
    private int modeTicks = 0;
    private int shotTimer = 0;

    /** 円運動の角度。 */
    private double orbitAngle = 0.0D;

    public SkeletonLordEntity(EntityType<? extends Skeleton> type, Level level) {
        super(type, level);
        this.xpReward = 100;
        this.setPersistenceRequired();
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Skeleton.createAttributes()
                .add(Attributes.MAX_HEALTH, 100.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.425D) // 0.25 * 1.7
                .add(Attributes.ATTACK_DAMAGE, 4.0D)
                .add(Attributes.ARMOR, 20.0D)
                .add(Attributes.ARMOR_TOUGHNESS, 12.0D)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.4D)
                .add(Attributes.FOLLOW_RANGE, 64.0D);
    }

    // =========================================================
    // 装備
    // =========================================================

    @Override
    protected void populateDefaultEquipmentSlots(net.minecraft.util.RandomSource random, DifficultyInstance difficulty) {
        super.populateDefaultEquipmentSlots(random, difficulty);

        // メインハンド: 弓
        ItemStack bow = new ItemStack(net.minecraft.world.item.Items.BOW);
        fullEnchant(bow);
        this.setItemSlot(EquipmentSlot.MAINHAND, bow);

        // オフハンド: 盾
        this.setItemSlot(EquipmentSlot.OFFHAND, new ItemStack(net.minecraft.world.item.Items.SHIELD));

        // フルコミメタル防具
        equipArmor(EquipmentSlot.HEAD,
                new ItemStack(com.tyami.forlaism.registry.Items.GARBAGE_METAL_HELMET.get()));
        equipArmor(EquipmentSlot.CHEST,
                new ItemStack(com.tyami.forlaism.registry.Items.GARBAGE_METAL_CHESTPLATE.get()));
        equipArmor(EquipmentSlot.LEGS,
                new ItemStack(com.tyami.forlaism.registry.Items.GARBAGE_METAL_LEGGINGS.get()));
        equipArmor(EquipmentSlot.FEET,
                new ItemStack(com.tyami.forlaism.registry.Items.GARBAGE_METAL_BOOTS.get()));

        for (EquipmentSlot slot : EquipmentSlot.values()) {
            this.setDropChance(slot, 0.0F);
        }
    }

    private void equipArmor(EquipmentSlot slot, ItemStack stack) {
        fullEnchant(stack);
        this.setItemSlot(slot, stack);
    }

    // =========================================================
    // エンチャント（NBT直接書き込み）
    // =========================================================

    private static void fullEnchant(ItemStack stack) {
        CompoundTag nbt = stack.getOrCreateTag();
        ListTag list = new ListTag();

        // 防具エンチャ
        addEnchant(list, Enchantments.ALL_DAMAGE_PROTECTION, 10);
        addEnchant(list, Enchantments.FIRE_PROTECTION, 10);
        addEnchant(list, Enchantments.BLAST_PROTECTION, 10);
        addEnchant(list, Enchantments.PROJECTILE_PROTECTION, 10);
        addEnchant(list, Enchantments.FALL_PROTECTION, 10);
        addEnchant(list, Enchantments.THORNS, 10);
        addEnchant(list, Enchantments.RESPIRATION, 10);
        addEnchant(list, Enchantments.AQUA_AFFINITY, 10);
        addEnchant(list, Enchantments.DEPTH_STRIDER, 10);
        addEnchant(list, Enchantments.FROST_WALKER, 10);
        addEnchant(list, Enchantments.BINDING_CURSE, 1);

        // 弓エンチャ
        addEnchant(list, Enchantments.POWER_ARROWS, 10);
        addEnchant(list, Enchantments.PUNCH_ARROWS, 10);
        addEnchant(list, Enchantments.FLAMING_ARROWS, 10);
        addEnchant(list, Enchantments.INFINITY_ARROWS, 1);

        // 汎用
        addEnchant(list, Enchantments.UNBREAKING, 10);
        addEnchant(list, Enchantments.MENDING, 1);
        addEnchant(list, Enchantments.VANISHING_CURSE, 1);

        nbt.put("Enchantments", list);
    }

    private static void addEnchant(ListTag list, net.minecraft.world.item.enchantment.Enchantment ench, int level) {
        CompoundTag tag = new CompoundTag();
        tag.putString("id", net.minecraftforge.registries.ForgeRegistries.ENCHANTMENTS.getKey(ench).toString());
        tag.putShort("lvl", (short) level);
        list.add(tag);
    }

    // =========================================================
    // AI
    // =========================================================

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(7, new LookAtPlayerGoal(this, Player.class, 32.0F));

        this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
    }

    @Override
    public boolean isBaby() {
        return false;
    }



    // =========================================================
    // Tick（メインロジック）
    // =========================================================

    @Override
    public void tick() {
        super.tick();

        if (this.level().isClientSide) {
            clientParticles();
            return;
        }

        LivingEntity target = this.getTarget();
        if (target == null) {
            mode = Mode.IDLE;
            return;
        }

        double distSqr = this.distanceToSqr(target);

        // ---- モード選択 ----
        if (mode == Mode.IDLE || modeTicks <= 0) {
            mode = chooseMode(target, distSqr);
            modeTicks = getModeDuration(mode);
            shotTimer = 0;
            onModeStart(target);
        }

        modeTicks--;

        // ---- モード実行 ----
        switch (mode) {
            case ORBIT_SHOTGUN -> tickOrbitShotgun(target);
            case BACKSTEP_SHOT -> tickBackstepShot(target);
            case ARROW_RAIN -> tickArrowRain(target);
            case MACHINEGUN -> tickMachinegun(target);
            default -> {}
        }
    }

    private Mode chooseMode(LivingEntity target, double distSqr) {

        // プレイヤーが弓を持っている → マシンガン
        if (target instanceof Player player && player.isUsingItem()
                && player.getUseItem().is(net.minecraft.world.item.Items.BOW)) {
            return Mode.MACHINEGUN;
        }

        // 15マス以上 → アローレイン
        if (distSqr >= 15.0 * 15.0) {
            return Mode.ARROW_RAIN;
        }

        // 5マス以内 → バックステップ
        if (distSqr <= 5.0 * 5.0) {
            return Mode.BACKSTEP_SHOT;
        }

        // プレイヤーが盾を構えてる → 円運動ショットガン
        if (target instanceof Player player && player.isBlocking()) {
            return Mode.ORBIT_SHOTGUN;
        }

        // デフォルトはマシンガン
        return Mode.MACHINEGUN;
    }

    private int getModeDuration(Mode m) {
        return switch (m) {
            case ORBIT_SHOTGUN -> 60;
            case BACKSTEP_SHOT -> BACKSTEP_DURATION;
            case ARROW_RAIN -> ARROW_RAIN_DURATION;
            case MACHINEGUN -> 40;
            default -> 20;
        };
    }

    private void onModeStart(LivingEntity target) {
        if (mode == Mode.ORBIT_SHOTGUN) {
            // 円運動の初期角度
            Vec3 toTarget = target.position().subtract(this.position());
            orbitAngle = Math.atan2(toTarget.z, toTarget.x);
        }
    }

    // =========================================================
    // モード1: 円運動ショットガン
    // =========================================================

    private void tickOrbitShotgun(LivingEntity target) {
        // 円運動
        orbitAngle += SHIELD_ORBIT_SPEED;

        double cx = target.getX() + Math.cos(orbitAngle) * SHIELD_ORBIT_RADIUS;
        double cz = target.getZ() + Math.sin(orbitAngle) * SHIELD_ORBIT_RADIUS;
        double cy = target.getY();

        // テレポート移動（速度より確実）
        this.getNavigation().stop();
        this.moveTo(cx, cy, cz, this.getYRot(), this.getXRot());
        this.lookAt(target, 360.0F, 360.0F);

        // 3tickごとに3本発射
        shotTimer++;
        if (shotTimer >= SHOTGUN_INTERVAL) {
            shotTimer = 0;

            Vec3 eye = this.getEyePosition();
            Vec3 dirBase = target.getEyePosition().subtract(eye).normalize();

            for (int i = 0; i < SHOTGUN_PELLETS; i++) {
                // 散弾: 少しずつ角度をずらす
                double spreadY = (i - 1) * 0.12;
                double spreadX = (this.random.nextDouble() - 0.5) * 0.08;

                Vec3 dir = dirBase.add(spreadX, spreadY, spreadX).normalize();
                shootArrow(dir, 2.0F);
            }

            playShootSound();
        }
    }

    // =========================================================
    // モード2: バックステップしながら3発
    // =========================================================

    private void tickBackstepShot(LivingEntity target) {
        // バックステップ開始時のみノックバック付与
        if (modeTicks == BACKSTEP_DURATION - 1) {
            Vec3 away = this.position().subtract(target.position()).normalize();
            this.setDeltaMovement(away.x * 0.9D, 0.4D, away.z * 0.9D);
            this.hurtMarked = true;
        }

        // 相手の方を向く
        this.lookAt(target, 360.0F, 360.0F);

        // 4tickごとに3発
        shotTimer++;
        if (shotTimer >= BACKSTEP_INTERVAL) {
            shotTimer = 0;

            Vec3 eye = this.getEyePosition();
            Vec3 dir = target.getEyePosition().subtract(eye).normalize();

            for (int i = 0; i < BACKSTEP_ARROWS; i++) {
                double spreadY = (i - 1) * 0.08;
                Vec3 d = dir.add(0, spreadY, 0).normalize();
                shootArrow(d, 2.2F);
            }

            playShootSound();
        }
    }

    // =========================================================
    // モード3: アローレイン
    // =========================================================

    private void tickArrowRain(LivingEntity target) {
        this.lookAt(target, 360.0F, 360.0F);

        shotTimer++;
        if (shotTimer >= ARROW_RAIN_INTERVAL) {
            shotTimer = 0;

            // プレイヤーの現在位置の上空から矢を降らせる
            if (!(this.level() instanceof ServerLevel sl)) return;

            double tx = target.getX();
            double ty = target.getY() + 20.0D; // 上空20m
            double tz = target.getZ();

            // 上空に矢を召喚して下向きに飛ばす
            Arrow arrow = new Arrow(sl, this);
            arrow.setPos(tx, ty, tz);
            arrow.setDeltaMovement(0.0D, -1.5D, 0.0D);
            arrow.setOwner(this);

            // エンチャント反映（強力な矢）
            arrow.setBaseDamage(6.0D);
            arrow.setCritArrow(true);

            sl.addFreshEntity(arrow);
        }
    }

    // =========================================================
    // モード4: マシンガン
    // =========================================================

    private void tickMachinegun(LivingEntity target) {
        this.lookAt(target, 360.0F, 360.0F);

        shotTimer++;
        if (shotTimer >= MACHINEGUN_INTERVAL) {
            shotTimer = 0;

            Vec3 eye = this.getEyePosition();
            Vec3 dir = target.getEyePosition().subtract(eye).normalize();

            // わずかなブレ
            dir = dir.add(
                    (this.random.nextDouble() - 0.5) * 0.05,
                    (this.random.nextDouble() - 0.5) * 0.05,
                    (this.random.nextDouble() - 0.5) * 0.05
            ).normalize();

            shootArrow(dir, 2.5F);
        }
    }

    // =========================================================
    // 矢発射ヘルパー
    // =========================================================

    private void shootArrow(Vec3 direction, float speed) {
        if (!(this.level() instanceof ServerLevel sl)) return;

        Arrow arrow = new Arrow(sl, this);
        arrow.setPos(this.getX(), this.getEyeY() - 0.1D, this.getZ());
        arrow.setDeltaMovement(direction.scale(speed));
        arrow.setOwner(this);

        // エンチャント反映（Power 10 → ダメージ増）
        arrow.setBaseDamage(8.0D);
        arrow.setCritArrow(true);

        sl.addFreshEntity(arrow);
    }

    private void playShootSound() {
        if (this.level() instanceof ServerLevel sl) {
            sl.playSound(null, this.getX(), this.getY(), this.getZ(),
                    SoundEvents.SKELETON_SHOOT, SoundSource.HOSTILE, 1.0F,
                    1.0F + (this.random.nextFloat() - 0.5F) * 0.2F);
        }
    }

    // =========================================================
    // クライアントパーティクル
    // =========================================================

    private void clientParticles() {
        if (mode == Mode.ARROW_RAIN && this.tickCount % 3 == 0) {
            this.level().addParticle(
                    ParticleTypes.CRIT,
                    this.getX() + (this.random.nextDouble() - 0.5) * 2.0,
                    this.getY() + 3.0,
                    this.getZ() + (this.random.nextDouble() - 0.5) * 2.0,
                    0, 0, 0
            );
        }
    }

    // =========================================================
    // 死亡ドロップ
    // =========================================================

    @Override
    public void die(DamageSource source) {
        if (!this.level().isClientSide && this.level() instanceof ServerLevel sl) {
            ItemStack reward = new ItemStack(com.tyami.forlaism.registry.Items.SILK_BOW.get());
            fullEnchantSilkBow(reward);
            this.spawnAtLocation(reward);

            sl.sendParticles(ParticleTypes.EXPLOSION_EMITTER,
                    this.getX(), this.getY() + 1, this.getZ(),
                    1, 0, 0, 0, 0);
        }
        super.die(source);
    }

    private static void fullEnchantSilkBow(ItemStack stack) {
        CompoundTag nbt = stack.getOrCreateTag();
        ListTag list = new ListTag();

        addEnchant(list, Enchantments.INFINITY_ARROWS, 10);
        addEnchant(list, Enchantments.POWER_ARROWS, 10);
        addEnchant(list, Enchantments.PUNCH_ARROWS, 10);
        addEnchant(list, Enchantments.FLAMING_ARROWS, 10);
        addEnchant(list, Enchantments.UNBREAKING, 10);
        addEnchant(list, Enchantments.MENDING, 1);

        nbt.put("Enchantments", list);
    }

    // =========================================================
    // スポーン時
    // =========================================================

    @Nullable
    @Override
    public SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty,
                                        MobSpawnType reason, @Nullable SpawnGroupData spawnData,
                                        @Nullable CompoundTag dataTag) {
        SpawnGroupData result = super.finalizeSpawn(level, difficulty, reason, spawnData, dataTag);
        this.populateDefaultEquipmentSlots(this.random, difficulty);
        this.setCanPickUpLoot(false);
        return result;
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt("SkeletonLordMode", mode.ordinal());
        tag.putInt("SkeletonLordModeTicks", modeTicks);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        if (tag.contains("SkeletonLordMode")) {
            mode = Mode.values()[tag.getInt("SkeletonLordMode")];
        }
        modeTicks = tag.getInt("SkeletonLordModeTicks");
    }
}