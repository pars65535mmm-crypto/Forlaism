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
 * 臨界核。
 *
 * 量子融合で生成される中間素材。
 * ネザライトブロック128 + 賢者の石 + ネザースター64 + 1000MFE。
 */
public class CriticalCoreItem extends ForalisItem implements IAnimatedTextItem {

    public CriticalCoreItem(Properties properties) {
        super(properties);
    }

    @Override
    public AnimatedText createAnimatedName(ItemStack stack) {
        return AnimatedText.of("臨界核")
                .wave(2.5F, 0.30F, 0.50F)
                .gradient(0xFFFF0000, 0xFFFFAA00, 0xFFFFFFFF, 0xFFFF00FF)
                .gradientSpeed(0.9F)
                .gradientPhase(0.7F);
    }

    @Override
    public void appendHoverText(
            ItemStack stack,
            @Nullable Level level,
            List<Component> tooltip,
            TooltipFlag flag
    ) {
        tooltip.add(Component.literal("§4§l臨界に達した核。")
                .withStyle(ChatFormatting.DARK_RED));
        tooltip.add(Component.literal("§7これ単体では何も起きないが…")
                .withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.literal("§8更なる融合の素材となる。")
                .withStyle(ChatFormatting.DARK_GRAY));
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return true;
    }
}