package com.tyami.forlaism.item;

import com.tyami.forlaism.ForalisItem;
import com.tyami.forlaism.client.magiceffect.IMagicEffectItem;
import com.tyami.forlaism.client.magiceffect.MagicEffectStyle;
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
 * チェレンコフコンニャクダイト。
 *
 * リンネディウム + コンニャクダイト + 100MFE で量子融合。
 * 後ろが青く光る（チェレンコフ放射のオマージュ）。
 */
public class CherenkovKonnyakuDaiteItem extends ForalisItem
        implements IAnimatedTextItem, IMagicEffectItem {

    /** チェレンコフ青。 */
    public static final MagicEffectStyle EFFECT_STYLE = MagicEffectStyle.builder()
            .color(0x8040C0FF)      // 半透明の強いチェレンコフ青
            .intensity(2.0f)        // 強めの光
            .scale(1.2f)            // アイテムを包むサイズ
            .speed(1.6f)            // 高速で脈動
            .ringCount(1)           // 光の輪を1つ
            .particleCount(6)       // 周囲に粒子を6個
            .flares(true)           // フレアON
            .coreGlow(true)         // 中心光ON
            .build();

    public CherenkovKonnyakuDaiteItem(Properties properties) {
        super(properties);
    }

    @Override
    public AnimatedText createAnimatedName(ItemStack stack) {
        return AnimatedText.of("チェレンコフコンニャクダイト")
                .wave(2.5F, 0.30F, 0.50F)
                .gradient(0xFF0040FF, 0xFF40C0FF, 0xFFE0FFFF, 0xFF40C0FF)
                .gradientSpeed(1.2F)
                .gradientPhase(0.7F);
    }

    @Override
    public MagicEffectStyle getMagicEffect(ItemStack stack) {
        return EFFECT_STYLE;
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level,
                                List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.literal("§b§l未知の放射線を放つこんにゃく。")
                .withStyle(ChatFormatting.AQUA));
        tooltip.add(Component.literal("§7後ろが青く光っている…")
                .withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.literal("§8これは本当に食べても大丈夫なのか？")
                .withStyle(ChatFormatting.DARK_GRAY));
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return true;
    }
}