package com.tyami.forlaism.item;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import javax.annotation.Nullable;
import java.util.List;

/**
 * 量子レンチ。
 *
 * 量子転送機のネットワーク番号を切り替えるためのツール。
 * 右クリックで +1、スニーク+右クリックで -1。
 */
public class QuantumWrenchItem extends Item {

    public QuantumWrenchItem(Properties properties) {
        super(properties.stacksTo(1));
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level,
                                List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.literal("§b量子転送機のネットワークを切り替える")
                .withStyle(ChatFormatting.AQUA));
        tooltip.add(Component.literal("§7右クリック: §a+1")
                .withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.literal("§7スニーク+右クリック: §c-1")
                .withStyle(ChatFormatting.GRAY));
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return true;
    }
}