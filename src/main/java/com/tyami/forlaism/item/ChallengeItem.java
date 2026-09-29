package com.tyami.forlaism.item;

import com.tyami.forlaism.ForalisItem;
import com.tyami.watelib.effect.GradientDirection;
import com.tyami.watelib.item.IAnimatedTextItem;
import com.tyami.watelib.text.AnimatedText;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import javax.annotation.Nullable;
import java.util.List;

/**
 * 挑戦。
 *
 * Tier 7 隠しアイテム。
 * 「あらゆる現実をすべて自分の方へねじ曲げたのだ。」
 *
 * 名前もTooltipも「ねじれ」を表現する。
 */
public class ChallengeItem extends ForalisItem implements IAnimatedTextItem {

    public ChallengeItem(Properties properties) {
        super(properties
                .stacksTo(1)
                .fireResistant()
                .rarity(Rarity.EPIC));
    }

    // =========================================================
    // 名前（ねじれ + グリッチ）
    // =========================================================

    @Override
    public AnimatedText createAnimatedName(ItemStack stack) {
        return AnimatedText.of("挑戦")

                // 中央から外へ色が流れる（自分の方へ引き寄せるイメージ）
                .rotate(-6.0f)
                .gradient(
                        0xFFFF0000,   // 中心: 深紅
                        0xFFFFAA00,   // 橙
                        0xFFFFFFFF,   // 白
                        0xFF00FFFF    // 水色（到達点）
                )
                .gradientSpeed(0.65f)
                .gradientPhase(0.65f)
                // 縦揺れより横揺れの方が「歪み」っぽい
                .glitch(0.5f, 0.25f, 0x000000)
                .wave(1.2f, 0.68f, 0.80f);
    }

    // =========================================================
    // Tooltip（行ごとに演出）
    // =========================================================

    @Override
    public AnimatedText createAnimatedTooltip(ItemStack stack, int lineIndex) {
        return switch (lineIndex) {
            // 1行目（タイトル扱い）
            /* 
            case 0 -> AnimatedText.of("挑戦")
                    .rotate(-6.0f)
                    .gradient(GradientDirection.CENTER_OUT,
                            0xFFFF0000, 0xFFFFFFFF, 0xFFFF0000)
                    .gradientSpeed(0.35f);

                    */

            // 2行目
            case 1 -> AnimatedText.of("§7あらゆる現実をすべて")
                    // 軽く傾ける = 現実をねじ曲げる
                .rotate(-18.0f)
                // 中央から外へ色が流れる（自分の方へ引き寄せるイメージ）
                .gradient(
                        0xFFFF0000,   // 中心: 深紅
                        0xFFFFAA00,   // 橙
                        0xFFFFFFFF,   // 白
                        0xFF00FFFF    // 水色（到達点）
                )
                // 逆走っぽく見せるため位相をずらす
                .gradientSpeed(0.25f)
                .gradientPhase(-0.25f)
                // 縦揺れより横揺れの方が「歪み」っぽい
                .sway(2.5f, 0.12f, 0.45f)
                .wave(1.2f, 0.08f, 0.30f)
                // 中心に白い強いグロー
                .glow(0xC0FFFFFF, 1);
  

            // 3行目
            case 2 -> AnimatedText.of("§7自分の方へねじ曲げたのだ")
                    // 軽く傾ける = 現実をねじ曲げる
                .rotate(-10.0f)
                // 中央から外へ色が流れる（自分の方へ引き寄せるイメージ）
                .gradient(
                       
                        0xFFFF0000,   // 中心: 深紅
                        0xFFFFAA00,   // 橙
                        0xFFFFFFFF,   // 白
                        0xFF00FFFF    // 水色（到達点）
                )
                // 逆走っぽく見せるため位相をずらす
                .gradientSpeed(0.25f)
                .gradientPhase(-0.25f)
                // 縦揺れより横揺れの方が「歪み」っぽい
                .sway(2.5f, 0.12f, 0.45f)
                .wave(1.2f, 0.08f, 0.30f)
                // 中心に白い強いグロー
                .glow(0xC0FFFFFF, 1);


            // 4行目（空行）
            case 3 -> null;

            // 以降はバニラ側に任せる
            default -> null;
        };
    }

    // =========================================================
    // appendHoverText（アニメーションで拾われなかった行）
    // =========================================================

    @Override
    public void appendHoverText(
            ItemStack stack,
            @Nullable Level level,
            List<Component> tooltip,
            TooltipFlag flag
    ) {
        tooltip.add(Component.literal(""));
        tooltip.add(Component.literal("§4§l【Tier 7】").withStyle(ChatFormatting.DARK_RED));
        tooltip.add(Component.literal("§c隠しアイテム。").withStyle(ChatFormatting.RED));
        tooltip.add(Component.literal("§8現実をねじ曲げた先にあるもの。")
                .withStyle(ChatFormatting.DARK_GRAY));

        if (net.minecraft.client.gui.screens.Screen.hasShiftDown()) {
            tooltip.add(Component.literal(""));
            tooltip.add(Component.literal("§6【挑戦】").withStyle(ChatFormatting.GOLD));
            tooltip.add(Component.literal("§e・現実は君の都合に合わせて曲がる。")
                    .withStyle(ChatFormatting.YELLOW));
            tooltip.add(Component.literal("§e・だが、ねじれた先に何があるかは誰も知らない。")
                    .withStyle(ChatFormatting.YELLOW));
            tooltip.add(Component.literal("§8…挑むか？ 挑まざるか？")
                    .withStyle(ChatFormatting.DARK_GRAY));
        } else {
            tooltip.add(Component.literal("§8[Shift] で詳細を表示")
                    .withStyle(ChatFormatting.DARK_GRAY));
        }
    }

    // =========================================================
    // エンチャント光る
    // =========================================================

    @Override
    public boolean isFoil(ItemStack stack) {
        return true;
    }
}