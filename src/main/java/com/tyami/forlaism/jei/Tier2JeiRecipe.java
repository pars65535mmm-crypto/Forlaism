package com.tyami.forlaism.jei;

import net.minecraft.world.item.ItemStack;
import java.util.List;

public record Tier2JeiRecipe(List<ItemStack> inputs, ItemStack output, int fo, int fe, int ticks) {
    public Tier2JeiRecipe { inputs = List.copyOf(inputs); output = output.copy(); }
}
