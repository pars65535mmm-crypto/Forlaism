package com.tyami.forlaism.client.gmb;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.world.item.ItemStack;
import org.joml.Matrix4f;
import org.joml.Quaternionf;

/**
 * GMB のワールド描画。
 *
 * アイテムの周囲に虹色グリッチのクアッドを貼る。
 */
public final class GMBRenderer {

    private static final long START_TIME = System.nanoTime();

    private GMBRenderer() {
    }

    public static float elapsedSeconds() {
        return (System.nanoTime() - START_TIME) / 1_000_000_000.0f;
    }

    public static void render(
            ItemStack stack,
            PoseStack poseStack,
            MultiBufferSource buffer,
            boolean billboard
    ) {
        ShaderInstance shader = GMBShader.getShader();
        if (shader == null) return;

        float time = elapsedSeconds();
        float alpha = 1.0f;

        poseStack.pushPose();

        if (billboard) {
            Quaternionf cam = Minecraft.getInstance()
                    .getEntityRenderDispatcher()
                    .cameraOrientation();
            poseStack.mulPose(cam);
        }

        // アイテムより少し大きめ
        float size = 0.85f;

        RenderSystem.enableBlend();
        RenderSystem.blendFunc(
                com.mojang.blaze3d.platform.GlStateManager.SourceFactor.SRC_ALPHA,
                com.mojang.blaze3d.platform.GlStateManager.DestFactor.ONE
        );
        RenderSystem.depthMask(false);
        RenderSystem.disableCull();

        RenderSystem.setShader(() -> shader);
        GMBShader.applyUniforms(time, alpha);

        Matrix4f matrix = poseStack.last().pose();

        BufferBuilder bb = Tesselator.getInstance().getBuilder();
        bb.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR_TEX);

        bb.vertex(matrix, -size, -size, 0).color(1f, 1f, 1f, 1f).uv(0f, 1f).endVertex();
        bb.vertex(matrix,  size, -size, 0).color(1f, 1f, 1f, 1f).uv(1f, 1f).endVertex();
        bb.vertex(matrix,  size,  size, 0).color(1f, 1f, 1f, 1f).uv(1f, 0f).endVertex();
        bb.vertex(matrix, -size,  size, 0).color(1f, 1f, 1f, 1f).uv(0f, 0f).endVertex();

        BufferUploader.drawWithShader(bb.end());

        RenderSystem.enableCull();
        RenderSystem.depthMask(true);
        RenderSystem.disableBlend();

        poseStack.popPose();
    }
}