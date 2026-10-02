package com.tyami.forlaism.item;

import com.tyami.watelib.item.IAnimatedTextItem;
import com.tyami.watelib.text.AnimatedText;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/**
 * デスメタル。
 *
 * コンスチール + コンニャクダイト + 賢者の石？ から作られる合金素材。
 *
 * Tooltip「輪廻の向こう側へ....」は波 + グラデーションで表示。
 */
public class DeathMetalItem extends Item implements IAnimatedTextItem {

    public DeathMetalItem(Properties properties) {
        super(properties);
    }

    // =========================================================
    // アイテム名（普通のまま）
    // =========================================================

    @Override
    public AnimatedText createAnimatedName(ItemStack stack) {
        // 翻訳キーから表示名を取得
        String translated = net.minecraft.network.chat.Component
                .translatable(this.getDescriptionId(stack))
                .getString();
        return AnimatedText.of(translated);
    }

    // =========================================================
    // Tooltip をアニメーション
    // =========================================================

    @Override
    public AnimatedText createAnimatedTooltip(ItemStack stack, int lineIndex) {

        // 1行目だけアニメーション
        if (lineIndex == 1) {
            return AnimatedText.of("輪廻の向こう側へ....")
                    // 波
                    .wave(2.5F, 0.35F, 0.50F)
                    // グラデーション：暗い紫 → 薄い紫 → 白 → 薄い紫 → 暗い紫
                    .gradient(0xFF3A0066, 0xFFAA00FF, 0xFFFFFFFF, 0xFFAA00FF, 0xFF3A0066)
                    // 色の流れる速さ
                    .gradientSpeed(0.6F)
                    // 位相
                    .gradientPhase(0.5F);
        }

        return null;
    }
}