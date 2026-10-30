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
 * 見た目は回転する草ブロック。
 */
public class ImaginationItem extends Item
        implements IAnimatedTextItem, IAnimatedItemVisual {

    /** 想像の赤。 */
    private static final int COLOR_RED = 0xFF3333;

    /** 闇きの黄。 */
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
    // アイテム名（既存のまま）
    // =========================================================

    @Override
    public AnimatedText createAnimatedName(ItemStack stack) {
        return AnimatedText.of("想像")
                .wave(Waveform.SINE, 1.5F, 0.55F, 0.5F)
                .sway(0.8F, 0.35F, 0.3F)
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
                .gradientSpeed(0.9F)
                .gradientPhase(0.4F)
                .sparkle(0xFFFFFFFF, 3.0F, 0.6F)
                .glow(0x60AA55FF, 1);
    }

        @Override
    public void appendHoverText(
            ItemStack stack,
            @Nullable Level level,
            List<Component> tooltip,
            TooltipFlag flag
    ) {
        tooltip.add(Component.empty());  // lineIndex 1 用
        tooltip.add(Component.empty());  // lineIndex 2 用
        }

    // =========================================================
    // Tooltip（既存のまま）
    // =========================================================

    @Override
    public AnimatedText createAnimatedTooltip(ItemStack stack, int lineIndex) {

        if (lineIndex == 1) {
            return AnimatedText.of("想像と創造は違う意味を持つ、だが創造するためには創造するものを想像しなければならない")
                    .wave(Waveform.SMOOTHSTEP, 1.2F, 0.30F, 0.35F)
                    .gradient(
                            GradientDirection.LEFT_TO_RIGHT,
                            COLOR_RED,
                            COLOR_YELLOW,
                            COLOR_PURPLE,
                            COLOR_BLUE
                    )
                    .gradientSpeed(0.25F)
                    .gradientPhase(0.5F)
                    .sparkle(0xB0FFFFFF, 2.5F, 0.55F)
                    .outline(0x80000000, 1)
                    .glow(0x50AA55FF, 1);
        }

        if (lineIndex == 2) {
            return AnimatedText.of("想像トハ創造奴カ?")
                    .scrambleResolve(40, "諠ｳ蜒上→縺ｯ縺昴≧縺槭≧?")
                    .typewriter(2, 20)
                    .gradient(COLOR_BLUE, COLOR_PURPLE, COLOR_RED)
                    .gradientSpeed(0.20F)
                    .gradientPhase(0.3F)
                    .glow(0x4033D9FF, 1);
        }

        return null;
    }

    // =========================================================
    // アイテムの見た目（回転する草ブロック）
    // =========================================================

    /**
     * アイテムモデルを回転させる。
     *
     * AnimatedItemRenderer.Visual の引数:
     *   sway          : 手ブレ（度）→ Y軸まわりの回転
     *   shake         : ガタガタ揺れ（ブロック単位）
     *   wave          : 上下の浮遊（ブロック単位）
     *   outline       : 縁取りの ARGB (0 で無効)
     *   outlineRadius : 縁取りの太さ（px）
     *   speed         : アニメ速度
     *
     * 草ブロックらしく、ゆっくり回転 + ほんのり浮遊感。
     */
    @Override
    public AnimatedItemRenderer.Visual createVisual(ItemStack stack) {
        return new AnimatedItemRenderer.Visual(
                360.0F,          // sway: Y軸をぐるっと1回転させる
                0.000F,          // shake: ガタつきなし
                0.020F,          // wave: ほんの少し上下に浮遊
                0x60A0FFA0,      // outline: 半透明の薄い緑で縁取り
                1,               // outlineRadius: 1px
                1.0F             // speed: 標準速度
        );
    }
}