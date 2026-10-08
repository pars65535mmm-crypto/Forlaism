package com.tyami.forlaism.item;

import com.tyami.watelib.item.IAnimatedTextItem;
import com.tyami.watelib.text.AnimatedText;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import javax.annotation.Nullable;
import java.util.List;

/**
 * 次元のカケラ。
 *
 * 第四の壁の破片。
 * 水入り瓶と醸造することで「目覚め薬」になる。
 */
public class DimensionShardItem extends Item implements IAnimatedTextItem {

    public DimensionShardItem(Properties properties) {
        super(properties.fireResistant().rarity(Rarity.RARE));
    }

    @Override
    public AnimatedText createAnimatedName(ItemStack stack) {
        return AnimatedText.of("次元のカケラ")
                .wave(2.5F, 0.35F, 0.50F)
                .gradient(0xFF1A0033, 0xFFAA00FF, 0xFFFFFFFF, 0xFF6600CC)
                .gradientSpeed(0.9F)
                .gradientPhase(0.5F);
    }

    @Override
    public void appendHoverText(
            ItemStack stack,
            @Nullable Level level,
            List<Component> tooltip,
            TooltipFlag flag
    ) {
        tooltip.add(Component.literal("§5壁の破片。")
                .withStyle(ChatFormatting.DARK_PURPLE));
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return true;
    }
}