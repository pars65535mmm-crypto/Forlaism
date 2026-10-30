package com.tyami.forlaism.item;

import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.crafting.Ingredient;

/**
 * ハニワイニウムの防具素材。
 *
 * 各部位の防御力合計 = 128 (フル装備時)。
 *
 * 配分:
 *   ヘルメット 32
 *   チェスト  48
 *   レギンス  32
 *   ブーツ    16
 *   合計     128
 */
public class HaniwainiumArmorMaterial implements ArmorMaterial {

    public static final HaniwainiumArmorMaterial INSTANCE =
            new HaniwainiumArmorMaterial();

    /**
     * 部位ごとの耐久値 (ネザライト相当 * 10)
     *
     * index: 0=BOOTS, 1=LEGGINGS, 2=CHESTPLATE, 3=HELMET
     */
    private static final int[] DURABILITY = {
            999_999, // BOOTS
            999_999, // LEGGINGS
            999_999, // CHESTPLATE
            999_999  // HELMET
    };

    /**
     * 部位ごとの防御力。
     *
     * index: 0=BOOTS, 1=LEGGINGS, 2=CHESTPLATE, 3=HELMET
     */
    private static final int[] DEFENSE = {
            16, // BOOTS
            32, // LEGGINGS
            48, // CHESTPLATE
            32  // HELMET
    };

    private HaniwainiumArmorMaterial() {
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
        return 30;
    }

    @Override
    public net.minecraft.sounds.SoundEvent getEquipSound() {
        return SoundEvents.ARMOR_EQUIP_NETHERITE;
    }

    @Override
    public Ingredient getRepairIngredient() {
        return Ingredient.of(
                com.tyami.forlaism.registry.Items.HANIWAINIUM_INGOT.get()
        );
    }

    @Override
    public String getName() {
        return "forlaism:haniwainium";
    }

    @Override
    public float getToughness() {
        return 4.0F;
    }

    @Override
    public float getKnockbackResistance() {
        return 0.2F;
    }
}