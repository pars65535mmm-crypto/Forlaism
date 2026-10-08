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
 * 夢幻。
 *
 * 「夢幻と泡沫は対になるもの、
 *   ならば「無限」は何と対になるのだろうか」
 *
 * ぐるぐる回って、ぐわぐわんぐわん震える。
 */
public class DreamItem extends Item
        implements IAnimatedTextItem, IAnimatedItemVisual {

    /** 夢幻の紫。 */
    private static final int COLOR_DREAM_DEEP = 0x6A00AA;

    /** 幻想的なピンク。 */
    private static final int COLOR_DREAM_PINK = 0xFF66CC;

    /** 泡沫の白。 */
    private static final int COLOR_FOAM = 0xFFFFFF;

    /** 深淵の黒紫。 */
    private static final int COLOR_ABYSS = 0x1A0033;

    public DreamItem(Properties properties) {
        super(properties.stacksTo(1).fireResistant().rarity(Rarity.EPIC));
    }

    // =========================================================
    // アイテム名
    // =========================================================
    //
    // 夢幻っぽく紫〜ピンク〜白でぐるぐる動く。
    @Override
    public AnimatedText createAnimatedName(ItemStack stack) {
        return AnimatedText.of("夢幻")
                // ゆらゆら揺れる
                .wave(Waveform.SINE, 2.0F, 0.40F, 0.50F)
                .sway(1.5F, 0.55F, 0.30F)
                // 深紫 → ピンク → 白 → ピンク → 深紫
                .gradient(COLOR_DREAM_DEEP, COLOR_DREAM_PINK, COLOR_FOAM, COLOR_DREAM_PINK, COLOR_DREAM_DEEP)
                .gradientSpeed(0.8F)
                .gradientPhase(0.5F)
                // 幻想の彩り
                .rainbowCycle(0.25F, 0.40F, 0.45F, 0.85F)
                // きらめき
                .sparkle(0xFFFFCCFF, 3.0F, 0.7F)
                // 発光
                .glow(0x80FF66CC, 2);
    }

    // =========================================================
    // Tooltip
    // =========================================================
    @Override
    public AnimatedText createAnimatedTooltip(ItemStack stack, int lineIndex) {

        // 1行目: 指定の文言
        if (lineIndex == 1) {
            return AnimatedText.of("夢幻と泡沫は対になるもの、ならば「無限」は何と対になるのだろうか")
                    // ゆるやかに波打つ
                    .wave(Waveform.SMOOTHSTEP, 1.5F, 0.25F, 0.35F)
                    // 中央から外へ広がるグラデで「拡散する問い」を表現
                    .gradient(GradientDirection.CENTER_OUT, COLOR_DREAM_PINK, COLOR_DREAM_DEEP, COLOR_FOAM)
                    .gradientSpeed(0.35F)
                    .gradientPhase(0.4F)
                    // 星屑のような輝き
                    .sparkle(0xFFFFCCFF, 2.5F, 0.6F)
                    // ほんのり縁取り
                    .outline(0x80000000, 1)
                    .glow(0x60FF66CC, 2);
        }

        // 2行目: 自由枠
        if (lineIndex == 2) {
            return AnimatedText.of("泡沫は弾け、夢幻は醒めない。")
                    // 儚く揺れる
                    .wave(Waveform.SINE, 1.8F, 0.30F, 0.45F)
                    .sway(1.2F, 0.40F, 0.25F)
                    // 泡のような青白 ⇄ 紫
                    .gradient(COLOR_FOAM, COLOR_DREAM_PINK, COLOR_DREAM_DEEP)
                    .gradientSpeed(0.4F)
                    .gradientPhase(0.35F)
                    // 儚いきらめき
                    .sparkle(0xB0FFFFFF, 4.0F, 0.8F)
                    .glow(0x50AAAAFF, 1);
        }

        return null;
    }

    // =========================================================
    // 手に持った時の見た目
    // =========================================================
    //
    // もう、ぐるぐる と ぐわぐわんぐわん を全開で。
    //
    //   sway          : 手ブレ（度）→ これが「傾きグルグル」の主役
    //   shake         : ガタガタ揺れ（ブロック単位）→ ぐわぐわん
    //   wave          : 上下の浮遊（ブロック単位）→ ふわふわ
    //   outline       : 縁取りの ARGB (0 で無効)
    //   outlineRadius : 縁取りの太さ（px）
    //   speed         : アニメ速度 → 爆速
    @Override
    public AnimatedItemRenderer.Visual createVisual(ItemStack stack) {
        return new AnimatedItemRenderer.Visual(
                1.0F,        // sway  : ぐるぐる回す
                0.030F,        // shake : ぐわぐわんぐわん（かなり強め）
                0.020F,        // wave  : ふわふわ上下に大きく浮く
                0x80FF66CC,    // outline: 半透明の夢幻ピンク
                2,             // outlineRadius: 2px で太めに光らせる
                2.5F           // speed : 爆速
        );
    }

     @Override
    public boolean isFoil(ItemStack stack) {
        // 常にエンチャントされたようなピカピカしたエフェクト（Glint）を付与する
        return true;
    }

}