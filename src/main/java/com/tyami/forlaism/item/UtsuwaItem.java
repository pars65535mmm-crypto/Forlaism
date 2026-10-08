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
 * ウツワ。
 *
 * ただの器。
 * ポーズメニューの「セーブする」ボタンを押すと、
 * 中に「決意」が宿る。
 */
public class UtsuwaItem extends Item implements IAnimatedTextItem {

    public UtsuwaItem(Properties properties) {
        super(properties);
    }

    @Override
    public AnimatedText createAnimatedName(ItemStack stack) {
        return AnimatedText.of("ウツワ")
                .gradient(0xFF888888, 0xFFDDDDDD, 0xFF888888);
    }

    @Override
    public void appendHoverText(
            ItemStack stack,
            @Nullable Level level,
            List<Component> tooltip,
            TooltipFlag flag
    ) {
        tooltip.add(Component.literal("§7からっぽの器。")
                .withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.literal("§8何かを注ぐことで意味を持つ。")
                .withStyle(ChatFormatting.DARK_GRAY));
    }
}