package com.tyami.forlaism.item;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import top.theillusivec4.curios.api.type.capability.ICurioItem;

import javax.annotation.Nullable;
import java.util.List;

/**
 * ムーンリング。
 *
 * 夜間のみ能力が強化される。
 * - 移動速度 1.5倍
 * - ジャンプ力 1.5倍
 * - 防御力 3倍
 * - HP 3倍
 * - 回復力 3倍
 * - 攻撃力 3倍
 * - 暗視
 */
public class MoonRingItem extends Item implements ICurioItem {

    public MoonRingItem(Properties properties) {
        super(properties.stacksTo(1));
    }

    @Override
    public void appendHoverText(
            ItemStack stack,
            @Nullable Level level,
            List<Component> tooltip,
            TooltipFlag flag
    ) {
        tooltip.add(Component.literal("§9【夜間のみ】")
                .withStyle(ChatFormatting.BLUE));
        tooltip.add(Component.literal("§7・移動速度 ×1.5")
                .withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.literal("§7・ジャンプ力 ×1.5")
                .withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.literal("§7・防御力 ×3")
                .withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.literal("§7・HP ×3")
                .withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.literal("§7・回復力 ×3")
                .withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.literal("§7・攻撃力 ×3")
                .withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.literal("§7・暗視")
                .withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.literal("§8月の光を纏う指輪。")
                .withStyle(ChatFormatting.DARK_GRAY));
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return true;
    }
}