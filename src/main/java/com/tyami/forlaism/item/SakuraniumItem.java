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
 * サクラニウム。
 *
 * 桜 + ウラン + 鉛 + デスメタル から作られる合金素材。
 * 桜色に輝く危険な金属。
 */
public class SakuraniumItem extends Item implements IAnimatedTextItem {

    public SakuraniumItem(Properties properties) {
        super(properties);
    }

    @Override
    public AnimatedText createAnimatedName(ItemStack stack) {
        return AnimatedText.of("サクラニウム")
                .wave(2.5F, 0.30F, 0.50F)
                .gradient(0xFFFFB7C5, 0xFFFFFFFF, 0xFFFF69B4, 0xFFFFFFFF)
                .gradientSpeed(0.7F)
                .gradientPhase(0.6F);
    }

    @Override
    public void appendHoverText(
            ItemStack stack,
            @Nullable Level level,
            List<Component> tooltip,
            TooltipFlag flag
    ) {
        tooltip.add(Component.literal("§d§l先行者達に敬意を込めて。")
                .withStyle(ChatFormatting.LIGHT_PURPLE));
    }

}