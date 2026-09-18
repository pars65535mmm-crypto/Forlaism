package com.tyami.forlaism.item;

import net.minecraft.world.item.Tier;
import net.minecraft.world.item.Tiers;

public class GarbageMetalTier implements Tier {

    public static final GarbageMetalTier INSTANCE = new GarbageMetalTier();

    private GarbageMetalTier() {
    }

    @Override
    public int getUses() {
        return 4301;
    }

    @Override
    public float getSpeed() {
        return Tiers.DIAMOND.getSpeed();
    }

    @Override
    public float getAttackDamageBonus() {
        return 0.0F;
    }

    @Override
    public int getLevel() {
        return 3;
    }

    @Override
    public int getEnchantmentValue() {
        return Tiers.DIAMOND.getEnchantmentValue();
    }

    @Override
    public net.minecraft.world.item.crafting.Ingredient getRepairIngredient() {
        return net.minecraft.world.item.crafting.Ingredient.of(
                com.tyami.forlaism.registry.Items.FORLAISM_IRON_ALLOY.get()
        );
    }
}