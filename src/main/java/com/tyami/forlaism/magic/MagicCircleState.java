package com.tyami.forlaism.magic;

import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class MagicCircleState {

    public static final float DISAPPEAR_TIME = 7.8F;
    private static final float APPEAR_TIME = 0.3F;

    private static MagicCircleState active;

    private final Vec3 origin;
    private final Vec3 forward;
    private final Vec3 right;
    private final Vec3 up;
    private final long startTick;

    private final List<Node> nodes =
            new ArrayList<>();

    private boolean pigRequested;

    private MagicCircleState(
            Vec3 origin,
            Vec3 forward,
            long startTick
    ) {
        this.origin = origin;
        this.forward = forward.normalize();

        this.right =
                this.forward
                        .cross(new Vec3(0, 1, 0))
                        .normalize();

        this.up =
                this.right
                        .cross(this.forward)
                        .normalize();

        this.startTick = startTick;

        createNodes();
    }

    public static void summon(
            Vec3 origin,
            Vec3 forward,
            long startTick
    ) {
        active =
                new MagicCircleState(
                        origin,
                        forward,
                        startTick
                );
    }

    public static MagicCircleState getActive() {
        return active;
    }

    public static void clear() {
        active = null;
    }

    public Vec3 getOrigin() {
        return origin;
    }

    public Vec3 getForward() {
        return forward;
    }

    public Vec3 getRight() {
        return right;
    }

    public Vec3 getUp() {
        return up;
    }

    public float time(long gameTime, float partialTick) {
        return (gameTime - startTick)
                + partialTick;
    }

    private void createNodes() {
        /*
         * 0.00秒
         * 中央
         */
        nodes.add(
                new Node(
                        origin,
                        MagicCircle.Shape.HEXAGRAM,
                        1.0F,
                        0.0F
                )
        );

        /*
         * 0.80秒
         * 右上
         */
        nodes.add(
                new Node(
                        relative(
                                2.0,
                                2.0,
                                0.0
                        ),
                        MagicCircle.Shape.PENTAGRAM,
                        0.8F,
                        0.8F
                )
        );

        /*
         * 1.50秒
         * 左上
         */
        nodes.add(
                new Node(
                        relative(
                                -2.0,
                                2.0,
                                0.0
                        ),
                        MagicCircle.Shape.DIAMOND_SQUARE,
                        0.8F,
                        1.5F
                )
        );

        /*
         * 2.10秒
         * 右下
         */
        nodes.add(
                new Node(
                        relative(
                                2.0,
                                -2.0,
                                0.0
                        ),
                        MagicCircle.Shape.TRIANGLE,
                        0.8F,
                        2.1F
                )
        );

        /*
         * 2.60秒
         * 左下
         */
        nodes.add(
                new Node(
                        relative(
                                -2.0,
                                -2.0,
                                0.0
                        ),
                        MagicCircle.Shape.LINE,
                        0.8F,
                        2.6F
                )
        );

        /*
         * 3.40秒
         * 奥7m
         */
        nodes.add(
                new Node(
                        origin.add(
                                forward.scale(2.0)
                        ),
                        MagicCircle.Shape.OCTAGRAM,
                        3.0F,
                        3.4F
                )
        );
    }

    private Vec3 relative(
            double horizontal,
            double vertical,
            double depth
    ) {
        return origin
                .add(right.scale(horizontal))
                .add(up.scale(vertical))
                .add(forward.scale(depth));
    }

    public List<Node> nodes() {
        return Collections.unmodifiableList(nodes);
    }

    /*
     * 大魔法陣の位置。
     */
    public Vec3 largeCirclePosition() {
        return origin.add(
                forward.scale(2.0)
        );
    }

    /*
     * 大魔法陣からさらに1m奥。
     */
    public Vec3 pigPosition() {
        return largeCirclePosition()
                .add(forward.scale(1.0));
    }

    /*
     * 接続線の開始時刻。
     */
    public float connectionStart(int index) {
        return switch (index) {
            // 左上
            case 0 -> 3.8F;
            // 右上
            case 1 -> 4.1F;
            // 左下
            case 2 -> 4.3F;
            // 右下
            case 3 -> 4.4F;
            default -> Float.MAX_VALUE;
        };
    }

    /*
     * 中央高速回転開始
     */
    public float centralFastRotationStart() {
        return 4.9F;
    }

    /*
     * 大魔法陣逆回転開始
     */
    public float largeReverseRotationStart() {
        return 5.9F;
    }

    /*
     * 合体開始
     */
    public float fusionStart() {
        return 6.2F;
    }

    /*
     * 中央・周辺魔法陣の収束完了
     */
    public float fusionCollapseEnd() {
        return 6.6F;
    }

    /*
     * 大魔法陣との合体完了
     */
    public float fusionEnd() {
        return 6.9F;
    }

    /*
     * 消滅開始
     */
    public float disappearanceStart() {
        return 7.0F;
    }

    /*
     * 豚召喚は合体完了時。
     */
    public boolean shouldSpawnPig(float time) {
        if (pigRequested) {
            return false;
        }
        if (time >= fusionEnd()) {
            pigRequested = true;
            return true;
        }
        return false;
    }

    public static final class Node {
        public final Vec3 position;
        public final MagicCircle.Shape shape;
        public final float radius;
        public final float delay;

        private Node(
                Vec3 position,
                MagicCircle.Shape shape,
                float radius,
                float delay
        ) {
            this.position = position;
            this.shape = shape;
            this.radius = radius;
            this.delay = delay;
        }
    }
}