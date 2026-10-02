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
 * トレジャーリング。
 *
 * 装備中、近くのコンテナ（チェスト・樽・シュルカーボックス等）の
 * 方向と距離をアクションバーに表示する。
 */
public class TreasureRingItem extends Item implements ICurioItem {

    public TreasureRingItem(Properties properties) {
        super(properties.stacksTo(1));
    }

    @Override
    public void appendHoverText(
            ItemStack stack,
            @Nullable Level level,
            List<Component> tooltip,
            TooltipFlag flag
    ) {
        tooltip.add(Component.literal("§6装備中: 近くのチェストの方向を探知")
                .withStyle(ChatFormatting.GOLD));
        tooltip.add(Component.literal("§7宝を探す指輪。")
                .withStyle(ChatFormatting.GRAY));
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return false;
    }
}