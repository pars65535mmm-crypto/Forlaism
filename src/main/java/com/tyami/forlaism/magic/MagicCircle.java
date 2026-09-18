package com.tyami.forlaism.magic;

import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.PoseStack;
import org.joml.Matrix4f;

public final class MagicCircle {

    private MagicCircle() {
    }

    public enum Shape {
        HEXAGRAM,
        PENTAGRAM,
        SQUARE,
        DIAMOND_SQUARE,
        TRIANGLE,
        LINE,
        OCTAGRAM
    }

    private static final int CIRCLE_SEGMENTS = 96;

    /**
     * 外周円
     */
    public static void circle(
            BufferBuilder buffer,
            Matrix4f matrix,
            float radius,
            float alpha
    ) {
        for (int i = 0; i < CIRCLE_SEGMENTS; i++) {
            double a1 =
                    Math.PI * 2.0 * i / CIRCLE_SEGMENTS;
            double a2 =
                    Math.PI * 2.0 * (i + 1) / CIRCLE_SEGMENTS;

            vertex(
                    buffer,
                    matrix,
                    (float) Math.cos(a1) * radius,
                    0.0F,
                    (float) Math.sin(a1) * radius,
                    0.25F,
                    0.85F,
                    1.0F,
                    alpha
            );
            vertex(
                    buffer,
                    matrix,
                    (float) Math.cos(a2) * radius,
                    0.0F,
                    (float) Math.sin(a2) * radius,
                    0.25F,
                    0.85F,
                    1.0F,
                    alpha
            );
        }
    }

    /**
     * 魔法陣本体。
     *
     * reveal:
     * 0.0 = 外周だけ
     * 1.0 = 内部完全描画
     */
    public static void draw(
            BufferBuilder buffer,
            PoseStack poseStack,
            Shape shape,
            float radius,
            float reveal,
            float alpha
    ) {
        Matrix4f matrix =
                poseStack.last().pose();

        circle(
                buffer,
                matrix,
                radius,
                alpha
        );

        if (reveal <= 0.0F) {
            return;
        }

        reveal =
                Math.max(
                        0.0F,
                        Math.min(1.0F, reveal)
                );

        float inner =
                radius * 0.72F;

        switch (shape) {
            case HEXAGRAM -> {
                triangle(
                        buffer,
                        matrix,
                        inner,
                        0.0F,
                        reveal
                );
                triangle(
                        buffer,
                        matrix,
                        inner,
                        (float) Math.PI,
                        reveal
                );
            }
            case PENTAGRAM -> {
                star(
                        buffer,
                        matrix,
                        inner,
                        5,
                        reveal
                );
            }
            case SQUARE -> {
                polygon(
                        buffer,
                        matrix,
                        inner,
                        4,
                        0.0F,
                        reveal
                );
            }
            case DIAMOND_SQUARE -> {
                polygon(
                        buffer,
                        matrix,
                        inner,
                        4,
                        (float) Math.PI / 4.0F,
                        reveal
                );
            }
            case TRIANGLE -> {
                triangle(
                        buffer,
                        matrix,
                        inner,
                        0.0F,
                        reveal
                );
            }
            case LINE -> {
                line(
                        buffer,
                        matrix,
                        inner,
                        reveal
                );
            }
            case OCTAGRAM -> {
                star(
                        buffer,
                        matrix,
                        inner,
                        8,
                        reveal
                );
                polygon(
                        buffer,
                        matrix,
                        inner * 0.60F,
                        8,
                        (float) Math.PI / 8.0F,
                        reveal
                );
            }
        }
    }

    private static void triangle(
            BufferBuilder buffer,
            Matrix4f matrix,
            float radius,
            float rotation,
            float reveal
    ) {
        polygon(
                buffer,
                matrix,
                radius,
                3,
                rotation,
                reveal
        );
    }

    private static void polygon(
            BufferBuilder buffer,
            Matrix4f matrix,
            float radius,
            int sides,
            float rotation,
            float reveal
    ) {
        int count =
                Math.max(
                        1,
                        (int) Math.ceil(
                                sides * reveal
                        )
                );

        for (int i = 0; i < count; i++) {
            double a1 =
                    rotation
                            + Math.PI * 2.0 * i / sides;
            double a2 =
                    rotation
                            + Math.PI * 2.0
                            * (i + 1)
                            / sides;

            vertex(
                    buffer,
                    matrix,
                    (float) Math.cos(a1) * radius,
                    0,
                    (float) Math.sin(a1) * radius,
                    0.25F,
                    0.85F,
                    1.0F,
                    1.0F
            );
            vertex(
                    buffer,
                    matrix,
                    (float) Math.cos(a2) * radius,
                    0,
                    (float) Math.sin(a2) * radius,
                    0.25F,
                    0.85F,
                    1.0F,
                    1.0F
            );
        }
    }

    private static void star(
            BufferBuilder buffer,
            Matrix4f matrix,
            float radius,
            int points,
            float reveal
    ) {
        int count =
                Math.max(
                        1,
                        (int) Math.ceil(
                                points * reveal
                        )
                );

        /*
         * 外周多角形
         */
        polygon(
                buffer,
                matrix,
                radius,
                points,
                -((float) Math.PI / 2.0F),
                reveal
        );

        /*
         * 星の交差線
         */
        for (int i = 0; i < count; i++) {
            double a1 =
                    -Math.PI / 2.0
                            + Math.PI * 2.0
                            * i / points;
            double a2 =
                    -Math.PI / 2.0
                            + Math.PI * 2.0
                            * ((i + 2) % points)
                            / points;

            vertex(
                    buffer,
                    matrix,
                    (float) Math.cos(a1) * radius,
                    0,
                    (float) Math.sin(a1) * radius,
                    1.0F,
                    0.85F,
                    0.25F,
                    reveal
            );
            vertex(
                    buffer,
                    matrix,
                    (float) Math.cos(a2) * radius,
                    0,
                    (float) Math.sin(a2) * radius,
                    1.0F,
                    0.85F,
                    0.25F,
                    reveal
            );
        }
    }

    private static void line(
            BufferBuilder buffer,
            Matrix4f matrix,
            float radius,
            float reveal
    ) {
        float x =
                radius * reveal;

        vertex(
                buffer,
                matrix,
                -x,
                0,
                0,
                1,
                1,
                1,
                reveal
        );
        vertex(
                buffer,
                matrix,
                x,
                0,
                0,
                1,
                1,
                1,
                reveal
        );
    }

    private static void vertex(
            BufferBuilder buffer,
            Matrix4f matrix,
            float x,
            float y,
            float z,
            float r,
            float g,
            float b,
            float a
    ) {
        buffer.vertex(matrix, x, y, z)
                .color(r, g, b, a)
                .endVertex();
    }
}