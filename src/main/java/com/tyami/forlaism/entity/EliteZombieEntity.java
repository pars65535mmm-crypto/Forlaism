package com.tyami.forlaism.entity;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;

/**
 * エリートゾンビ。
 *
 * 軽量化:
 *   - エンチャント済み装備は static テンプレート化
 *   - populateDefaultEquipmentSlots は 1 回だけエンチャ詰め
 */
public class EliteZombieEntity extends Zombie {

    private int chargeCooldown = 0;
    private boolean charging = false;
    private int chargeTicks = 0;

    private static final String TAG_EQUIP_DONE = "ForlaismEliteZombieEquipped";

    // =========================================================
    // 装備テンプレート（static 1回だけ生成）
    // =========================================================

    private static final ItemStack TEMPLATE_AXE = createEnchantedAxe();
    private static final ItemStack TEMPLATE_HELMET = createEnchantedArmor(new ItemStack(Items.IRON_HELMET));
    private static final ItemStack TEMPLATE_CHEST = createEnchantedArmor(new ItemStack(Items.IRON_CHESTPLATE));
    private static final ItemStack TEMPLATE_LEGS = createEnchantedArmor(new ItemStack(Items.IRON_LEGGINGS));
    private static final ItemStack TEMPLATE_BOOTS = createEnchantedArmor(new ItemStack(Items.IRON_BOOTS));

    private static ItemStack createEnchantedAxe() {
        ItemStack axe = new ItemStack(Items.IRON_AXE);
        axe.enchant(Enchantments.SHARPNESS, 5);
        axe.enchant(Enchantments.UNBREAKING, 3);
        return axe;
    }

    private static ItemStack createEnchantedArmor(ItemStack stack) {
        stack.enchant(Enchantments.ALL_DAMAGE_PROTECTION, 5);
        stack.enchant(Enchantments.UNBREAKING, 3);
        return stack;
    }

    public EliteZombieEntity(EntityType<? extends Zombie> type, Level level) {
        super(type, level);
        this.xpReward = 20;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Zombie.createAttributes()
                .add(Attributes.MAX_HEALTH, 40.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.28D)
                .add(Attributes.ATTACK_DAMAGE, 5.0D)
                .add(Attributes.ARMOR, 10.0D)
                .add(Attributes.FOLLOW_RANGE, 48.0D);
    }

    @Override
    protected void populateDefaultEquipmentSlots(net.minecraft.util.RandomSource random, DifficultyInstance difficulty) {
        super.populateDefaultEquipmentSlots(random, difficulty);

        if (this.getPersistentData().getBoolean(TAG_EQUIP_DONE)) {
            return;
        }

        // 鉄斧
        this.setItemSlot(EquipmentSlot.MAINHAND, TEMPLATE_AXE.copy());

        // フル鉄装備
        this.setItemSlot(EquipmentSlot.HEAD, TEMPLATE_HELMET.copy());
        this.setItemSlot(EquipmentSlot.CHEST, TEMPLATE_CHEST.copy());
        this.setItemSlot(EquipmentSlot.LEGS, TEMPLATE_LEGS.copy());
        this.setItemSlot(EquipmentSlot.FEET, TEMPLATE_BOOTS.copy());

        for (EquipmentSlot slot : EquipmentSlot.values()) {
            this.setDropChance(slot, 0.0F);
        }

        this.getPersistentData().putBoolean(TAG_EQUIP_DONE, true);
    }

    @Override
    public boolean isBaby() {
        return false;
    }

    @Override
    protected boolean convertsInWater() {
        return false;
    }

    @Override
    public void tick() {
        super.tick();

        if (this.level().isClientSide) return;

        if (chargeCooldown > 0) chargeCooldown--;

        if (charging) {
            tickCharge();
            return;
        }

        LivingEntity target = this.getTarget();
        if (target == null) return;

        double distSqr = this.distanceToSqr(target);

        // 15マス以上で突進
        if (distSqr >= 15.0 * 15.0 && chargeCooldown <= 0) {
            startCharge(target);
        }
    }

    private void startCharge(LivingEntity target) {
        charging = true;
        chargeTicks = 30;
        chargeCooldown = 120;

        Vec3 dir = target.position().subtract(this.position()).normalize();
        this.setDeltaMovement(dir.x * 1.4D, 0.55D, dir.z * 1.4D);
        this.hurtMarked = true;
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
                v.x * 0.85 + dir.x * 0.3,
                v.y,
                v.z * 0.85 + dir.z * 0.3
        );
        this.hurtMarked = true;

        if (this.distanceToSqr(target) <= 2.0 * 2.0) {
            target.hurt(this.damageSources().mobAttack(this), 8.0F);
            charging = false;
        }
    }

    @Nullable
    @Override
    public SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty,
                                        MobSpawnType reason, @Nullable SpawnGroupData spawnData,
                                        @Nullable CompoundTag dataTag) {
        SpawnGroupData result = super.finalizeSpawn(level, difficulty, reason, spawnData, dataTag);
        this.populateDefaultEquipmentSlots(this.random, difficulty);
        return result;
    }
}