package com.tyami.forlaism.item;

import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.ArmorMaterials;
import net.minecraft.world.item.crafting.Ingredient;

public class GarbageMetalArmorMaterial implements ArmorMaterial {

    public static final GarbageMetalArmorMaterial INSTANCE =
            new GarbageMetalArmorMaterial();

    private static final int[] DURABILITY = {
            429,
            495,
            528,
            561
    };

    private static final int[] DEFENSE = {
            4,
            10,
            8,
            4
    };

    private GarbageMetalArmorMaterial() {
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
        return ArmorMaterials.DIAMOND.getEnchantmentValue();
    }

    @Override
    public net.minecraft.sounds.SoundEvent getEquipSound() {
        return SoundEvents.ARMOR_EQUIP_DIAMOND;
    }

    @Override
    public Ingredient getRepairIngredient() {
        return Ingredient.of(
                com.tyami.forlaism.registry.Items.FORLAISM_IRON_ALLOY.get()
        );
    }

    @Override
public String getName() {
    return "forlaism:garbage_metal";
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