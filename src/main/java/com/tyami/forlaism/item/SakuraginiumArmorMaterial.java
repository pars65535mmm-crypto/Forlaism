package com.tyami.forlaism.item;

import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.ArmorMaterials;
import net.minecraft.world.item.crafting.Ingredient;

/**
 * サクラギニウムの防具素材。
 *
 * フルセット防御力合計 = 36
 *   頭 8 / 胴 12 / 脚 10 / 足 6
 *
 * 耐久 101761（共通）。
 */
public class SakuraginiumArmorMaterial implements ArmorMaterial {

    public static final SakuraginiumArmorMaterial INSTANCE =
            new SakuraginiumArmorMaterial();

    /** 耐久値（全部位共通）。 */
    public static final int DURABILITY = 101_761;

    /** 部位ごとの耐久（全部位同じ）。 */
    private static final int[] DURABILITY_TABLE = {
            DURABILITY, // BOOTS
            DURABILITY, // LEGGINGS
            DURABILITY, // CHESTPLATE
            DURABILITY  // HELMET
    };

    /** 部位ごとの防御力（合計36）。 */
    private static final int[] DEFENSE = {
            6,   // BOOTS
            10,  // LEGGINGS
            12,  // CHESTPLATE
            8    // HELMET
    };

    private SakuraginiumArmorMaterial() {
    }

    @Override
    public int getDurabilityForType(ArmorItem.Type type) {
        return DURABILITY_TABLE[type.getSlot().getIndex()];
    }

    @Override
    public int getDefenseForType(ArmorItem.Type type) {
        return DEFENSE[type.getSlot().getIndex()];
    }

    @Override
    public int getEnchantmentValue() {
        return ArmorMaterials.NETHERITE.getEnchantmentValue();
    }

    @Override
    public net.minecraft.sounds.SoundEvent getEquipSound() {
        return SoundEvents.ARMOR_EQUIP_NETHERITE;
    }

    @Override
    public Ingredient getRepairIngredient() {
        return Ingredient.of(
                com.tyami.forlaism.registry.Items.SAKURAGINIUM_INGOT.get()
        );
    }

    @Override
    public String getName() {
        return "forlaism:sakuraginium";
    }

    @Override
    public float getToughness() {
        return ArmorMaterials.NETHERITE.getToughness();
    }

    @Override
    public float getKnockbackResistance() {
        return 0.1F;
    }
}