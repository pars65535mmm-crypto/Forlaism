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
 * 想像。
 *
 * 「想像と創造は違う意味を持つ、
 *   だが創造するためには創造するものを想像しなければならない」
 *
 * 赤・黄・紫・青の4色が思考のように駆け巡る。
 */
public class ImaginationItem extends Item
        implements IAnimatedTextItem, IAnimatedItemVisual {

    /** 想像の赤。 */
    private static final int COLOR_RED = 0xFF3333;

    /** 閃きの黄。 */
    private static final int COLOR_YELLOW = 0xFFD933;

    /** 思索の紫。 */
    private static final int COLOR_PURPLE = 0xAA55FF;

    /** 深淵の青。 */
    private static final int COLOR_BLUE = 0x3366FF;

    /** 閃光の白。 */
    private static final int COLOR_FLASH = 0xFFFFFF;

    public ImaginationItem(Properties properties) {
        super(properties.stacksTo(1).fireResistant().rarity(Rarity.EPIC));
    }

    // =========================================================
    // アイテム名
    // =========================================================
    //
    // 赤 → 黄 → 紫 → 青 が巡る。
    // 思考が駆け巡るように速めのグラデ。
    @Override
    public AnimatedText createAnimatedName(ItemStack stack) {
        return AnimatedText.of("想像")
                // 思考の揺らぎ
                .wave(Waveform.SINE, 1.5F, 0.55F, 0.5F)
                .sway(0.8F, 0.35F, 0.3F)
                // 赤 → 黄 → 紫 → 青 → 白 → 青 → 紫 → 黄 → 赤
                .gradient(
                        COLOR_RED,
                        COLOR_YELLOW,
                        COLOR_PURPLE,
                        COLOR_BLUE,
                        COLOR_FLASH,
                        COLOR_BLUE,
                        COLOR_PURPLE,
                        COLOR_YELLOW,
                        COLOR_RED
                )
                // 思考が駆け巡る速さ
                .gradientSpeed(0.9F)
                .gradientPhase(0.4F)
                // 閃きの輝き
                .sparkle(0xFFFFFFFF, 3.0F, 0.6F)
                // ほんのり発光
                .glow(0x60AA55FF, 1);
    }

    // =========================================================
    // Tooltip
    // =========================================================
    @Override
    public AnimatedText createAnimatedTooltip(ItemStack stack, int lineIndex) {

        // 1行目: 指定の文言
        if (lineIndex == 1) {
            return AnimatedText.of("想像と創造は違う意味を持つ、だが創造するためには創造するものを想像しなければならない")
                    // 思考が波打つ
                    .wave(Waveform.SMOOTHSTEP, 1.2F, 0.30F, 0.35F)
                    // 左から右へ、赤→黄→紫→青の流れ
                    .gradient(
                            GradientDirection.LEFT_TO_RIGHT,
                            COLOR_RED,
                            COLOR_YELLOW,
                            COLOR_PURPLE,
                            COLOR_BLUE
                    )
                    .gradientSpeed(0.25F)
                    .gradientPhase(0.5F)
                    // 思考のひらめき
                    .sparkle(0xB0FFFFFF, 2.5F, 0.55F)
                    // 読みやすい縁取り
                    .outline(0x80000000, 1)
                    .glow(0x50AA55FF, 1);
        }

        // 2行目: 思索の追記
        if (lineIndex == 2) {
            return AnimatedText.of("— 想像と創造。")
                    // 静かにタイプライター
                    .typewriter(2, 20)
                    // 赤 → 紫 の逆流
                    .gradient(COLOR_BLUE, COLOR_PURPLE, COLOR_RED)
                    .gradientSpeed(0.20F)
                    .gradientPhase(0.3F)
                    .glow(0x4033D9FF, 1);
        }

        return null;
    }


}