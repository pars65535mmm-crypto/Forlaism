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
 * アクアリング。
 *
 * 泳ぐ速度1.5倍、水の抵抗なし、水中での採掘速度低下なし。
 */
public class AquaRingItem extends Item implements ICurioItem {

    public AquaRingItem(Properties properties) {
        super(properties.stacksTo(1));
    }

    @Override
    public void appendHoverText(
            ItemStack stack,
            @Nullable Level level,
            List<Component> tooltip,
            TooltipFlag flag
    ) {
        tooltip.add(Component.literal("§3泳ぎ速度 ×1.5")
                .withStyle(ChatFormatting.DARK_AQUA));
        tooltip.add(Component.literal("§3水の抵抗なし")
                .withStyle(ChatFormatting.DARK_AQUA));
        tooltip.add(Component.literal("§3水中での採掘速度低下なし")
                .withStyle(ChatFormatting.DARK_AQUA));
        tooltip.add(Component.literal("§7水を駆ける指輪。")
                .withStyle(ChatFormatting.GRAY));
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return false;
    }
}