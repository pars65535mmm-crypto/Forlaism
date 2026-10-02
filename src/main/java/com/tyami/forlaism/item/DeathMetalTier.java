package com.tyami.forlaism.item;

import net.minecraft.world.item.Tier;
import net.minecraft.world.item.Tiers;
import net.minecraft.world.item.crafting.Ingredient;

/**
 * デスメタルのツールTier。
 *
 * 性能はダイヤの2倍。
 */
public class DeathMetalTier implements Tier {

    public static final DeathMetalTier INSTANCE = new DeathMetalTier();

    private DeathMetalTier() {}

    @Override
    public int getUses() {
        // ダイヤ 1561 の2倍
        return 3122;
    }

    @Override
    public float getSpeed() {
        // ダイヤ 8.0 の2倍
        return Tiers.DIAMOND.getSpeed() * 2.0F;
    }

    @Override
    public float getAttackDamageBonus() {
        // ダイヤ 3.0 の2倍
        return Tiers.DIAMOND.getAttackDamageBonus() * 2.0F;
    }

    @Override
    public int getLevel() {
        // ダイヤと同じ（4）
        return Tiers.DIAMOND.getLevel();
    }

    @Override
    public int getEnchantmentValue() {
        // ダイヤ 10 の2倍
        return Tiers.DIAMOND.getEnchantmentValue() * 2;
    }

    @Override
    public Ingredient getRepairIngredient() {
        return Ingredient.of(
                com.tyami.forlaism.registry.Items.DEATH_METAL.get()
        );
    }
}