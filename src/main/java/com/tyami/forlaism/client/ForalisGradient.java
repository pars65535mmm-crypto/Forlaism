package com.tyami.forlaism.client;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;

public final class ForalisGradient {

    private ForalisGradient() {
    }

    // 左：水色
    private static final int START_COLOR = 0x55FFFF;

    // 右：白
    private static final int END_COLOR = 0xFFFFFF;

    /**
     * 水色 → 白のForalisグラデーション。
     *
     * 文字列全体を左から右へ見て、
     * その位置に応じた色を計算する。
     */
    public static Component create(String text) {

        MutableComponent result = Component.empty();

        if (text.isEmpty()) {
            return result;
        }

        int length = text.codePointCount(0, text.length());
        int index = 0;

        for (int offset = 0; offset < text.length();) {

            int codePoint = text.codePointAt(offset);
            int charCount = Character.charCount(codePoint);

            float progress;

            if (length <= 1) {
                progress = 0.0F;
            } else {
                progress = (float) index / (float) (length - 1);
            }

            int color = interpolate(
                    START_COLOR,
                    END_COLOR,
                    progress
            );

            String character =
                    new String(Character.toChars(codePoint));

            result.append(
                    Component.literal(character)
                            .setStyle(
                                    Style.EMPTY.withColor(
                                            TextColor.fromRgb(color)
                                    )
                            )
            );

            offset += charCount;
            index++;
        }

        return result;
    }

    private static int interpolate(
            int start,
            int end,
            float progress
    ) {
        progress = Math.max(
                0.0F,
                Math.min(1.0F, progress)
        );

        int sr = (start >> 16) & 0xFF;
        int sg = (start >> 8) & 0xFF;
        int sb = start & 0xFF;

        int er = (end >> 16) & 0xFF;
        int eg = (end >> 8) & 0xFF;
        int eb = end & 0xFF;

        int r = Math.round(sr + (er - sr) * progress);
        int g = Math.round(sg + (eg - sg) * progress);
        int b = Math.round(sb + (eb - sb) * progress);

        return (r << 16) | (g << 8) | b;
    }
}