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

public class MadoromuItem extends ForalisItem implements IAnimatedTextItem { // ← これを実装！

    public MadoromuItem(Properties properties) {
        super(properties);
    }

    // IAnimatedTextItem のメソッドを実装（これで自動的に名前が AnimatedText になる！）
    @Override
    public AnimatedText createAnimatedName(ItemStack stack) {
        return AnimatedText.of("微睡む九十九の夢")
                .gradient(0xFFFFFFFF, 0xFF000000, 0xFFFFFFFF)
                .gradientSpeed(0.6f)
                .gradientPhase(0.8f);
    }

    // getName() はオーバーライドしない！（IAnimatedTextItem が自動で処理してくれる）

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("item.forlaism.madoromu.tooltip").withStyle(ChatFormatting.DARK_GRAY));
        tooltip.add(Component.translatable("item.forlaism.madoromu.tooltip2").withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("item.forlaism.madoromu.tooltip3").withStyle(ChatFormatting.DARK_PURPLE));
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return true;
    }
}