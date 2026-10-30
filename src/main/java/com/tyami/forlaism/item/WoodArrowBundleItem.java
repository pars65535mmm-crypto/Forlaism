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
 * 木の矢の束。
 *
 * 木の矢 ×8 を束ねた中間素材。
 * 火の矢のクラフトに使う。
 */
public class WoodArrowBundleItem extends Item {

    public WoodArrowBundleItem(Properties properties) {
        super(properties);
    }

    @Override
    public void appendHoverText(
            ItemStack stack,
            @Nullable Level level,
            List<Component> tooltip,
            TooltipFlag flag
    ) {
        tooltip.add(Component.literal("§7木の矢を束ねたもの。")
                .withStyle(ChatFormatting.GRAY));
    }
}