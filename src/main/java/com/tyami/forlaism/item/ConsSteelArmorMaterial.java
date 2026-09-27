package com.tyami.forlaism.item;

import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.ArmorMaterials;
import net.minecraft.world.item.crafting.Ingredient;

/**
 * コンスチールの防具素材。
 *
 * 性能はダイヤ級、耐久値は1,000,000。
 */
public class ConsSteelArmorMaterial implements ArmorMaterial {

    public static final ConsSteelArmorMaterial INSTANCE =
            new ConsSteelArmorMaterial();

    /** 部位ごとの耐久値（全部100万）。 */
    private static final int[] DURABILITY = {
            1_000_000, // BOOTS
            1_000_000, // LEGGINGS
            1_000_000, // CHESTPLATE
            1_000_000  // HELMET
    };

    /** 部位ごとの防御力（ダイヤと同じ）。 */
    private static final int[] DEFENSE = {
            3, // BOOTS
            6, // LEGGINGS
            8, // CHESTPLATE
            3  // HELMET
    };

    private ConsSteelArmorMaterial() {
    }

    @Override
    public int getDurabilityForType(ArmorItem.Type type) {
        return DURABILITY[type.getSlot().getIndex()];
    }

    @Override
    public int getDefenseForType(ArmorItem.Type type) {
        return DEFENSE[type.getSlot().getIndex()];
    }

    @Override
    public int getEnchantmentValue() {
        return ArmorMaterials.DIAMOND.getEnchantmentValue();
    }

    @Override
    public net.minecraft.sounds.SoundEvent getEquipSound() {
        return SoundEvents.ARMOR_EQUIP_DIAMOND;
    }

    @Override
    public Ingredient getRepairIngredient() {
        return Ingredient.of(
                com.tyami.forlaism.registry.Items.CONS_STEEL.get()
        );
    }

    @Override
    public String getName() {
        return "forlaism:cons_steel";
    }

    @Override
    public float getToughness() {
        return ArmorMaterials.DIAMOND.getToughness();
    }

    @Override
    public float getKnockbackResistance() {
        return ArmorMaterials.DIAMOND.getKnockbackResistance();
    }
}