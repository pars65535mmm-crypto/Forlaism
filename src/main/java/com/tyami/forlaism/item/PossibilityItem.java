package com.tyami.forlaism.item;

import com.tyami.forlaism.ForalisItem;
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
 * 可能性。
 *
 * Tier 7 隠しアイテム。
 * 「可能性という名の無限は、君から万物を想像させるだろう。」
 *
 * 名前もTooltipもグリッチする。
 */
public class PossibilityItem extends ForalisItem implements IAnimatedTextItem {

    public PossibilityItem(Properties properties) {
        super(properties
                .stacksTo(1)
                .fireResistant()
                .rarity(Rarity.EPIC));
    }

    // =========================================================
    // 名前（グリッチ + 動くグラデ + グロー）
    // =========================================================

    @Override
    public AnimatedText createAnimatedName(ItemStack stack) {
        return AnimatedText.of("可能性")
                // たゆたう波
                .wave(2.5f, 0.10f, 0.50f)
                // 横揺れも足す
                .sway(1.5f, 0.07f, 0.30f)
                // 黒 → 白 → 紫 → 白 → 黒 で「万象」をイメージ
                .gradient(0xFF000000, 0xFFFFFFFF, 0xFFAA00FF, 0xFFFFFFFF, 0xFF000000)
                // グラデを動かす（0で止まるので0.01以上）
                .gradientSpeed(0.35f)
                .gradientPhase(0.40f)
                // 外側に紫のグロー
                .glow(0x80AA00FF, 2)
                // グリッチで時々色と位置が飛ぶ
                .glitch(2.5f, 0.35f, 0xFFFF00FF);
    }

    // =========================================================
    // Tooltip（行ごとにグリッチ or グラデ）
    // =========================================================

    @Override
    public AnimatedText createAnimatedTooltip(ItemStack stack, int lineIndex) {
        return switch (lineIndex) {
            // 1行目（タイトル扱い）
            case 0 -> AnimatedText.of("可能性")
                    .gradient(0xFFAA00FF, 0xFFFFFFFF, 0xFFAA00FF)
                    .gradientSpeed(0.30f)
                    .glitch(3.0f, 0.25f, 0xFFFF00FF);

            // 2行目
            case 1 -> AnimatedText.of("可能性という名の無限は")
                    .glitch(1.5f, 0.08f, 0xFF8888FF);

            // 3行目
            case 2 -> AnimatedText.of("君から万物を想像させるだろう")
                    .glitch(1.5f, 0.08f, 0xFF8888FF);

            // 4行目（空行）
            case 3 -> null;

            // 5行目以降はバニラ描画に任せる
            default -> null;
        };
    }

    // =========================================================
    // バニラの appendHoverText に追加情報を載せる
    // （IAnimatedTextItem の createAnimatedTooltip が
    //   拾わなかった行をここで書く）
    // =========================================================

    @Override
    public void appendHoverText(
            ItemStack stack,
            @Nullable Level level,
            List<Component> tooltip,
            TooltipFlag flag
    ) {
        // ここに書いた Component は
        // createAnimatedTooltip が null を返した行にだけ出る
        tooltip.add(Component.literal(""));
        tooltip.add(Component.literal("§8無限の可能性を内包している。")
                .withStyle(ChatFormatting.DARK_GRAY));

        // シフト押下で追加説明
        if (net.minecraft.client.gui.screens.Screen.hasShiftDown()) {
            tooltip.add(Component.literal(""));
            tooltip.add(Component.literal("§6【可能性】").withStyle(ChatFormatting.GOLD));
            tooltip.add(Component.literal("§e・これは存在しない。されど、存在する。")
                    .withStyle(ChatFormatting.YELLOW));
            tooltip.add(Component.literal("§e・全ては君の中に、君は全ての中に。")
                    .withStyle(ChatFormatting.YELLOW));
            tooltip.add(Component.literal("§8…まだ何も始まっていない。")
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