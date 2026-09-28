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
 * ダッシュリング。
 *
 * Curiosの ring スロットに装備可能。
 * 歩行時: 移動速度 ×1.3
 * ダッシュ時: さらに ×1.2 (= ×1.56)
 */
public class DashRingItem extends Item implements ICurioItem {

    public DashRingItem(Properties properties) {
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
        tooltip.add(Component.literal("§7足元が軽くなる不思議な指輪。")
                .withStyle(ChatFormatting.GRAY));
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return false;
    }
}