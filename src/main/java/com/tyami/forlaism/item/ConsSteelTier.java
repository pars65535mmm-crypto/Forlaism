package com.tyami.forlaism.item;

import net.minecraft.world.item.Tier;
import net.minecraft.world.item.Tiers;
import net.minecraft.world.item.crafting.Ingredient;

/**
 * コンスチールのツールTier。
 *
 * 性能はダイヤ級、耐久値は1,000,000。
 */
public class ConsSteelTier implements Tier {

    public static final ConsSteelTier INSTANCE = new ConsSteelTier();

    /** 耐久値 1,000,000。 */
    public static final int DURABILITY = 1_000_000;

    private ConsSteelTier() {
    }

    @Override
    public int getUses() {
        return DURABILITY;
    }

    @Override
    public float getSpeed() {
        return Tiers.DIAMOND.getSpeed();
    }

    @Override
    public float getAttackDamageBonus() {
        return Tiers.DIAMOND.getAttackDamageBonus();
    }

    @Override
    public int getLevel() {
        return Tiers.DIAMOND.getLevel();
    }

    @Override
    public int getEnchantmentValue() {
        return Tiers.DIAMOND.getEnchantmentValue();
    }

    @Override
    public Ingredient getRepairIngredient() {
        return Ingredient.of(
                com.tyami.forlaism.registry.Items.CONS_STEEL.get()
        );
    }
}