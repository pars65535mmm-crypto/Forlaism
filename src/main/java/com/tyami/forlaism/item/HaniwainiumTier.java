package com.tyami.forlaism.item;

import net.minecraft.world.item.Tier;
import net.minecraft.world.item.Tiers;
import net.minecraft.world.item.crafting.Ingredient;

/**
 * ハニワイニウムのツールTier。
 *
 * 採掘ティア: ネザライト
 * 耐久値: 999,999
 * 採掘速度 / 攻撃力: ネザライトと同等以上
 */
public class HaniwainiumTier implements Tier {

    public static final HaniwainiumTier INSTANCE = new HaniwainiumTier();

    public static final int DURABILITY = 999_999;

    private HaniwainiumTier() {
    }

    @Override
    public int getUses() {
        return DURABILITY;
    }

    @Override
    public float getSpeed() {
        // ネザライト相当
        return Tiers.NETHERITE.getSpeed();
    }

    @Override
    public float getAttackDamageBonus() {
        // 剣本体側で AttributeModifier を当てるので base は 0 でOK
        return 0.0F;
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
                com.tyami.forlaism.registry.Items.HANIWAINIUM_INGOT.get()
        );
    }
}