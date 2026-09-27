package com.tyami.forlaism.entity;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.AxeItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Tiers;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import javax.annotation.Nullable;
import java.util.List;

/**
 * プラネザイトアクス。
 *
 * ゾンビロード確定ドロップ。
 * ネザライトアクスの上位互換、エンチャント上限突破済み。
 */
public class PlanaiteAxeItem extends AxeItem {

    public PlanaiteAxeItem(Properties properties) {
        super(
                Tiers.NETHERITE,
                30.0F,   
                5.0F,  // 攻撃速度
                properties.fireResistant().rarity(net.minecraft.world.item.Rarity.EPIC)
        );
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level,
                                List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.literal("§4ゾンビロードの遺産。")
                .withStyle(ChatFormatting.DARK_RED));
        tooltip.add(Component.literal("§7斧。")
                .withStyle(ChatFormatting.GRAY));
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return true;
    }
}