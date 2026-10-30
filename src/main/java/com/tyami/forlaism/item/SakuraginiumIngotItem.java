package com.tyami.forlaism.item;

import com.tyami.watelib.item.IAnimatedTextItem;
import com.tyami.watelib.text.AnimatedText;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import javax.annotation.Nullable;
import java.util.List;

/**
 * サクラギニウムインゴット。
 *
 * 桜色に輝く超合金素材。
 * Tier 7 相当。
 */
public class SakuraginiumIngotItem extends Item implements IAnimatedTextItem {

    public SakuraginiumIngotItem(Properties properties) {
        super(properties.fireResistant());
    }

    @Override
    public AnimatedText createAnimatedName(ItemStack stack) {
        return AnimatedText.of("サクラギニウムインゴット")
                .gradient(0xFFFFB7C5, 0xFFFFFFFF, 0xFFFF69B4, 0xFFFFFFFF, 0xFFFFB7C5)
                .gradientSpeed(0.9F)
                .gradientPhase(0.7F)
                .sparkle(0xFFFFE4EC, 2.5F, 0.6F)
                .glow(0x80FF69B4, 2);
    }

    @Override
    public void appendHoverText(
            ItemStack stack,
            @Nullable Level level,
            List<Component> tooltip,
            TooltipFlag flag
    ) {
        tooltip.add(Component.literal("§7サクラニウムの核に星の輝きを宿した合金。")
                .withStyle(ChatFormatting.GRAY));
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return true;
    }
}