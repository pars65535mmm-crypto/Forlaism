package com.tyami.forlaism.jei;

import com.tyami.forlaism.Forlaism;
import com.tyami.forlaism.registry.Fluids;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.category.IRecipeCategory;
import mezz.jei.api.gui.ingredient.IRecipeSlotDrawable;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

public class Tier2RecipeCategory implements IRecipeCategory<Tier2JeiRecipe> {
    public static final RecipeType<Tier2JeiRecipe> ALLOY = RecipeType.create(Forlaism.MOD_ID, "alloy_machine", Tier2JeiRecipe.class);
    public static final RecipeType<Tier2JeiRecipe> CONCENTRATOR = RecipeType.create(Forlaism.MOD_ID, "concentrator", Tier2JeiRecipe.class);
    public static final RecipeType<Tier2JeiRecipe> REACTOR = RecipeType.create(Forlaism.MOD_ID, "reactor", Tier2JeiRecipe.class);
    private final RecipeType<Tier2JeiRecipe> type; private final Component title; private final IDrawable background; private final IDrawable icon;
    public Tier2RecipeCategory(mezz.jei.api.helpers.IGuiHelper helper, RecipeType<Tier2JeiRecipe> type, Component title, ItemStack iconStack) {
        this.type = type; this.title = title; background = helper.createDrawable(new ResourceLocation(Forlaism.MOD_ID, "textures/gui/container/jeiyou.png"), 0, 0, 176, 86); icon = helper.createDrawableItemStack(iconStack);
    }
    @Override public RecipeType<Tier2JeiRecipe> getRecipeType(){return type;}
    @Override public Component getTitle(){return title;}
    @Override public IDrawable getBackground(){return background;}
    @Override public IDrawable getIcon(){return icon;}
    @Override public void setRecipe(IRecipeLayoutBuilder builder, Tier2JeiRecipe recipe, IFocusGroup focuses) {
        int x = 62;
        for (ItemStack input : recipe.inputs()) { builder.addSlot(RecipeIngredientRole.INPUT, x, 35).addItemStack(input); x += 18; }
        builder.addSlot(RecipeIngredientRole.OUTPUT, 116, 35).addItemStack(recipe.output());
        if (recipe.fo() > 0) builder.addSlot(RecipeIngredientRole.INPUT, 152, 16).addFluidStack(Fluids.FO.get(), recipe.fo());
    }
}
