package com.tyami.forlaism.entity;

import com.tyami.forlaism.registry.ModEntityTypes;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
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
 * 軽量化:
 *   - フルエンチャント装備は static テンプレート化
 *   - populateDefaultEquipmentSlots は 1 回だけ
 *   - シルクボウのドロップもテンプレートから .copy() で渡す
 */
public class SkeletonLordEntity extends Skeleton {

    private static final double SHIELD_ORBIT_RADIUS = 5.0D;
    private static final double SHIELD_ORBIT_SPEED = 0.25D;

    private static final int SHOTGUN_INTERVAL = 3;
    private static final int SHOTGUN_PELLETS = 3;

    private static final int BACKSTEP_ARROWS = 3;
    private static final int BACKSTEP_INTERVAL = 4;
    private static final int BACKSTEP_DURATION = 20;

    private static final int ARROW_RAIN_INTERVAL = 5;
    private static final int ARROW_RAIN_DURATION = 60;

    private static final int MACHINEGUN_INTERVAL = 2;

    private static final String TAG_EQUIP_DONE = "ForlaismSkeletonLordEquipped";

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

    private double orbitAngle = 0.0D;

    // =========================================================
    // 装備テンプレート（static 1回だけ生成）
    // =========================================================

    private static final ItemStack TEMPLATE_BOW = createEnchantedBow();
    private static final ItemStack TEMPLATE_SHIELD = new ItemStack(net.minecraft.world.item.Items.SHIELD);
    private static final ItemStack TEMPLATE_HELMET = createEnchantedArmor(
            new ItemStack(com.tyami.forlaism.registry.Items.GARBAGE_METAL_HELMET.get()));
    private static final ItemStack TEMPLATE_CHEST = createEnchantedArmor(
            new ItemStack(com.tyami.forlaism.registry.Items.GARBAGE_METAL_CHESTPLATE.get()));
    private static final ItemStack TEMPLATE_LEGS = createEnchantedArmor(
            new ItemStack(com.tyami.forlaism.registry.Items.GARBAGE_METAL_LEGGINGS.get()));
    private static final ItemStack TEMPLATE_BOOTS = createEnchantedArmor(
            new ItemStack(com.tyami.forlaism.registry.Items.GARBAGE_METAL_BOOTS.get()));

    /** シルクボウのドロップテンプレート。 */
    private static final ItemStack TEMPLATE_SILK_BOW = createEnchantedSilkBow();

    private static ItemStack createEnchantedBow() {
        ItemStack bow = new ItemStack(net.minecraft.world.item.Items.BOW);
        fullEnchant(bow);
        return bow;
    }

    private static ItemStack createEnchantedArmor(ItemStack stack) {
        fullEnchant(stack);
        return stack;
    }

    private static ItemStack createEnchantedSilkBow() {
        ItemStack bow = new ItemStack(com.tyami.forlaism.registry.Items.SILK_BOW.get());
        CompoundTag nbt = bow.getOrCreateTag();
        ListTag list = new ListTag();
        addEnchant(list, Enchantments.INFINITY_ARROWS, 10);
        addEnchant(list, Enchantments.POWER_ARROWS, 10);
        addEnchant(list, Enchantments.PUNCH_ARROWS, 10);
        addEnchant(list, Enchantments.FLAMING_ARROWS, 10);
        addEnchant(list, Enchantments.UNBREAKING, 10);
        addEnchant(list, Enchantments.MENDING, 1);
        nbt.put("Enchantments", list);
        return bow;
    }

    public SkeletonLordEntity(EntityType<? extends Skeleton> type, Level level) {
        super(type, level);
        this.xpReward = 100;
        this.setPersistenceRequired();
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Skeleton.createAttributes()
                .add(Attributes.MAX_HEALTH, 100.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.425D)
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

        if (this.getPersistentData().getBoolean(TAG_EQUIP_DONE)) {
            return;
        }

        this.setItemSlot(EquipmentSlot.MAINHAND, TEMPLATE_BOW.copy());
        this.setItemSlot(EquipmentSlot.OFFHAND, TEMPLATE_SHIELD.copy());

        this.setItemSlot(EquipmentSlot.HEAD, TEMPLATE_HELMET.copy());
        this.setItemSlot(EquipmentSlot.CHEST, TEMPLATE_CHEST.copy());
        this.setItemSlot(EquipmentSlot.LEGS, TEMPLATE_LEGS.copy());
        this.setItemSlot(EquipmentSlot.FEET, TEMPLATE_BOOTS.copy());

        for (EquipmentSlot slot : EquipmentSlot.values()) {
            this.setDropChance(slot, 0.0F);
        }

        this.getPersistentData().putBoolean(TAG_EQUIP_DONE, true);
    }

    // =========================================================
    // エンチャント（テンプレート生成用・1回だけ呼ばれる）
    // =========================================================

    private static void fullEnchant(ItemStack stack) {
        CompoundTag nbt = stack.getOrCreateTag();
        ListTag list = new ListTag();

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

        addEnchant(list, Enchantments.POWER_ARROWS, 10);
        addEnchant(list, Enchantments.PUNCH_ARROWS, 10);
        addEnchant(list, Enchantments.FLAMING_ARROWS, 10);
        addEnchant(list, Enchantments.INFINITY_ARROWS, 1);

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
    // Tick
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

        if (mode == Mode.IDLE || modeTicks <= 0) {
            mode = chooseMode(target, distSqr);
            modeTicks = getModeDuration(mode);
            shotTimer = 0;
            onModeStart(target);
        }

        modeTicks--;

        switch (mode) {
            case ORBIT_SHOTGUN -> tickOrbitShotgun(target);
            case BACKSTEP_SHOT -> tickBackstepShot(target);
            case ARROW_RAIN -> tickArrowRain(target);
            case MACHINEGUN -> tickMachinegun(target);
            default -> {}
        }
    }

    private Mode chooseMode(LivingEntity target, double distSqr) {
        if (target instanceof Player player && player.isUsingItem()
                && player.getUseItem().is(net.minecraft.world.item.Items.BOW)) {
            return Mode.MACHINEGUN;
        }

        if (distSqr >= 15.0 * 15.0) {
            return Mode.ARROW_RAIN;
        }

        if (distSqr <= 5.0 * 5.0) {
            return Mode.BACKSTEP_SHOT;
        }

        if (target instanceof Player player && player.isBlocking()) {
            return Mode.ORBIT_SHOTGUN;
        }

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
            Vec3 toTarget = target.position().subtract(this.position());
            orbitAngle = Math.atan2(toTarget.z, toTarget.x);
        }
    }

    private void tickOrbitShotgun(LivingEntity target) {
        orbitAngle += SHIELD_ORBIT_SPEED;

        double cx = target.getX() + Math.cos(orbitAngle) * SHIELD_ORBIT_RADIUS;
        double cz = target.getZ() + Math.sin(orbitAngle) * SHIELD_ORBIT_RADIUS;
        double cy = target.getY();

        this.getNavigation().stop();
        this.moveTo(cx, cy, cz, this.getYRot(), this.getXRot());
        this.lookAt(target, 360.0F, 360.0F);

        shotTimer++;
        if (shotTimer >= SHOTGUN_INTERVAL) {
            shotTimer = 0;

            Vec3 eye = this.getEyePosition();
            Vec3 dirBase = target.getEyePosition().subtract(eye).normalize();

            for (int i = 0; i < SHOTGUN_PELLETS; i++) {
                double spreadY = (i - 1) * 0.12;
                double spreadX = (this.random.nextDouble() - 0.5) * 0.08;

                Vec3 dir = dirBase.add(spreadX, spreadY, spreadX).normalize();
                shootArrow(dir, 2.0F);
            }

            playShootSound();
        }
    }

    private void tickBackstepShot(LivingEntity target) {
        if (modeTicks == BACKSTEP_DURATION - 1) {
            Vec3 away = this.position().subtract(target.position()).normalize();
            this.setDeltaMovement(away.x * 0.9D, 0.4D, away.z * 0.9D);
            this.hurtMarked = true;
        }

        this.lookAt(target, 360.0F, 360.0F);

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

    private void tickArrowRain(LivingEntity target) {
        this.lookAt(target, 360.0F, 360.0F);

        shotTimer++;
        if (shotTimer >= ARROW_RAIN_INTERVAL) {
            shotTimer = 0;

            if (!(this.level() instanceof ServerLevel sl)) return;

            double tx = target.getX();
            double ty = target.getY() + 20.0D;
            double tz = target.getZ();

            Arrow arrow = new Arrow(sl, this);
            arrow.setPos(tx, ty, tz);
            arrow.setDeltaMovement(0.0D, -1.5D, 0.0D);
            arrow.setOwner(this);

            arrow.setBaseDamage(6.0D);
            arrow.setCritArrow(true);

            sl.addFreshEntity(arrow);
        }
    }

    private void tickMachinegun(LivingEntity target) {
        this.lookAt(target, 360.0F, 360.0F);

        shotTimer++;
        if (shotTimer >= MACHINEGUN_INTERVAL) {
            shotTimer = 0;

            Vec3 eye = this.getEyePosition();
            Vec3 dir = target.getEyePosition().subtract(eye).normalize();

            dir = dir.add(
                    (this.random.nextDouble() - 0.5) * 0.05,
                    (this.random.nextDouble() - 0.5) * 0.05,
                    (this.random.nextDouble() - 0.5) * 0.05
            ).normalize();

            shootArrow(dir, 2.5F);
        }
    }

    private void shootArrow(Vec3 direction, float speed) {
        if (!(this.level() instanceof ServerLevel sl)) return;

        Arrow arrow = new Arrow(sl, this);
        arrow.setPos(this.getX(), this.getEyeY() - 0.1D, this.getZ());
        arrow.setDeltaMovement(direction.scale(speed));
        arrow.setOwner(this);

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
            // テンプレートから .copy() で渡す（NBT 再構築ゼロ）
            this.spawnAtLocation(TEMPLATE_SILK_BOW.copy());

            sl.sendParticles(ParticleTypes.EXPLOSION_EMITTER,
                    this.getX(), this.getY() + 1, this.getZ(),
                    1, 0, 0, 0, 0);
        }
        super.die(source);
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