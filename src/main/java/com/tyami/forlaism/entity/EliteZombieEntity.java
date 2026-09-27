package com.tyami.forlaism.entity;

import net.minecraft.nbt.CompoundTag;   // ← ★これ追加！
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
 * ゾンビロードの配下。
 *
 * - フル鉄装備 + 鉄斧
 * - HP40
 * - 突進持ち（15マス以上離れると突進ジャンプ）
 */
public class EliteZombieEntity extends Zombie {

    private int chargeCooldown = 0;
    private boolean charging = false;
    private int chargeTicks = 0;

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

        // 鉄斧
        ItemStack axe = new ItemStack(Items.IRON_AXE);
        axe.enchant(Enchantments.SHARPNESS, 5);
        axe.enchant(Enchantments.UNBREAKING, 3);
        this.setItemSlot(EquipmentSlot.MAINHAND, axe);

        // フル鉄装備
        equipArmor(EquipmentSlot.HEAD, new ItemStack(Items.IRON_HELMET));
        equipArmor(EquipmentSlot.CHEST, new ItemStack(Items.IRON_CHESTPLATE));
        equipArmor(EquipmentSlot.LEGS, new ItemStack(Items.IRON_LEGGINGS));
        equipArmor(EquipmentSlot.FEET, new ItemStack(Items.IRON_BOOTS));

        for (EquipmentSlot slot : EquipmentSlot.values()) {
            this.setDropChance(slot, 0.0F);
        }
    }

    private void equipArmor(EquipmentSlot slot, ItemStack stack) {
        stack.enchant(Enchantments.ALL_DAMAGE_PROTECTION, 5);
        stack.enchant(Enchantments.UNBREAKING, 3);
        this.setItemSlot(slot, stack);
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