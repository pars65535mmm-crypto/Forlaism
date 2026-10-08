package com.tyami.forlaism.item;

import com.tyami.watelib.client.AnimatedItemRenderer;
import com.tyami.watelib.effect.GradientDirection;
import com.tyami.watelib.effect.Waveform;
import com.tyami.watelib.item.IAnimatedItemVisual;
import com.tyami.watelib.item.IAnimatedTextItem;
import com.tyami.watelib.text.AnimatedText;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import javax.annotation.Nullable;
import java.util.List;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import javax.annotation.Nullable;
import java.util.List;

/**
 * マスターピースクロック。
 *
 * 「常に決意を持ち、自由を手に入れ、可能性を信じ続け、
 *   夢幻の力を使い、無限を超え、想像し、挑戦をやめず、
 *   微睡を持った者のみが手に入れることのできる銀時計」
 *
 * 9つの概念が交わる、至高の銀時計。
 */
public class MasterpieceClockItem extends Item
        implements IAnimatedTextItem, IAnimatedItemVisual {

    /** 銀の輝き。 */
    private static final int COLOR_SILVER = 0xE0E0E0;

    /** 銀の影。 */
    private static final int COLOR_SILVER_DARK = 0x888888;

    /** 時を刻む金。 */
    private static final int COLOR_GOLD = 0xFFD700;

    /** 至高の白。 */
    private static final int COLOR_PURE = 0xFFFFFF;

    public MasterpieceClockItem(Properties properties) {
        super(properties.stacksTo(1).fireResistant().rarity(Rarity.EPIC));
    }

        @Override
public void appendHoverText(
        ItemStack stack,
        @Nullable Level level,
        List<Component> tooltip,
        TooltipFlag flag
) {
    // ダミー行を3行追加（createAnimatedTooltip が呼ばれるようにするため）
    tooltip.add(Component.empty());  // lineIndex 1
    tooltip.add(Component.empty());  // lineIndex 2
    tooltip.add(Component.empty());  // lineIndex 3
}
    // =========================================================
    // アイテム名
    // =========================================================
    //
    // 時計の振り子のように、ゆったりと上品に揺れる。
@Override
public AnimatedText createAnimatedName(ItemStack stack) {
    return AnimatedText.of("Masterpiece Clock")
            // 時計の振り子のような、ゆったり大きく揺れる
            .wave(Waveform.SINE, 4.5F, 0.45F, 0.60F)
            .sway(2.5F, 0.35F, 0.45F)
            // 銀 → 純白 → 金 → 純白 → 銀 の循環
            .gradient(
                    COLOR_SILVER_DARK,
                    COLOR_SILVER,
                    COLOR_PURE,
                    COLOR_GOLD,
                    COLOR_PURE,
                    COLOR_SILVER,
                    COLOR_SILVER_DARK
            )
            // 時の流れのように、ゆっくり
            .gradientSpeed(0.35F)
            .gradientPhase(0.50F)
            .typewriter(3, 5)
            // 虹色のオーラを重ねる（9つの概念の煌めき）
            .rainbowCycle(0.45F, 0.30F, 0.35F, 0.95F)
            // 銀のきらめき（密度アップ）
            .sparkle(0xFFFFFFFF, 4.5F, 0.75F)
            // 上品な発光
            .glow(0xA0FFFFFF, 2);
}

// =========================================================
// Tooltip
// =========================================================
@Override
public AnimatedText createAnimatedTooltip(ItemStack stack, int lineIndex) {

    // 1行目: 定義（長文）
    if (lineIndex == 1) {
        return AnimatedText.of(
                        "常に決意を持ち、自由を手に入れ、可能性を信じ続け、")
                // 揺らめく波
                .wave(Waveform.SINE, 1.5F, 0.35F, 0.40F)
                .sway(0.8F, 0.25F, 0.30F)
                // 左→右へ流れる銀→白→金
                .gradient(
                        GradientDirection.LEFT_TO_RIGHT,
                        COLOR_SILVER,
                        COLOR_PURE,
                        COLOR_GOLD
                )
                .gradientSpeed(0.30F)
                .gradientPhase(0.45F)
                // 虹色のアクセント
                .rainbowCycle(0.30F, 0.35F, 0.40F, 0.90F)
                // 金のきらめき
                .sparkle(0xFFFFEECC, 3.5F, 0.65F)
                // 縁取り + 発光
                .outline(0x80000000, 1)
                .glow(0x80FFFFFF, 2);
    }

    // 2行目: 定義（続き）
    if (lineIndex == 2) {
        return AnimatedText.of(
                        "夢幻の力を使い、無限を超え、想像し、挑戦をやめず、")
                .wave(Waveform.SINE, 1.5F, 0.35F, 0.40F)
                .sway(0.8F, 0.25F, 0.30F)
                .gradient(
                        GradientDirection.LEFT_TO_RIGHT,
                        COLOR_PURE,
                        COLOR_GOLD,
                        COLOR_PURE
                )
                .gradientSpeed(0.30F)
                .gradientPhase(0.55F)
                .rainbowCycle(0.30F, 0.30F, 0.45F, 0.95F)
                .sparkle(0xFFFFEECC, 3.5F, 0.65F)
                .outline(0x80000000, 1)
                .glow(0x80FFFFFF, 2);
    }

    // 3行目: 定義（締め）
    if (lineIndex == 3) {
        return AnimatedText.of(
                        "微睡を持った者のみが手に入れることのできる銀時計")
                // 締めは力強く
                .wave(Waveform.SMOOTHSTEP, 2.0F, 0.45F, 0.45F)
                .sway(1.2F, 0.30F, 0.35F)
                // 中心から外へ、金 → 白 → 銀
                .gradient(
                        GradientDirection.CENTER_OUT,
                        COLOR_GOLD,
                        COLOR_PURE,
                        COLOR_SILVER
                )
                .gradientSpeed(0.25F)
                .gradientPhase(0.60F)
                // 締めの虹オーラ
                .rainbowCycle(0.50F, 0.40F, 0.35F, 1.0F)
                // 派手めのきらめき
                .sparkle(0xFFFFD700, 4.5F, 0.80F)
                // 二重の発光 + 縁取り
                .outline(0x80000000, 1)
                .glow(0x90FFD700, 2);
    }

    return null;
}

    // =========================================================
    // 手に持った時の見た目
    // =========================================================
    //
    // 時計の針が刻むように、優雅に動く。
    @Override
    public AnimatedItemRenderer.Visual createVisual(ItemStack stack) {
        return new AnimatedItemRenderer.Visual(
                2.0F,        // sway: 上品な傾き
                0.003F,      // shake: ごく僅かな振動（時を刻む音）
                0.008F,      // wave: 静かな上下動
                0x80FFFFFF,  // outline: 半透明の銀
                1,           // outlineRadius: 1px
                0.9F         // speed: とてもゆったり
        );
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return true;
    }


}