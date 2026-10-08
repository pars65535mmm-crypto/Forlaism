package com.tyami.forlaism.item;

import com.tyami.watelib.client.AnimatedItemRenderer;
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
 * ドゥラムクロック。
 *
 * 時を刻む時計。
 * 微睡む九十九の夢、夢幻の欠片、リンネディウム、
 * メタアダマンタイン、イノチノカケラ、フォラリスの結晶、
 * チェレンコフコンニャクダイトを組み合わせて作られる。
 */
public class DulamClockItem extends Item
        implements IAnimatedTextItem, IAnimatedItemVisual {

    /** 淡い青白。 */
    private static final int COLOR_ICE = 0xDBE3FF;

    /** 淡い黄色。 */
    private static final int COLOR_GOLD = 0xFDF55F;

    public DulamClockItem(Properties properties) {
        super(properties.stacksTo(1).fireResistant().rarity(Rarity.EPIC));
    }

    // =========================================================
    // アイテム名
    // =========================================================
    //
    // 淡い青白 ⇄ 淡い黄色の動くグラデーション。
    // 時計の振り子のようにゆっくり揺れる wave と合わせる。
    @Override
    public AnimatedText createAnimatedName(ItemStack stack) {
        return AnimatedText.of("ドゥラムクロック")
                // 青白 ⇄ 黄 をゆっくり循環
                .gradient(COLOR_ICE, COLOR_GOLD)
                .gradientSpeed(0.35F)
                .gradientPhase(0.65F)
                .rainbowCycle(0.15F, 0.30F, 0.30F, 0.85F)
                .glow(0x90FBF3FF, 1);
    }

    // =========================================================
    // Tooltip
    // =========================================================
    //
    // lineIndex == 1 が本文1行目。
    @Override
    public AnimatedText createAnimatedTooltip(ItemStack stack, int lineIndex) {

        // 1行目: 説明文
        if (lineIndex == 1) {
            return AnimatedText.of("時は有限であり、刻は非有限である。")
                    // 波をゆるく
                    .wave(Waveform.SMOOTHSTEP, 1.2F, 0.20F, 0.40F)
                    // 逆方向グラデで「時の流れ」を表現
                    .gradient(COLOR_GOLD, COLOR_ICE)
                    .gradientSpeed(0.25F)
                    .gradientPhase(0.30F)
                    // 淡く光る
                    .sparkle(0xB0FFFFFF, 2.0F, 0.6F);
        }

        // 2行目: もうちょい説明
        if (lineIndex == 2) {
            return AnimatedText.of("止まった時は、二度と戻らない。")
                    .gradient(COLOR_ICE, COLOR_GOLD, COLOR_ICE)
                    .gradientSpeed(0.20F)
                    .glow(0x50000000, 1);
        }

        // それ以外の行はバニラ描画に任せる
        return null;
    }

    // =========================================================
    // アイテムを持った時の見た目（手の中のモデル）
    // =========================================================
    //
    // AnimatedItemRenderer.Visual の引数:
    //
    //   sway          : 手ブレ（度）
    //   shake         : ガタガタ揺れ（ブロック単位の微小移動）
    //   wave          : 上下の浮遊（ブロック単位）
    //   outline       : 縁取りの ARGB (0 で無効)
    //   outlineRadius : 縁取りの太さ（px）
    //   speed         : アニメ速度
    @Override
    public AnimatedItemRenderer.Visual createVisual(ItemStack stack) {
        return new AnimatedItemRenderer.Visual(
                3.0F,                    // sway  : ゆったり傾く（振り子っぽく）
                0.004F,                  // shake : ごく僅かに震える
                0.010F,                  // wave  : ふわふわ上下に浮く
                0x80DBE3FF,              // outline: 半透明の淡い青白で縁取り
                1,                       // outlineRadius: 1px
                1.2F                     // speed : ゆっくりめ
        );
    }
}