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
 * null_sugar。
 *
 * 砂糖が奈落に落ちたとき、1/8192 の確率で生成される。
 * 量子融合で Re糖分の剣 の素材になる。
 */
public class NullSugarItem extends Item implements IAnimatedTextItem {

    public NullSugarItem(Properties properties) {
        super(properties);
    }

    @Override
    public AnimatedText createAnimatedName(ItemStack stack) {
        return AnimatedText.of("null_sugar")
                .wave(2.0F, 0.25F, 0.40F)
                .gradient(0xFF000000, 0xFFAAAAAA, 0xFFFFFFFF)
                .gradientSpeed(0.5F)
                .gradientPhase(0.7F);
    }

    @Override
    public void appendHoverText(
            ItemStack stack,
            @Nullable Level level,
            List<Component> tooltip,
            TooltipFlag flag
    ) {
        tooltip.add(Component.literal("§8奈落の底で結晶化した砂糖")
                .withStyle(ChatFormatting.DARK_GRAY));
        tooltip.add(Component.literal("§7存在が §fnull §7に置き換わっている…")
                .withStyle(ChatFormatting.GRAY));
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return true;
    }
}