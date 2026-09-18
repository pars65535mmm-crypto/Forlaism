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
 * イノチノカケラ。
 *
 * Dreamで儀式の短剣を使い、村人から搾取した命の欠片。
 */
public class InochiNoKakeraItem extends Item implements IAnimatedTextItem {

    public InochiNoKakeraItem(Properties properties) {
        super(properties);
    }

    @Override
    public AnimatedText createAnimatedName(ItemStack stack) {
        return AnimatedText.of("イノチノカケラ")
                .wave(2.0F, 0.25F, 0.40F)
                .gradient(0xFF660000, 0xFFFF2222, 0xFFFFAAAA)
                .gradientSpeed(0.6F)
                .gradientPhase(0.5F);
    }

    @Override
    public void appendHoverText(
            ItemStack stack,
            @Nullable Level level,
            List<Component> tooltip,
            TooltipFlag flag
    ) {
        tooltip.add(Component.literal("§4生贄から搾り取った命の欠片。")
                .withStyle(ChatFormatting.DARK_RED));
        tooltip.add(Component.literal("§7微かに脈打っている…")
                .withStyle(ChatFormatting.GRAY));
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return true;
    }
}