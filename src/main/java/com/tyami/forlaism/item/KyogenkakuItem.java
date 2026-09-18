package com.tyami.forlaism.item;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import javax.annotation.Nullable;
import java.util.List;

public class KyogenkakuItem extends Item {

    public KyogenkakuItem(Properties properties) {
        super(properties);
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("item.forlaism.kyogenkaku.tooltip").withStyle(ChatFormatting.LIGHT_PURPLE));
        tooltip.add(Component.translatable("item.forlaism.kyogenkaku.tooltip2").withStyle(ChatFormatting.RED));
        tooltip.add(Component.translatable("item.forlaism.kyogenkaku.tooltip3").withStyle(ChatFormatting.GRAY));
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return true;
    }
}