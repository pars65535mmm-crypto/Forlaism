package com.tyami.forlaism.item;

import com.tyami.watelib.client.AnimatedItemRenderer;
import com.tyami.watelib.effect.GradientDirection;
import com.tyami.watelib.effect.Waveform;
import com.tyami.watelib.item.IAnimatedItemVisual;
import com.tyami.watelib.item.IAnimatedTextItem;
import com.tyami.watelib.text.AnimatedText;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import javax.annotation.Nullable;
import java.util.List;

/**
 * 微睡。
 *
 * 「静かに、安らかに、安心して、心行くまま、お眠りください。」
 *
 * 眠りを誘うように、静かにゆっくりと流れる。
 */
public class MadoromiItem extends Item
        implements IAnimatedTextItem, IAnimatedItemVisual {

    /** 深い眠りの藍。 */
    private static final int COLOR_SLEEP_DEEP = 0x1A1A4A;

    /** 微睡の紫。 */
    private static final int COLOR_DROWSY = 0x6A5A9A;

    /** 柔らかな月光。 */
    private static final int COLOR_MOON = 0xD8D0FF;

    /** 揺り籠の白。 */
    private static final int COLOR_CRADLE = 0xFFFFFF;

    public MadoromiItem(Properties properties) {
        super(properties.stacksTo(1).fireResistant().rarity(Rarity.EPIC));
    }

    // =========================================================
    // アイテム名
    // =========================================================
    //
    // 静かに、ゆっくりタイプライターで「微睡」が現れる。
    @Override
    public AnimatedText createAnimatedName(ItemStack stack) {
        return AnimatedText.of("微睡")
                .dropIn(2.0F, 20.0F, 1.5F)
                .sparkle(0xFFFFFFFF, 2.0F, 0.8F)
                // 深い眠りの藍 → 微睡の紫 → 月光 → 白
                .gradient(COLOR_SLEEP_DEEP, COLOR_DROWSY, COLOR_MOON, COLOR_CRADLE)
                // とてもゆっくり流れる
                .gradientSpeed(0.15F)
                .gradientPhase(0.7F)
                // 柔らかな揺れ
                .wave(Waveform.SINE, 0.8F, 0.15F, 0.40F)
                // ほんのり光る
                .glow(0x40D8D0FF, 1);
    }

    // =========================================================
    // Tooltip
    // =========================================================
    @Override
    public AnimatedText createAnimatedTooltip(ItemStack stack, int lineIndex) {

        // 1行目: 指定の文言
        if (lineIndex == 1) {
            return AnimatedText.of("静かに、安らかに、安心して、心行くまま、お眠りください。")
                    .sparkle(0xFFFFFFFF, 2.0F, 0.8F)
                    // 外から中央へ、微睡の紫 → 月光（内側に引き込むイメージ）
                    .gradient(GradientDirection.OUTSIDE_IN, COLOR_DROWSY, COLOR_MOON, COLOR_CRADLE)
                    // とてもゆっくり流れる
                    .gradientSpeed(0.12F)
                    .gradientPhase(0.5F)
                    // 静かな揺らぎ
                    .wave(Waveform.SINE, 0.5F, 0.10F, 0.35F)
                    // ほのかな縁取り
                    .outline(0x60000000, 1)
                    .colorNoise(3.0F, 0.3F, 0.4F)
                    .glow(0x40D8D0FF, 1);
        }

        // 2行目: 静寂の追記
        if (lineIndex == 2) {
            return AnimatedText.of("— 旅路は、どこまでも。")
                    .sparkle(0xFFFFFFFF, 2.0F, 0.8F)
                    // 静かなグラデ
                    .gradient(COLOR_SLEEP_DEEP, COLOR_MOON)
                    .gradientSpeed(0.10F)
                    .dropIn(100.0F, 60.0F, 15.0F)
                    // 中央を少し光らせる
                    .glow(0x30FFFFFF, 1);
        }

        return null;
    }


}