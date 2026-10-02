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
 * トワイライトリング。
 *
 * 昼夜問わず常時発動する最上位リング。
 * ムーン + サンの効果を統合したもの。
 */
public class TwilightRingItem extends Item implements ICurioItem {

    public TwilightRingItem(Properties properties) {
        super(properties.stacksTo(1));
    }

    @Override
    public void appendHoverText(
            ItemStack stack,
            @Nullable Level level,
            List<Component> tooltip,
            TooltipFlag flag
    ) {
        tooltip.add(Component.literal("§d§l【常時発動】")
                .withStyle(ChatFormatting.LIGHT_PURPLE));
        tooltip.add(Component.literal("§7・移動速度 ×1.5")
                .withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.literal("§7・ジャンプ力 ×1.5")
                .withStyle(ChatFormatting.GRAY));
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
        tooltip.add(Component.literal("§7・火炎耐性")
                .withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.literal("§7・溺れない")
                .withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.literal("§8薄明の光を纏う指輪。")
                .withStyle(ChatFormatting.DARK_GRAY));
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return true;
    }
}