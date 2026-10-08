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
 * 自由。
 *
 * 「自由とは時に残酷なものである。」
 *
 * 羽ばたくように、空を舞う。
 */
public class FreedomItem extends Item
        implements IAnimatedTextItem, IAnimatedItemVisual {

    /** 空の蒼。 */
    private static final int COLOR_SKY = 0x87CEEB;

    /** 風の白。 */
    private static final int COLOR_WIND = 0xFFFFFF;

    /** 遥か彼方の淡青。 */
    private static final int COLOR_FAR = 0x4488CC;

    /** 黄昏の金。 */
    private static final int COLOR_DUSK = 0xFFCC66;

    public FreedomItem(Properties properties) {
        super(properties.stacksTo(1).fireResistant().rarity(Rarity.EPIC));
    }

    // =========================================================
    // アイテム名
    // =========================================================
    //
    // 羽ばたくように、上下にゆったりと大きく揺れる。
    @Override
    public AnimatedText createAnimatedName(ItemStack stack) {
        return AnimatedText.of("自由")
                // 羽ばたきのような、ゆったり大きく滑らかな上下動
                .wave(Waveform.SINE, 5.0F, 0.55F, 0.60F)
                // 横にもゆっくり流れる
                .sway(2.0F, 0.30F, 0.40F)
                // 空 → 風 → 遥か彼方 → 黄昏 → 風 → 空
                .gradient(
                        COLOR_FAR,
                        COLOR_SKY,
                        COLOR_WIND,
                        COLOR_DUSK,
                        COLOR_WIND,
                        COLOR_SKY,
                        COLOR_FAR
                )
                // 風が流れるような速さ
                .gradientSpeed(0.35F)
                .gradientPhase(0.45F)
                // 陽の光のようなきらめき
                .sparkle(0xFFFFEECC, 2.5F, 0.50F)
                // 空に溶けるような柔らかな発光
                .glow(0x6099CCFF, 2);
    }

    // =========================================================
    // Tooltip
    // =========================================================
    @Override
    public AnimatedText createAnimatedTooltip(ItemStack stack, int lineIndex) {

        // 1行目: 自由の定義
        if (lineIndex == 1) {
            return AnimatedText.of("自由とは時に残酷なものである。")
                    // 風に舞うように緩やかな波
                    .wave(Waveform.SMOOTHSTEP, 1.5F, 0.25F, 0.40F)
                    .sway(1.0F, 0.15F, 0.30F)
                    // 外から内へ、広がる空が収束するようなイメージ
                    .gradient(
                            GradientDirection.OUTSIDE_IN,
                            COLOR_SKY,
                            COLOR_WIND,
                            COLOR_FAR
                    )
                    .gradientSpeed(0.20F)
                    .gradientPhase(0.50F)
                    // 陽射しのきらめき
                    .sparkle(0xFFFFEECC, 2.0F, 0.45F)
                    // 読みやすさのための縁取り
                    .outline(0x80000000, 1)
                    .glow(0x5099CCFF, 1);
        }

        // 2行目: 自由枠
        if (lineIndex == 2) {
            return AnimatedText.of("— 空は、どこまでも広い。")
                    .typewriter(2, 20)
                    .gradient(COLOR_DUSK, COLOR_WIND, COLOR_SKY)
                    .gradientSpeed(0.18F)
                    .gradientPhase(0.35F)
                    .glow(0x40FFCC66, 1);
        }

        return null;
    }



    @Override
    public boolean isFoil(ItemStack stack) {
        return true;
    }
}