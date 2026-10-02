package com.tyami.forlaism.enchantment;

import com.tyami.forlaism.registry.Items;

import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentCategory;

/**
 * リベンジ。
 *
 * デスメタルの剣にのみ付与可能。
 * 死亡時、剣の耐久を 100/Lv % 削って HP全快 + 耐性Lv5 で 2秒後に復活する。
 */
public class RevengeEnchantment extends Enchantment {

    public RevengeEnchantment() {
        super(
                Rarity.RARE,
                EnchantmentCategory.WEAPON,
                new EquipmentSlot[]{ EquipmentSlot.MAINHAND }
        );
    }

    @Override
    public int getMaxLevel() {
        return 3;
    }

    @Override
    public int getMinCost(int level) {
        return 20 + (level - 1) * 15;
    }

    @Override
    public int getMaxCost(int level) {
        return super.getMinCost(level) + 60;
    }

    @Override
    public boolean canEnchant(ItemStack stack) {
        // デスメタルの剣のみ
        return stack.is(Items.DEATH_METAL_SWORD.get());
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