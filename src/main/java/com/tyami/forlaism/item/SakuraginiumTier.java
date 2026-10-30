package com.tyami.forlaism.item;

import net.minecraft.world.item.Tier;
import net.minecraft.world.item.Tiers;
import net.minecraft.world.item.crafting.Ingredient;

/**
 * サクラギニウムのツールTier。
 *
 * - 耐久値 101761 (= 319²)
 * - 採掘ティア：ネザライト相当
 * - 採掘速度：ダイヤの1.5倍
 */
public class SakuraginiumTier implements Tier {

    public static final SakuraginiumTier INSTANCE = new SakuraginiumTier();

    /** 耐久値 319²。 */
    public static final int DURABILITY = 101_761;

    private SakuraginiumTier() {
    }

    @Override
    public int getUses() {
        return DURABILITY;
    }

    @Override
    public float getSpeed() {
        // ダイヤの1.5倍
        return Tiers.DIAMOND.getSpeed() * 1.5F;
    }

    @Override
    public float getAttackDamageBonus() {
        return Tiers.NETHERITE.getAttackDamageBonus();
    }

    @Override
    public int getLevel() {
        return Tiers.NETHERITE.getLevel();
    }

    @Override
    public int getEnchantmentValue() {
        return Tiers.NETHERITE.getEnchantmentValue();
    }

    @Override
    public Ingredient getRepairIngredient() {
        return Ingredient.of(
                com.tyami.forlaism.registry.Items.SAKURAGINIUM_INGOT.get()
        );
    }
}