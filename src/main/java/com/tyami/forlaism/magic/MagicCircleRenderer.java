package com.tyami.forlaism.magic;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;

import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.world.phys.Vec3;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import org.joml.Matrix4f;
import org.joml.Quaternionf;

@Mod.EventBusSubscriber(
        modid = "forlaism",
        bus = Mod.EventBusSubscriber.Bus.FORGE,
        value = Dist.CLIENT
)
public final class MagicCircleRenderer {

    private MagicCircleRenderer() {
    }

    @SubscribeEvent
    public static void render(
            RenderLevelStageEvent event
    ) {
        
        if (
                event.getStage()
                        != RenderLevelStageEvent.Stage.AFTER_PARTICLES
        ) {
            return;
        }

        MagicCircleState state =
                MagicCircleState.getActive();

        if (state == null) {
            return;
        }

        Minecraft mc =
                Minecraft.getInstance();

        if (mc.level == null) {
            return;
        }

        Camera camera =
                event.getCamera();

        Vec3 cameraPosition =
                camera.getPosition();

        float time =
                state.time(
                        mc.level.getGameTime(),
                        event.getPartialTick()
                );

        /*
         * 7.8秒で完全終了。
         */
        if (time >= MagicCircleState.DISAPPEAR_TIME) {
            MagicCircleState.clear();
            return;
        }

        PoseStack pose =
                event.getPoseStack();

        pose.pushPose();

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableDepthTest();
        RenderSystem.lineWidth(2.0F);
        RenderSystem.setShader(
                GameRenderer::getPositionColorShader
        );

        Tesselator tesselator =
                Tesselator.getInstance();

        BufferBuilder buffer =
                tesselator.getBuilder();

        buffer.begin(
                VertexFormat.Mode.LINES,
                DefaultVertexFormat.POSITION_COLOR
        );

        /*
         * 全魔法陣
         */
        for (
                MagicCircleState.Node node
                : state.nodes()
        ) {
            float local =
                    time - node.delay;

            if (local < 0.0F) {
                continue;
            }

            renderNode(
                    buffer,
                    pose,
                    cameraPosition,
                    state,
                    node,
                    local,
                    time
            );
        }

        /*
         * 接続線
         */
        renderConnections(
                buffer,
                pose,
                cameraPosition,
                state,
                time
        );

        /*
         * 魔法陣の収束・合体
         */
        renderFusion(
                buffer,
                pose,
                cameraPosition,
                state,
                time
        );

        tesselator.end();

        RenderSystem.lineWidth(1.0F);
        RenderSystem.enableDepthTest();
        RenderSystem.disableBlend();

        pose.popPose();
    }

    private static void renderNode(
            BufferBuilder buffer,
            PoseStack pose,
            Vec3 cameraPosition,
            MagicCircleState state,
            MagicCircleState.Node node,
            float localTime,
            float totalTime
    ) {
        /*
         * ========================================================
         * 出現
         * ========================================================
         */
        float appear =
                smooth(
                        localTime / 0.3F
                );

        /*
         * 最初は「○」
         *
         * revealは0から開始。
         */
        float reveal =
                smooth(
                        (localTime - 0.02F)
                                / 0.28F
                );

        /*
         * ========================================================
         * 消滅
         *
         * 7.0 → 7.8
         *
         * 内部 → 円 → 縮小
         * ========================================================
         */
        float disappear =
                1.0F;

        if (totalTime >= 7.0F) {
            disappear =
                    1.0F -
                            smooth(
                                    (totalTime - 7.0F)
                                            / 0.8F
                            );
        }

        /*
         * 外周まで消える割合。
         */
        float vanishRadius =
                totalTime >= 7.0F
                        ? 1.0F - disappear
                        : 0.0F;

        float radius =
                node.radius
                        * appear
                        * (
                                1.0F
                                        - vanishRadius
                        );

        if (radius <= 0.001F) {
            return;
        }

        /*
         * 内部を先に消す。
         */
        float internalAlpha;
        if (totalTime >= 7.0F) {
            internalAlpha =
                    1.0F -
                            smooth(
                                    (totalTime - 7.0F)
                                            / 0.45F
                            );
        } else {
            internalAlpha = 1.0F;
        }

        /*
         * 外周は内部より遅く消える。
         */
        float ringAlpha;
        if (totalTime >= 7.45F) {
            ringAlpha =
                    1.0F -
                            smooth(
                                    (totalTime - 7.45F)
                                            / 0.35F
                            );
        } else {
            ringAlpha = 1.0F;
        }

        /*
         * ========================================================
         * 回転
         * ========================================================
         */
        float rotation =
                totalTime
                        * ((float) Math.PI / 2.0F);

        /*
         * 中央高速回転
         */
        if (
                node.shape
                        == MagicCircle.Shape.HEXAGRAM
                        &&
                        totalTime >= 4.9F
        ) {
            float t =
                    totalTime - 4.9F;

            rotation =
                    4.9F
                            * ((float) Math.PI / 2.0F)
                            + t * (float) Math.PI * 8.0F;
        }

        /*
         * 大魔法陣逆回転
         */
        if (
                node.shape
                        == MagicCircle.Shape.OCTAGRAM
                        &&
                        totalTime >= 5.9F
        ) {
            float t =
                    totalTime - 5.9F;

            rotation =
                    5.9F
                            * ((float) Math.PI / 2.0F)
                            - t * (float) Math.PI * 4.0F;
        }

        /*
         * ========================================================
         * 合体
         * ========================================================
         */
        Vec3 position =
                node.position;

        if (
                totalTime >= 6.2F
                        &&
                        totalTime < 6.9F
        ) {
            float p;

            if (totalTime < 6.6F) {
                p =
                        smooth(
                                (totalTime - 6.2F)
                                        / 0.4F
                        );

                position =
                        position.lerp(
                                state.getOrigin(),
                                p
                        );
            } else {
                p =
                        smooth(
                                (totalTime - 6.6F)
                                        / 0.3F
                        );

                position =
                        state.getOrigin()
                                .lerp(
                                        state.largeCirclePosition(),
                                        p
                                );
            }
        }

        Vec3 relative =
                position.subtract(
                        cameraPosition
                );

        pose.pushPose();

        pose.translate(
                relative.x,
                relative.y,
                relative.z
        );

        /*
         * 魔法陣面を視線面に固定。
         */
        pose.mulPose(
                new Quaternionf()
                        .rotationTo(
                                new org.joml.Vector3f(
                                        0,
                                        1,
                                        0
                                ),
                                new org.joml.Vector3f(
                                        (float) state.getForward().x,
                                        (float) state.getForward().y,
                                        (float) state.getForward().z
                                )
                        )
        );

        /*
         * Z軸回転。
         */
        pose.mulPose(
                new Quaternionf()
                        .rotateY(rotation)
        );

        Matrix4f matrix =
                pose.last().pose();

        /*
         * 外周。
         */
        MagicCircle.circle(
                buffer,
                matrix,
                radius,
                ringAlpha
        );

        /*
         * 内部。
         */
        MagicCircle.draw(
                buffer,
                pose,
                node.shape,
                radius,
                reveal * internalAlpha,
                internalAlpha
        );

        pose.popPose();
    }

    private static void renderConnections(
            BufferBuilder buffer,
            PoseStack pose,
            Vec3 cameraPosition,
            MagicCircleState state,
            float time
    ) {
        /*
         * 対応：
         *
         * 0 = 左上
         * 1 = 右上
         * 2 = 左下
         * 3 = 右下
         */
        int[] nodeIndices = {
                2,
                1,
                4,
                3
        };

        for (int i = 0; i < 4; i++) {
            float start =
                    state.connectionStart(i);

            if (time < start) {
                continue;
            }

            float progress =
                    smooth(
                            (time - start)
                                    / 0.3F
                    );

            Vec3 from =
                    state.nodes()
                            .get(
                                    nodeIndices[i]
                            )
                            .position;

            Vec3 to =
                    state.getOrigin();

            Vec3 current =
                    from.lerp(
                            to,
                            progress
                    );

            Vec3 a =
                    from.subtract(
                            cameraPosition
                    );
            Vec3 b =
                    current.subtract(
                            cameraPosition
                    );

            Matrix4f matrix =
                    pose.last().pose();

            vertex(
                    buffer,
                    matrix,
                    a,
                    1,
                    1,
                    1,
                    1
            );
            vertex(
                    buffer,
                    matrix,
                    b,
                    1,
                    1,
                    1,
                    1
            );
        }
    }

    private static void renderFusion(
            BufferBuilder buffer,
            PoseStack pose,
            Vec3 cameraPosition,
            MagicCircleState state,
            float time
    ) {
        if (time < 6.2F) {
            return;
        }

        Vec3 center =
                state.getOrigin();
        Vec3 large =
                state.largeCirclePosition();

        /*
         * 中央 → 大魔法陣の融合光線。
         */
        float progress;

        if (time < 6.6F) {
            progress =
                    smooth(
                            (time - 6.2F)
                                    / 0.4F
                    );
        } else {
            progress =
                    smooth(
                            (time - 6.6F)
                                    / 0.3F
                    );
        }

        Vec3 current =
                center.lerp(
                        large,
                        progress
                );

        Vec3 a =
                center.subtract(
                        cameraPosition
                );
        Vec3 b =
                current.subtract(
                        cameraPosition
                );

        Matrix4f matrix =
                pose.last().pose();

        vertex(
                buffer,
                matrix,
                a,
                1,
                1,
                1,
                1
        );
        vertex(
                buffer,
                matrix,
                b,
                1,
                1,
                1,
                1
        );
    }

    private static void vertex(
            BufferBuilder buffer,
            Matrix4f matrix,
            Vec3 position,
            float r,
            float g,
            float b,
            float a
    ) {
        buffer.vertex(
                        matrix,
                        (float) position.x,
                        (float) position.y,
                        (float) position.z
                )
                .color(
                        r,
                        g,
                        b,
                        a
                )
                .endVertex();
    }

    private static float smooth(
            float x
    ) {
        x =
                Math.max(
                        0.0F,
                        Math.min(1.0F, x)
                );

        return x * x * (3.0F - 2.0F * x);
    }
}