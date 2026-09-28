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
 * ジャンプリング。
 *
 * Curiosの ring スロットに装備可能。
 * ジャンプ力 ×1.5
 * ダッシュ中にジャンプすると、向いている方向へ追加加速。
 */
public class JumpRingItem extends Item implements ICurioItem {

    public JumpRingItem(Properties properties) {
        super(properties.stacksTo(1));
    }

    @Override
    public void appendHoverText(
            ItemStack stack,
            @Nullable Level level,
            List<Component> tooltip,
            TooltipFlag flag
    ) {
        tooltip.add(Component.literal("§a装備中: ジャンプ力 ×1.5")
                .withStyle(ChatFormatting.GREEN));
        tooltip.add(Component.literal("§aダッシュジャンプで前方に加速")
                .withStyle(ChatFormatting.GREEN));
        tooltip.add(Component.literal("§7跳ねるたびに風を感じる指輪。")
                .withStyle(ChatFormatting.GRAY));
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return false;
    }
}