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
 * ロケットリング。
 *
 * ダッシュリング + ジャンプリング の上位互換。
 * さらに空中でShiftを押すと0.4秒間だけ空中を歩ける。
 */
public class RocketRingItem extends Item implements ICurioItem {

    public RocketRingItem(Properties properties) {
        super(properties.stacksTo(1));
    }

    @Override
    public void appendHoverText(
            ItemStack stack,
            @Nullable Level level,
            List<Component> tooltip,
            TooltipFlag flag
    ) {
        tooltip.add(Component.literal("§b装備中: 移動速度 ×1.3")
                .withStyle(ChatFormatting.AQUA));
        tooltip.add(Component.literal("§bダッシュ時: さらに ×1.2")
                .withStyle(ChatFormatting.AQUA));
        tooltip.add(Component.literal("§aジャンプ時に向いている方向へ加速")
                .withStyle(ChatFormatting.GREEN));
        tooltip.add(Component.literal("§d空中でShift: 0.4秒間 空中歩行")
                .withStyle(ChatFormatting.LIGHT_PURPLE));
        tooltip.add(Component.literal("§7空を駆ける指輪。")
                .withStyle(ChatFormatting.GRAY));
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return true;
    }
}