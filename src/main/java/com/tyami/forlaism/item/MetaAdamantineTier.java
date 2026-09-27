package com.tyami.forlaism.item;

import net.minecraft.world.item.Tier;
import net.minecraft.world.item.crafting.Ingredient;

/**
 * メタアダマンタインのツールTier。
 *
 * 全てのステータスが255。
 */
public class MetaAdamantineTier implements Tier {

    public static final MetaAdamantineTier INSTANCE = new MetaAdamantineTier();

    /** 全ステータス共通値。 */
    public static final int ALL_STATS = 255;

    private MetaAdamantineTier() {
    }

    @Override
    public int getUses() {
        // 255だとすぐ壊れるので、ツールの耐久は 255 * 255 = 65025 に
        return ALL_STATS * ALL_STATS;
    }

    @Override
    public float getSpeed() {
        return ALL_STATS;
    }

    @Override
    public float getAttackDamageBonus() {
        return ALL_STATS;
    }

    @Override
    public int getLevel() {
        return ALL_STATS;
    }

    @Override
    public int getEnchantmentValue() {
        return ALL_STATS;
    }

    @Override
    public Ingredient getRepairIngredient() {
        return Ingredient.of(
                com.tyami.forlaism.registry.Items.META_ADAMANTINE.get()
        );
    }
}