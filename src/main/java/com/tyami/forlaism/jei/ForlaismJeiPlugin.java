package com.tyami.forlaism.jei;

import com.tyami.forlaism.Forlaism;
import com.tyami.forlaism.registry.Items;

import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.registration.IRecipeCategoryRegistration;
import mezz.jei.api.registration.IRecipeRegistration;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

import java.util.List;

@JeiPlugin
public class ForlaismJeiPlugin implements IModPlugin {

    private static final ResourceLocation UID =
            new ResourceLocation(Forlaism.MOD_ID, "jei_plugin");

    @Override
    public ResourceLocation getPluginUid() {
        return UID;
    }

    @Override
    public void registerCategories(IRecipeCategoryRegistration registration) {

        var h = registration.getJeiHelpers().getGuiHelper();

        registration.addRecipeCategories(

                new Tier2RecipeCategory(
                        h,
                        Tier2RecipeCategory.ALLOY,
                        Component.translatable(
                                "block.forlaism.forlaism_alloy_machine"
                        ),
                        new ItemStack(
                                Items.FORLAISM_ALLOY_MACHINE.get()
                        )
                ),

                new Tier2RecipeCategory(
                        h,
                        Tier2RecipeCategory.CONCENTRATOR,
                        Component.translatable(
                                "block.forlaism.forlaism_concentrator"
                        ),
                        new ItemStack(
                                Items.FORLAISM_CONCENTRATOR.get()
                        )
                ),

                new Tier2RecipeCategory(
                        h,
                        Tier2RecipeCategory.REACTOR,
                        Component.translatable(
                                "block.forlaism.forlaism_reactor"
                        ),
                        new ItemStack(
                                Items.FORLAISM_REACTOR.get()
                        )
                )
        );
    }

    @Override
    public void registerRecipes(IRecipeRegistration registration) {

        // =========================================================
        // Alloy Machine
        // =========================================================

        registration.addRecipes(
                Tier2RecipeCategory.ALLOY,
                List.of(
                        new Tier2JeiRecipe(
                                List.of(
                                        new ItemStack(
                                                net.minecraft.world.item.Items.IRON_INGOT
                                        )
                                ),
                                new ItemStack(
                                        Items.FORLAISM_IRON_ALLOY.get()
                                ),
                                1000,
                                2000,
                                100
                        )
                )
        );

        // =========================================================
        // Concentrator
        // =========================================================

        registration.addRecipes(
                Tier2RecipeCategory.CONCENTRATOR,
                List.of(
                        new Tier2JeiRecipe(
                                List.of(
                                        new ItemStack(
                                                Items.FORLAISM_IRON_ALLOY.get()
                                        )
                                ),
                                new ItemStack(
                                        Items.FORLAISM_CONCENTRATED_IRON_ALLOY.get()
                                ),
                                1000,
                                2000,
                                100
                        )
                )
        );

        // =========================================================
        // Forlaism Reactor
        //
        // 不完全なフォラリス星核
        //        ↓
        // フォラリス星核
        //
        // FO: 10000 mB
        // Energy: 5000 FE/t
        // Time: 1280 ticks (64 seconds)
        // =========================================================

        registration.addRecipes(
                Tier2RecipeCategory.REACTOR,
                List.of(
                        new Tier2JeiRecipe(
                                List.of(
                                        new ItemStack(
                                                Items.INCOMPLETE_FORLAISM_STAR_CORE.get()
                                        )
                                ),
                                new ItemStack(
                                        Items.FORLAISM_STAR_CORE.get()
                                ),
                                10000,
                                5000,
                                1280
                        )
                )
        );
    }
}