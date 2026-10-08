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
 * 決意。
 *
 * 「やり抜く力。押し通す力。諦めない力。不動の力。」
 *
 * 意志の炎が揺らめくように、赤〜白〜金で燃え上がる。
 */
public class DeterminationItem extends Item
        implements IAnimatedTextItem, IAnimatedItemVisual {

    /** 決意の深紅。 */
    private static final int COLOR_CRIMSON = 0x8B0000;

    /** 燃え上がる炎の赤。 */
    private static final int COLOR_FLAME = 0xFF3300;

    /** 意志の白。 */
    private static final int COLOR_WILL = 0xFFFFFF;

    /** 不動の金。 */
    private static final int COLOR_IMMOVABLE = 0xFFD700;

    public DeterminationItem(Properties properties) {
        super(properties.stacksTo(1).fireResistant().rarity(Rarity.EPIC));
    }

    // =========================================================
    // アイテム名
    // =========================================================
    //
    // 炎が内側から燃え上がるように、深紅→炎→白→金が巡る。
    @Override
    public AnimatedText createAnimatedName(ItemStack stack) {
        return AnimatedText.of("決意")
                // 心の内に燃え続ける炎のような揺らぎ
                .wave(Waveform.SINE, 2.0F, 0.35F, 0.50F)
                .sway(1.0F, 0.25F, 0.40F)
                // 深紅 → 炎 → 白 → 金 → 白 → 炎 → 深紅
                .gradient(
                        COLOR_CRIMSON,
                        COLOR_FLAME,
                        COLOR_WILL,
                        COLOR_IMMOVABLE,
                        COLOR_WILL,
                        COLOR_FLAME,
                        COLOR_CRIMSON
                )
                // 意志が燃え続ける速さ
                .gradientSpeed(0.45F)
                .gradientPhase(0.35F)
                // 炎の粉のような輝き
                .sparkle(0xFFFFDD88, 3.5F, 0.65F)
                // 揺るがぬ意志のオーラ
                .glow(0x80FF6600, 2);
    }

    // =========================================================
    // Tooltip
    // =========================================================
    @Override
    public AnimatedText createAnimatedTooltip(ItemStack stack, int lineIndex) {

        // 1行目: 決意の定義
        if (lineIndex == 1) {
            return AnimatedText.of("やり抜く力。押し通す力。諦めない力。不動の力。")
                    // 揺るがぬ意志を表す力強い波
                    .wave(Waveform.SMOOTHSTEP, 1.2F, 0.30F, 0.35F)
                    // 中心から外へ広がる「意志の放射」
                    .gradient(
                            GradientDirection.CENTER_OUT,
                            COLOR_WILL,
                            COLOR_FLAME,
                            COLOR_CRIMSON
                    )
                    .gradientSpeed(0.20F)
                    .typewriter(2, 20)
                    .gradientPhase(0.45F)
                    // 炎の粉
                    .sparkle(0xFFFFCC66, 2.5F, 0.55F)
                    // 読みやすさのための縁取り
                    .outline(0x80000000, 1)
                    .glow(0x60FF8800, 1);
        }

        // 2行目: 自由枠
        if (lineIndex == 2) {
            return AnimatedText.of("— その歩みは、誰にも止められない。")
                    // 静かに、だが力強く
                    .typewriter(2, 20)
                    // 深紅 → 金 の逆流
                    .gradient(COLOR_IMMOVABLE, COLOR_FLAME, COLOR_CRIMSON)
                    .gradientSpeed(0.15F)
                    .gradientPhase(0.30F)
                    // 意志の残光
                    .glow(0x40FFD700, 1);
        }

        return null;
    }


    @Override
    public boolean isFoil(ItemStack stack) {
        return true;
    }
}