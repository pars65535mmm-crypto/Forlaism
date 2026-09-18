package com.tyami.forlaism.item;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import javax.annotation.Nullable;
import java.util.List;

public class StarOreBookItem extends Item {

    public static final int MAX_BOOST = 10;

    public StarOreBookItem(Properties properties) {
        super(properties);
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("item.forlaism.star_ore_book.tooltip").withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("item.forlaism.star_ore_book.tooltip2").withStyle(ChatFormatting.AQUA));
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return true;
    }
}