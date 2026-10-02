package com.tyami.forlaism.enchantment;

import com.tyami.forlaism.registry.Items;

import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentCategory;

/**
 * 無幻。
 *
 * デスメタル防具にのみ付与可能。
 * 最大HPを500%上昇、回復力を100%上昇、装備の耐久値が1/t回復する。
 */
public class MugenEnchantment extends Enchantment {

    public MugenEnchantment() {
        super(
                Rarity.VERY_RARE,
                EnchantmentCategory.ARMOR,
                new EquipmentSlot[]{
                        EquipmentSlot.HEAD,
                        EquipmentSlot.CHEST,
                        EquipmentSlot.LEGS,
                        EquipmentSlot.FEET
                }
        );
    }

    @Override
    public int getMaxLevel() {
        return 1;
    }

    @Override
    public int getMinCost(int level) {
        return 60;
    }

    @Override
    public int getMaxCost(int level) {
        return 120;
    }

    @Override
    public boolean canEnchant(ItemStack stack) {
        // デスメタル防具のみ
        return stack.getItem() instanceof ArmorItem
                && (stack.is(Items.DEATH_METAL_HELMET.get())
                 || stack.is(Items.DEATH_METAL_CHESTPLATE.get())
                 || stack.is(Items.DEATH_METAL_LEGGINGS.get())
                 || stack.is(Items.DEATH_METAL_BOOTS.get()));
    }

    @Override
    public boolean isTreasureOnly() {
        return true;
    }

    @Override
    public boolean isTradeable() {
        return false;
    }

    @Override
    public boolean isDiscoverable() {
        return true;
    }
}