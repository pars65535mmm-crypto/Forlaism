package com.tyami.forlaism.recipe;

import com.tyami.forlaism.registry.ModPotions;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionUtils;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraftforge.common.brewing.BrewingRecipeRegistry;
import net.minecraftforge.common.brewing.IBrewingRecipe;

public final class ModBrewingRecipes {

    private ModBrewingRecipes() {}

    public static void registerRecipes() {

        BrewingRecipeRegistry.addRecipe(new IBrewingRecipe() {

            @Override
            public boolean isInput(ItemStack input) {
                return input.is(Items.POTION)
                        && PotionUtils.getPotion(input) == Potions.WATER;
            }

            @Override
            public boolean isIngredient(ItemStack ingredient) {
                return ingredient.is(
                        com.tyami.forlaism.registry.Items.DIMENSION_SHARD.get()
                );
            }

            @Override
            public ItemStack getOutput(ItemStack input, ItemStack ingredient) {
                if (!isInput(input)) return ItemStack.EMPTY;
                if (!isIngredient(ingredient)) return ItemStack.EMPTY;

                return PotionUtils.setPotion(
                        new ItemStack(com.tyami.forlaism.registry.Items.AWAKENING_POTION.get()),
                        ModPotions.AWAKENING.get()
                );
            }
        });
    }
}