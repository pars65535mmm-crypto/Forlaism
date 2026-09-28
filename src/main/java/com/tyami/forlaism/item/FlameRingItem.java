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
 * フレイムリング。
 *
 * ファイアリング + ブレイズリング の上位互換。
 * さらに攻撃力 ×1.5。
 */
public class FlameRingItem extends Item implements ICurioItem {

    public FlameRingItem(Properties properties) {
        super(properties.stacksTo(1));
    }

    @Override
    public void appendHoverText(
            ItemStack stack,
            @Nullable Level level,
            List<Component> tooltip,
            TooltipFlag flag
    ) {
        tooltip.add(Component.literal("§6装備中: 火炎耐性")
                .withStyle(ChatFormatting.GOLD));
        tooltip.add(Component.literal("§c攻撃時: 相手を燃やし +3ダメージ")
                .withStyle(ChatFormatting.RED));
        tooltip.add(Component.literal("§4攻撃力 ×1.5")
                .withStyle(ChatFormatting.DARK_RED));
        tooltip.add(Component.literal("§7炎を統べる指輪。")
                .withStyle(ChatFormatting.GRAY));
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return true;
    }
}