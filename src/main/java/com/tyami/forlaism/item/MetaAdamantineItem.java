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
 * メタアダマンタイン。
 *
 * 愚者の石 + rough_steel ×200 を量子融合して得られる。
 * 武器・ツール・防具すべてのステータスが255になる究極素材。
 */
public class MetaAdamantineItem extends ForalisItem implements IAnimatedTextItem {

    public MetaAdamantineItem(Properties properties) {
        super(properties);
    }

    @Override
    public AnimatedText createAnimatedName(ItemStack stack) {
        return AnimatedText.of("メタアダマンタイン")
                .wave(2.5F, 0.30F, 0.50F)
                .gradient(0xFFFF0000, 0xFFFFAA00, 0xFFFFFFFF, 0xFF00FFFF)
                .gradientSpeed(0.6F)
                .gradientPhase(0.8F);
    }

    @Override
    public void appendHoverText(
            ItemStack stack,
            @Nullable Level level,
            List<Component> tooltip,
            TooltipFlag flag
    ) {
        tooltip.add(Component.literal("§4§l未知なる金属。")
                .withStyle(ChatFormatting.DARK_RED));
        tooltip.add(Component.literal("§7武器・ツール・防具、あらゆる性能が §c255 §7に達する。")
                .withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.literal("§8愚者の石と200の粗鋼が、ここに至る。")
                .withStyle(ChatFormatting.DARK_GRAY));
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return true;
    }
}