package com.tyami.forlaism.item;

import com.tyami.forlaism.ForalisItem;
import com.tyami.watelib.item.IAnimatedTextItem;
import com.tyami.watelib.text.AnimatedText;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import javax.annotation.Nullable;
import java.util.List;

/**
 * リンネディウムインゴット。
 *
 * Tier 5 素材。
 * 核の自爆と引き換えに、大罪の石から変容して生まれる。
 */
public class RinnediumIngotItem extends ForalisItem implements IAnimatedTextItem {

    public RinnediumIngotItem(Properties properties) {
        super(properties.fireResistant());
    }

    @Override
    public AnimatedText createAnimatedName(ItemStack stack) {
        return AnimatedText.of("リンネディウムインゴット")
                .wave(2.5F, 0.40F, 0.50F)
                .gradient(0xFF1A0033, 0xFFAA00FF, 0xFFFFFFFF, 0xFF6600CC)
                .gradientSpeed(1.0F)
                .gradientPhase(0.6F);
    }

    @Override
    public void appendHoverText(
            ItemStack stack,
            @Nullable Level level,
            List<Component> tooltip,
            TooltipFlag flag
    ) {
        tooltip.add(Component.literal("§5§lTier 5 素材。")
                .withStyle(ChatFormatting.DARK_PURPLE));
        tooltip.add(Component.literal("§7輪廻の果てに生まれた金属。")
                .withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.literal("§8それは、終わりを知らない。")
                .withStyle(ChatFormatting.DARK_GRAY));
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return true;
    }
}