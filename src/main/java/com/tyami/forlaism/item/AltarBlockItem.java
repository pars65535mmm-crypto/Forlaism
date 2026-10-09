package com.tyami.forlaism.item;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;

import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * 儀式祭壇のアイテム。操作方法をTooltipで表示する。
 */
public class AltarBlockItem extends BlockItem {

    public AltarBlockItem(Block block, Properties properties) {
        super(block, properties);
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level,
                                List<Component> tooltip, TooltipFlag flag) {

        tooltip.add(Component.literal("§6§l儀式祭壇")
                .withStyle(ChatFormatting.GOLD));

        tooltip.add(Component.empty());

        tooltip.add(Component.literal("§e【操作方法】")
                .withStyle(ChatFormatting.YELLOW));

        tooltip.add(Component.literal("§7・アイテム持ち §f右クリック")
                .withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.literal("  §7→ カーソル位置に配置")
                .withStyle(ChatFormatting.DARK_GRAY));

        tooltip.add(Component.literal("§7・素手 §f右クリック")
                .withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.literal("  §7→ カーソル位置から回収")
                .withStyle(ChatFormatting.DARK_GRAY));

        tooltip.add(Component.literal("§7・§fX§7 + §fスクロール")
                .withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.literal("  §7→ カーソルを左右に移動")
                .withStyle(ChatFormatting.DARK_GRAY));

        tooltip.add(Component.literal("§7・§fY§7 + §fスクロール")
                .withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.literal("  §7→ カーソルを前後に移動")
                .withStyle(ChatFormatting.DARK_GRAY));

        tooltip.add(Component.literal("§7・§fShift§7 + スクロール")
                .withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.literal("  §7→ 3マスずつ高速移動")
                .withStyle(ChatFormatting.DARK_GRAY));

        tooltip.add(Component.literal("§7・§fスニーク§7 + §f右クリック")
                .withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.literal("  §7→ 全アイテム回収")
                .withStyle(ChatFormatting.DARK_GRAY));

        tooltip.add(Component.empty());

        tooltip.add(Component.literal("§8アイテムを9×9に並べて儀式を行う。")
                .withStyle(ChatFormatting.DARK_GRAY));
        tooltip.add(Component.literal("§8レシピが成立すると自動でクラフトされる。")
                .withStyle(ChatFormatting.DARK_GRAY));
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return true;
    }
}