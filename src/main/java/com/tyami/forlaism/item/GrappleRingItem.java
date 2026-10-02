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
 * グラップルリング。
 *
 * ダッシュ + フック + 賢者の石？ でクラフト。
 * Cキーでフックを発射し、ヒット地点へ高速移動する。
 */
public class GrappleRingItem extends Item implements ICurioItem {

    public GrappleRingItem(Properties properties) {
        super(properties.stacksTo(1));
    }

    @Override
    public void appendHoverText(
            ItemStack stack,
            @Nullable Level level,
            List<Component> tooltip,
            TooltipFlag flag
    ) {
        tooltip.add(Component.literal("§aCキー: フック発射")
                .withStyle(ChatFormatting.GREEN));
        tooltip.add(Component.literal("§7射程: 100ブロック")
                .withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.literal("§7Shift: フック解除")
                .withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.literal("§7発射中 + 解除後3秒間は落下無効")
                .withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.literal("§8壁を掴む指輪の上位版。")
                .withStyle(ChatFormatting.DARK_GRAY));
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return true;
    }
}