package com.tyami.forlaism.client.magiceffect;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import org.joml.Matrix4f;
import org.joml.Quaternionf;

/**
 * ワールド内（ドロップアイテム、手持ち、エンティティ等）における魔法エフェクト描画を担当するクラス。
 */
public final class MagicEffectWorldRenderer {

    private MagicEffectWorldRenderer() {
    }

    /**
     * 3D空間において、指定されたPoseStackとMultiBufferSourceを用いてアイテムの中心に魔法エフェクトを描画する。
     * カメラに正対するビルボードとして描画され、どの角度から見てもアイテムの周囲に魔力が展開される。
     *
     * @param poseStack ワールド変換行列スタック
     * @param buffer MultiBufferSource
     * @param style 適用するスタイル
     * @param billboard true の場合、カメラに向かってビルボード回転を行う
     */
    public static void render(PoseStack poseStack, MultiBufferSource buffer, MagicEffectStyle style, boolean billboard) {
        if (style == null || !style.isEnabled()) {
            return;
        }

        float time = MagicEffectGuiRenderer.getAnimationTime();
        MagicEffectShader.applyUniforms(time, style);

        RenderType renderType = MagicEffectShader.getWorldRenderType();
        VertexConsumer consumer = buffer.getBuffer(renderType);

        poseStack.pushPose();

        if (billboard) {
            // カメラの回転を反映してビルボード化
            Quaternionf cameraRotation = Minecraft.getInstance().getEntityRenderDispatcher().cameraOrientation();
            poseStack.mulPose(cameraRotation);
        }

        float halfSize = 0.65f * style.getScale();
        Matrix4f pose = poseStack.last().pose();

        // 四角形クアッド（POSITION_COLOR_TEX）
        // アルファ減衰を防ぎ鮮烈な発光を得るため白(1.0f)を渡す
        consumer.vertex(pose, -halfSize, -halfSize, 0.0f).color(1.0f, 1.0f, 1.0f, 1.0f).uv(0.0f, 1.0f).endVertex();
        consumer.vertex(pose,  halfSize, -halfSize, 0.0f).color(1.0f, 1.0f, 1.0f, 1.0f).uv(1.0f, 1.0f).endVertex();
        consumer.vertex(pose,  halfSize,  halfSize, 0.0f).color(1.0f, 1.0f, 1.0f, 1.0f).uv(1.0f, 0.0f).endVertex();
        consumer.vertex(pose, -halfSize,  halfSize, 0.0f).color(1.0f, 1.0f, 1.0f, 1.0f).uv(0.0f, 0.0f).endVertex();

        poseStack.popPose();
    }
}
