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
 * サンリング。
 *
 * 昼間のみ能力が強化される。
 * - 落下無効
 * - 発光
 * - 暗視
 * - HP 5倍
 * - 回復力 5倍
 * - 防御力 5倍
 * - 周囲の敵対者を燃やす
 */
public class SunRingItem extends Item implements ICurioItem {

    public SunRingItem(Properties properties) {
        super(properties.stacksTo(1));
    }

    @Override
    public void appendHoverText(
            ItemStack stack,
            @Nullable Level level,
            List<Component> tooltip,
            TooltipFlag flag
    ) {
        tooltip.add(Component.literal("§6【昼間のみ】")
                .withStyle(ChatFormatting.GOLD));
        tooltip.add(Component.literal("§7・落下ダメージ無効")
                .withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.literal("§7・発光")
                .withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.literal("§7・暗視")
                .withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.literal("§7・HP ×5")
                .withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.literal("§7・回復力 ×5")
                .withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.literal("§7・防御力 ×5")
                .withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.literal("§7・周囲の敵対者を燃やす")
                .withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.literal("§8太陽の光を纏う指輪。")
                .withStyle(ChatFormatting.DARK_GRAY));
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return true;
    }
}