package com.tyami.forlaism.entity;

import com.tyami.forlaism.registry.ModEntityTypes;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
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
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;

/**
 * ゾンビロード。
 *
 * 軽量化:
 *   - エンチャント済み装備は static テンプレート化（NBT 再構築ゼロ）
 *   - populateDefaultEquipmentSlots は 1 回だけ実行
 */
public class ZombieLordEntity extends Zombie {

    private static final String TAG_SUMMONED_MINIONS = "ForlaismZombieLordSummoned";
    private static final String TAG_EQUIP_DONE = "ForlaismZombieLordEquipped";

    private int chargeCooldown = 0;
    private boolean charging = false;
    private int chargeTicks = 0;
    private int backstepTicks = 0;
    private int comboStage = 0;
    private int comboTicks = 0;

    private static final EntityDataAccessor<Boolean> DATA_SHIELDING =
            SynchedEntityData.defineId(ZombieLordEntity.class, EntityDataSerializers.BOOLEAN);

    // =========================================================
    // 装備テンプレート（static 1回だけ生成）
    // =========================================================

    private static final ItemStack TEMPLATE_AXE = createEnchantedAxe();
    private static final ItemStack TEMPLATE_SWORD = createEnchantedSword();
    private static final ItemStack TEMPLATE_HELMET = createEnchantedArmor(new ItemStack(net.minecraft.world.item.Items.NETHERITE_HELMET));
    private static final ItemStack TEMPLATE_CHEST = createEnchantedArmor(new ItemStack(net.minecraft.world.item.Items.NETHERITE_CHESTPLATE));
    private static final ItemStack TEMPLATE_LEGS = createEnchantedArmor(new ItemStack(net.minecraft.world.item.Items.NETHERITE_LEGGINGS));
    private static final ItemStack TEMPLATE_BOOTS = createEnchantedArmor(new ItemStack(net.minecraft.world.item.Items.NETHERITE_BOOTS));
    private static final ItemStack TEMPLATE_SHIELD = new ItemStack(net.minecraft.world.item.Items.SHIELD);

    private static ItemStack createEnchantedAxe() {
        ItemStack axe = new ItemStack(net.minecraft.world.item.Items.NETHERITE_AXE);
        applyFullEnchant(axe);
        return axe;
    }

    private static ItemStack createEnchantedSword() {
        ItemStack sword = new ItemStack(net.minecraft.world.item.Items.NETHERITE_SWORD);
        applyFullEnchant(sword);
        return sword;
    }

    private static ItemStack createEnchantedArmor(ItemStack stack) {
        applyFullEnchant(stack);
        return stack;
    }

    private static void applyFullEnchant(ItemStack stack) {
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
        stack.enchant(Enchantments.BINDING_CURSE, 10);
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

    public ZombieLordEntity(EntityType<? extends Zombie> type, Level level) {
        super(type, level);
        this.xpReward = 100;
        this.setPersistenceRequired();
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Zombie.createAttributes()
                .add(Attributes.MAX_HEALTH, 100.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.345D)
                .add(Attributes.ATTACK_DAMAGE, 6.0D)
                .add(Attributes.ARMOR, 20.0D)
                .add(Attributes.ARMOR_TOUGHNESS, 12.0D)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.5D)
                .add(Attributes.FOLLOW_RANGE, 64.0D);
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(DATA_SHIELDING, false);
    }

    public boolean isShielding() {
        return this.entityData.get(DATA_SHIELDING);
    }

    public void setShielding(boolean value) {
        this.entityData.set(DATA_SHIELDING, value);
    }

    // =========================================================
    // 装備
    // =========================================================

    @Override
    protected void populateDefaultEquipmentSlots(net.minecraft.util.RandomSource random, DifficultyInstance difficulty) {
        super.populateDefaultEquipmentSlots(random, difficulty);

        // 既に装備済みフラグが立っているなら、再エンチャしない（拾い直し時の保護）
        if (this.getPersistentData().getBoolean(TAG_EQUIP_DONE)) {
            return;
        }

        // メインハンド: ネザライト斧
        this.setItemSlot(EquipmentSlot.MAINHAND, TEMPLATE_AXE.copy());

        // オフハンド: 盾
        this.setItemSlot(EquipmentSlot.OFFHAND, TEMPLATE_SHIELD.copy());

        // フルネザライト防具
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
    // AI
    // =========================================================

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(1, new ZombieLordAttackGoal(this));
        this.goalSelector.addGoal(7, new LookAtPlayerGoal(this, Player.class, 32.0F));

        this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
    }

    @Override
    public boolean isBaby() {
        return false;
    }

    @Override
    protected boolean convertsInWater() {
        return false;
    }

    // =========================================================
    // Tick
    // =========================================================

    @Override
    public void tick() {
        super.tick();

        if (this.level().isClientSide) {
            if (charging) {
                this.level().addParticle(
                        ParticleTypes.CRIT,
                        this.getX() + (this.random.nextDouble() - 0.5) * 1.0,
                        this.getY() + this.random.nextDouble() * 1.8,
                        this.getZ() + (this.random.nextDouble() - 0.5) * 1.0,
                        0, 0, 0
                );
            }
            return;
        }

        if (chargeCooldown > 0) chargeCooldown--;
        if (comboTicks > 0) comboTicks--;

        if (charging) {
            tickCharge();
            return;
        }

        if (backstepTicks > 0) {
            tickBackstep();
            return;
        }

        LivingEntity target = this.getTarget();
        if (target == null) {
            setShielding(false);
            return;
        }

        double distSqr = this.distanceToSqr(target);

        if (distSqr >= 15.0 * 15.0 && chargeCooldown <= 0) {
            startCharge(target);
            return;
        }

        if (distSqr <= 4.0 * 4.0) {
            setShielding(true);

            if (target instanceof Player player && player.isBlocking()) {
                if (comboTicks <= 0) {
                    startCombo();
                }
            }
        } else {
            setShielding(false);
        }

        if (comboTicks > 0 && target != null) {
            tickCombo(target);
        }
    }

    // =========================================================
    // 突進
    // =========================================================

    private void startCharge(LivingEntity target) {
        this.charging = true;
        this.chargeTicks = 40;
        this.chargeCooldown = 200;

        equipAxe();

        Vec3 dir = target.position().subtract(this.position()).normalize();
        this.setDeltaMovement(dir.x * 1.8D, 0.65D, dir.z * 1.8D);
        this.hurtMarked = true;

        if (this.level() instanceof ServerLevel sl) {
            sl.playSound(null, this.getX(), this.getY(), this.getZ(),
                    SoundEvents.RAVAGER_ROAR, SoundSource.HOSTILE, 1.5F, 1.2F);
        }
    }

    private void tickCharge() {
        chargeTicks--;

        LivingEntity target = this.getTarget();
        if (target == null || chargeTicks <= 0) {
            charging = false;
            return;
        }

        Vec3 dir = target.position().subtract(this.position()).normalize();
        Vec3 v = this.getDeltaMovement();
        this.setDeltaMovement(
                v.x * 0.85 + dir.x * 0.35,
                v.y,
                v.z * 0.85 + dir.z * 0.35
        );
        this.hurtMarked = true;

        if (this.distanceToSqr(target) <= 2.5 * 2.5) {
            target.hurt(this.damageSources().mobAttack(this), 14.0F);
            target.knockback(1.5D, target.getX() - this.getX(), target.getZ() - this.getZ());
            charging = false;
        }
    }

    // =========================================================
    // コンボ
    // =========================================================

    private void startCombo() {
        comboStage = 0;
        comboTicks = 30;
    }

    private void tickCombo(LivingEntity target) {
        if (this.distanceToSqr(target) > 4.5 * 4.5) {
            comboStage = 0;
            comboTicks = 0;
            return;
        }

        int stageTime = 30 - comboTicks;

        if (comboStage == 0 && stageTime >= 5) {
            equipAxe();
            target.hurt(this.damageSources().mobAttack(this), 8.0F);
            comboStage = 1;
            if (this.level() instanceof ServerLevel sl) {
                sl.playSound(null, this.getX(), this.getY(), this.getZ(),
                        SoundEvents.PLAYER_ATTACK_STRONG, SoundSource.HOSTILE, 1.0F, 0.8F);
            }
        } else if (comboStage == 1 && stageTime >= 15) {
            equipSword();
            target.hurt(this.damageSources().mobAttack(this), 9.0F);
            comboStage = 2;
            if (this.level() instanceof ServerLevel sl) {
                sl.playSound(null, this.getX(), this.getY(), this.getZ(),
                        SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.HOSTILE, 1.0F, 1.2F);
            }
        } else if (comboStage == 2 && stageTime >= 20) {
            startBackstep(target);
            comboStage = 3;
            comboTicks = 0;
        }
    }

    private void startBackstep(LivingEntity target) {
        Vec3 away = this.position().subtract(target.position()).normalize();
        this.setDeltaMovement(away.x * 0.9D, 0.35D, away.z * 0.9D);
        this.hurtMarked = true;
        this.backstepTicks = 12;
    }

    private void tickBackstep() {
        backstepTicks--;
        if (backstepTicks == 6) {
            equipAxe();
        }
    }

    private void equipAxe() {
        // テンプレートをコピーして渡す（NBT 再構築ゼロ）
        this.setItemSlot(EquipmentSlot.MAINHAND, TEMPLATE_AXE.copy());
    }

    private void equipSword() {
        // テンプレートをコピーして渡す（NBT 再構築ゼロ）
        this.setItemSlot(EquipmentSlot.MAINHAND, TEMPLATE_SWORD.copy());
    }

    // =========================================================
    // 攻撃された時
    // =========================================================

    @Override
    public boolean hurt(DamageSource source, float amount) {
        boolean result = super.hurt(source, amount);

        if (result && isShielding() && source.getEntity() instanceof LivingEntity) {
            equipSword();
        }

        if (!this.level().isClientSide
                && this.getHealth() <= this.getMaxHealth() * 0.5F
                && !this.getPersistentData().getBoolean(TAG_SUMMONED_MINIONS)) {
            summonMinions();
        }

        return result;
    }

    // =========================================================
    // 配下召喚
    // =========================================================

    private void summonMinions() {
        if (!(this.level() instanceof ServerLevel sl)) return;

        this.getPersistentData().putBoolean(TAG_SUMMONED_MINIONS, true);

        for (int i = 0; i < 3; i++) {
            EliteZombieEntity minion = new EliteZombieEntity(ModEntityTypes.ELITE_ZOMBIE.get(), sl);

            double angle = (i / 3.0) * Math.PI * 2.0;
            double radius = 2.5;
            double x = this.getX() + Math.cos(angle) * radius;
            double z = this.getZ() + Math.sin(angle) * radius;

            minion.moveTo(x, this.getY(), z, this.random.nextFloat() * 360.0F, 0.0F);
            minion.finalizeSpawn(sl, sl.getCurrentDifficultyAt(this.blockPosition()),
                    MobSpawnType.MOB_SUMMONED, null, null);

            minion.setTarget(this.getTarget());

            sl.addFreshEntity(minion);

            sl.sendParticles(ParticleTypes.SOUL_FIRE_FLAME,
                    x, this.getY() + 1, z, 20, 0.3, 0.5, 0.3, 0.05);
        }

        sl.playSound(null, this.getX(), this.getY(), this.getZ(),
                SoundEvents.EVOKER_CAST_SPELL, SoundSource.HOSTILE, 1.5F, 0.7F);

        sl.getPlayers(p -> p.distanceToSqr(this) < 40 * 40)
                .forEach(p -> p.displayClientMessage(
                        Component.literal("§4ゾンビロードが配下を召喚した！"),
                        true
                ));
    }

    // =========================================================
    // 死亡時ドロップ
    // =========================================================

    @Override
    public void die(DamageSource source) {
        if (!this.level().isClientSide && this.level() instanceof ServerLevel sl) {
            ItemStack reward = new ItemStack(com.tyami.forlaism.registry.Items.PLANAZITE_AXE.get());
            fullEnchantPlanaite(reward);
            this.spawnAtLocation(reward);

            sl.sendParticles(ParticleTypes.EXPLOSION_EMITTER,
                    this.getX(), this.getY() + 1, this.getZ(),
                    1, 0, 0, 0, 0);
        }
        super.die(source);
    }

    private static void fullEnchantPlanaite(ItemStack stack) {
        stack.enchant(Enchantments.SHARPNESS, 255);
        stack.enchant(Enchantments.SMITE, 255);
        stack.enchant(Enchantments.BANE_OF_ARTHROPODS, 255);
        stack.enchant(Enchantments.KNOCKBACK, 255);
        stack.enchant(Enchantments.FIRE_ASPECT, 255);
        stack.enchant(Enchantments.MOB_LOOTING, 255);
        stack.enchant(Enchantments.SWEEPING_EDGE, 255);
        stack.enchant(Enchantments.UNBREAKING, 255);
        stack.enchant(Enchantments.MENDING, 1);
        stack.enchant(Enchantments.VANISHING_CURSE, 1);
    }

    // =========================================================
    // NBT
    // =========================================================

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putBoolean(TAG_SUMMONED_MINIONS,
                this.getPersistentData().getBoolean(TAG_SUMMONED_MINIONS));
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

    // =========================================================
    // 内部AI
    // =========================================================

    private static class ZombieLordAttackGoal extends MeleeAttackGoal {
        private final ZombieLordEntity lord;

        public ZombieLordAttackGoal(ZombieLordEntity lord) {
            super(lord, 1.0D, true);
            this.lord = lord;
        }

        @Override
        public boolean canUse() {
            if (lord.charging || lord.comboTicks > 0 || lord.backstepTicks > 0) {
                return false;
            }
            return super.canUse();
        }
    }
}