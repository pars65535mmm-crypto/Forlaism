package com.tyami.forlaism.item;

import com.tyami.watelib.client.AnimatedItemRenderer;
import com.tyami.watelib.effect.GradientDirection;
import com.tyami.watelib.effect.Waveform;
import com.tyami.watelib.item.IAnimatedItemVisual;
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
 * 無限。
 *
 * 「無限と虚無は対になるもの、
 *   ならば"夢幻"は何と対になるのだろうか」
 *
 * 静けさの中の威厳。
 * タイプライターで1文字ずつ現れ、静かに輝く。
 */
public class InfinityItem extends Item
        implements IAnimatedTextItem, IAnimatedItemVisual {

    /** 深い夜空の青。 */
    private static final int COLOR_NIGHT = 0x0A0A2E;

    /** 遥か彼方の青。 */
    private static final int COLOR_AZURE = 0x3355CC;

    /** 静寂の白。 */
    private static final int COLOR_WHITE = 0xFFFFFF;

    /** 星の金。 */
    private static final int COLOR_STAR = 0xFFD966;

    public InfinityItem(Properties properties) {
        super(properties.stacksTo(1).fireResistant().rarity(Rarity.EPIC));
    }

    // =========================================================
    // アイテム名
    // =========================================================
    //
    // タイプライターで「無限」が1文字ずつ現れる。
    // 静かに深い青 → 白 → 金 のグラデで威厳を表現。
    @Override
    public AnimatedText createAnimatedName(ItemStack stack) {
        return AnimatedText.of("無限")
                // タイプライター: 4tickごとに1文字、初回10tick待つ
                .typewriter(4, 10)
                .scrambleResolve(60, "Infinity")
                // 深い夜空 → 遥か彼方の青 → 白 → 星の金
                .gradient(COLOR_NIGHT, COLOR_AZURE, COLOR_WHITE, COLOR_STAR)
                // ゆっくり流れる
                .gradientSpeed(0.20F)
                .gradientPhase(0.6F)
                // ほんのり白光（強すぎない）
                .glow(0x40FFFFFF, 1);
    }

    // =========================================================
    // Tooltip
    // =========================================================
    @Override
    public AnimatedText createAnimatedTooltip(ItemStack stack, int lineIndex) {

        // 1行目: 指定の文言
        if (lineIndex == 1) {
            return AnimatedText.of("無限と虚無は対になるもの、ならば\"夢幻\"は何と対になるのだろうか")
                    // タイプライター: 2tickごとに1文字、初回15tick
                    .typewriter(2, 15)
                    // 中央から外へ、深い夜空の青 → 星の金
                    .gradient(GradientDirection.CENTER_OUT, COLOR_AZURE, COLOR_STAR, COLOR_WHITE)
                    // 静かに流れる
                    .gradientSpeed(0.15F)
                    .gradientPhase(0.5F)
                    // ほんのり縁取り（読みやすさ）
                    .outline(0x80000000, 1);
        }

        // 2行目: 静寂の追記
        if (lineIndex == 2) {
            return AnimatedText.of("— 果ては、まだ見えぬ。")
                    // タイプライター（もっとゆっくり）
                    .typewriter(3, 40)
                    // 静かなグラデ（暗 → 明）
                    .gradient(COLOR_NIGHT, COLOR_WHITE)
                    .gradientSpeed(0.10F)
                    // 中央を少し光らせる
                    .glow(0x30FFD966, 1);
        }

        return null;
    }

}