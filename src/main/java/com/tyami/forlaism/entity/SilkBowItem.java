package com.tyami.forlaism.entity;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import javax.annotation.Nullable;
import java.util.List;

/**
 * シルクボウ。
 *
 * スケルトンロード確定ドロップ。
 * 無限・ダメージ増加・パンチ・火炎 Lv10。
 */
public class SilkBowItem extends BowItem {

    public SilkBowItem(Properties properties) {
        super(properties.durability(2048).fireResistant()
                .rarity(net.minecraft.world.item.Rarity.EPIC));
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level,
                                List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.literal("§bスケルトンロードの遺産。")
                .withStyle(ChatFormatting.AQUA));
        tooltip.add(Component.literal("§7無限・ダメージ増加・パンチ・火炎 Lv10")
                .withStyle(ChatFormatting.GRAY));
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return true;
    }
}