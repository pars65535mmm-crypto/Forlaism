package com.tyami.forlaism.item;

import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.ArmorMaterials;
import net.minecraft.world.item.crafting.Ingredient;

/**
 * デスメタルの防具素材。
 *
 * 性能はダイヤの2倍。
 */
public class DeathMetalArmorMaterial implements ArmorMaterial {

    public static final DeathMetalArmorMaterial INSTANCE =
            new DeathMetalArmorMaterial();

    /** 部位ごとの耐久値（ダイヤの2倍）。 */
    private static final int[] DURABILITY = {
            33 * 2 * 2, // BOOTS   (ダイヤ33 * 部位係数 * 2倍)
            33 * 2 * 4, // LEGGINGS
            33 * 2 * 6, // CHESTPLATE
            33 * 2 * 3  // HELMET
    };

    /** 部位ごとの防御力（ダイヤの2倍）。 */
    private static final int[] DEFENSE = {
            3 * 2, // BOOTS
            6 * 2, // LEGGINGS
            8 * 2, // CHESTPLATE
            3 * 2  // HELMET
    };

    private DeathMetalArmorMaterial() {}

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
        return ArmorMaterials.DIAMOND.getEnchantmentValue() * 2;
    }

    @Override
    public net.minecraft.sounds.SoundEvent getEquipSound() {
        return SoundEvents.ARMOR_EQUIP_NETHERITE;
    }

    @Override
    public Ingredient getRepairIngredient() {
        return Ingredient.of(
                com.tyami.forlaism.registry.Items.DEATH_METAL.get()
        );
    }

    @Override
    public String getName() {
        return "forlaism:death_metal";
    }

    @Override
    public float getToughness() {
        // ダイヤと同じ 2.0F
        return ArmorMaterials.DIAMOND.getToughness();
    }

    @Override
    public float getKnockbackResistance() {
        return 0.0F;
    }
}