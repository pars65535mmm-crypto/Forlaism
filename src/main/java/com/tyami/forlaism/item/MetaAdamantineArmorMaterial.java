package com.tyami.forlaism.item;

import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.crafting.Ingredient;

/**
 * メタアダマンタインの防具素材。
 *
 * 全てのステータスが255。
 */
public class MetaAdamantineArmorMaterial implements ArmorMaterial {

    public static final MetaAdamantineArmorMaterial INSTANCE =
            new MetaAdamantineArmorMaterial();

    /** 全ステータス共通値。 */
    public static final int ALL_STATS = 255;

    /**
     * 部位ごとの耐久値。
     *
     * 全部255だとすぐ壊れるので 255 × 部位係数 で。
     */
    private static final int[] DURABILITY = {
            ALL_STATS * 13, // BOOTS
            ALL_STATS * 15, // LEGGINGS
            ALL_STATS * 16, // CHESTPLATE
            ALL_STATS * 11  // HELMET
    };

    /**
     * 部位ごとの防御力。
     *
     * 全部255。
     */
    private static final int[] DEFENSE = {
            ALL_STATS, // BOOTS
            ALL_STATS, // LEGGINGS
            ALL_STATS, // CHESTPLATE
            ALL_STATS  // HELMET
    };

    private MetaAdamantineArmorMaterial() {
    }

    @Override
    public int getDurabilityForType(net.minecraft.world.item.ArmorItem.Type type) {
        return DURABILITY[type.getSlot().getIndex()];
    }

    @Override
    public int getDefenseForType(net.minecraft.world.item.ArmorItem.Type type) {
        return DEFENSE[type.getSlot().getIndex()];
    }

    @Override
    public int getEnchantmentValue() {
        return ALL_STATS;
    }

    @Override
    public net.minecraft.sounds.SoundEvent getEquipSound() {
        return SoundEvents.ARMOR_EQUIP_NETHERITE;
    }

    @Override
    public Ingredient getRepairIngredient() {
        return Ingredient.of(
                com.tyami.forlaism.registry.Items.META_ADAMANTINE.get()
        );
    }

    @Override
    public String getName() {
        return "forlaism:meta_adamantine";
    }

    @Override
    public float getToughness() {
        return ALL_STATS;
    }

    @Override
    public float getKnockbackResistance() {
        return 1.0F; // 最大1.0
    }
}