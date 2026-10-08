package com.tyami.forlaism.client.renderer;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.tyami.forlaism.entity.MeteorEntity;

import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.InventoryMenu;

import org.joml.Matrix4f;
import org.joml.Quaternionf;

/**
 * 隕石のレンダラ。
 *
 * モデルは使わず、Javaで発光する球体を描画する。
 * 中心の白熱球 + 周囲の炎 + グロー
 */
public class MeteorRenderer extends EntityRenderer<MeteorEntity> {

    public MeteorRenderer(EntityRendererProvider.Context ctx) {
        super(ctx);
    }

    @Override
    public ResourceLocation getTextureLocation(MeteorEntity entity) {
        return InventoryMenu.BLOCK_ATLAS;
    }

    @Override
    public void render(
            MeteorEntity entity,
            float entityYaw,
            float partialTick,
            PoseStack poseStack,
            MultiBufferSource buffer,
            int packedLight
    ) {
        super.render(entity, entityYaw, partialTick, poseStack, buffer, packedLight);

        // 加算合成で発光
        RenderSystem.enableBlend();
        RenderSystem.blendFunc(
                com.mojang.blaze3d.platform.GlStateManager.SourceFactor.SRC_ALPHA,
                com.mojang.blaze3d.platform.GlStateManager.DestFactor.ONE
        );
        RenderSystem.depthMask(false);
        RenderSystem.disableCull();
        RenderSystem.setShader(GameRenderer::getPositionColorShader);

        poseStack.pushPose();

        // カメラを向く
        Quaternionf cam = this.entityRenderDispatcher.cameraOrientation();
        poseStack.mulPose(cam);

        Matrix4f matrix = poseStack.last().pose();
        Tesselator tess = Tesselator.getInstance();
        BufferBuilder bb = tess.getBuilder();

        float time = (entity.tickCount + partialTick) * 0.1F;

        // =====================================================
        // 1. 外側の炎（大きく、半透明の橙）
        // =====================================================
        float outerSize = 4.0F + (float) Math.sin(time * 2) * 0.3F;
        bb.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
        addGlowQuad(bb, matrix,
                outerSize,
                1.0F, 0.4F, 0.1F,  // 橙
                0.4F
        );
        BufferUploader.drawWithShader(bb.end());

        // =====================================================
        // 2. 中間層（明るいオレンジ〜黄）
        // =====================================================
        float midSize = 2.5F;
        bb.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
        addGlowQuad(bb, matrix,
                midSize,
                1.0F, 0.8F, 0.2F,  // 黄橙
                0.7F
        );
        BufferUploader.drawWithShader(bb.end());

        // =====================================================
        // 3. 中心の白熱コア
        // =====================================================
        float coreSize = 1.2F;
        bb.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
        addGlowQuad(bb, matrix,
                coreSize,
                1.0F, 1.0F, 1.0F,  // 白
                1.0F
        );
        BufferUploader.drawWithShader(bb.end());

        // =====================================================
        // 4. 十字の光条
        // =====================================================
        bb.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
        // 縦
        bb.vertex(matrix, -0.05F, -6.0F, 0).color(1F, 0.8F, 0.4F, 0.0F).endVertex();
        bb.vertex(matrix,  0.05F, -6.0F, 0).color(1F, 0.8F, 0.4F, 0.0F).endVertex();
        bb.vertex(matrix,  0.05F,  6.0F, 0).color(1F, 0.8F, 0.4F, 0.0F).endVertex();
        bb.vertex(matrix, -0.05F,  6.0F, 0).color(1F, 0.8F, 0.4F, 0.0F).endVertex();
        // 横
        bb.vertex(matrix, -6.0F, -0.05F, 0).color(1F, 0.8F, 0.4F, 0.0F).endVertex();
        bb.vertex(matrix,  6.0F, -0.05F, 0).color(1F, 0.8F, 0.4F, 0.0F).endVertex();
        bb.vertex(matrix,  6.0F,  0.05F, 0).color(1F, 0.8F, 0.4F, 0.0F).endVertex();
        bb.vertex(matrix, -6.0F,  0.05F, 0).color(1F, 0.8F, 0.4F, 0.0F).endVertex();
        BufferUploader.drawWithShader(bb.end());

        poseStack.popPose();

        RenderSystem.depthMask(true);
        RenderSystem.enableCull();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableBlend();
    }

    /**
     * 中心が明るく、外側が透明になる放射グラデーションのクアッド。
     */
    private static void addGlowQuad(
            BufferBuilder bb,
            Matrix4f m,
            float size,
            float r, float g, float b,
            float alpha
    ) {
        float x0 = -size;
        float x1 = size;
        float y0 = -size;
        float y1 = size;

        // 四隅（透明）
        bb.vertex(m, x0, y1, 0).color(r, g, b, 0.0F).endVertex();
        bb.vertex(m, x1, y1, 0).color(r, g, b, 0.0F).endVertex();
        bb.vertex(m, x1, y0, 0).color(r, g, b, 0.0F).endVertex();
        bb.vertex(m, x0, y0, 0).color(r, g, b, 0.0F).endVertex();

        // 中心から4枚の扇
        float cx = 0.0F;
        float cy = 0.0F;

        bb.vertex(m, x0, y1, 0).color(r, g, b, 0.0F).endVertex();
        bb.vertex(m, x1, y1, 0).color(r, g, b, 0.0F).endVertex();
        bb.vertex(m, cx, cy, 0).color(r, g, b, alpha).endVertex();
        bb.vertex(m, cx, cy, 0).color(r, g, b, alpha).endVertex();

        bb.vertex(m, x1, y1, 0).color(r, g, b, 0.0F).endVertex();
        bb.vertex(m, x1, y0, 0).color(r, g, b, 0.0F).endVertex();
        bb.vertex(m, cx, cy, 0).color(r, g, b, alpha).endVertex();
        bb.vertex(m, cx, cy, 0).color(r, g, b, alpha).endVertex();

        bb.vertex(m, x1, y0, 0).color(r, g, b, 0.0F).endVertex();
        bb.vertex(m, x0, y0, 0).color(r, g, b, 0.0F).endVertex();
        bb.vertex(m, cx, cy, 0).color(r, g, b, alpha).endVertex();
        bb.vertex(m, cx, cy, 0).color(r, g, b, alpha).endVertex();

        bb.vertex(m, x0, y0, 0).color(r, g, b, 0.0F).endVertex();
        bb.vertex(m, x0, y1, 0).color(r, g, b, 0.0F).endVertex();
        bb.vertex(m, cx, cy, 0).color(r, g, b, alpha).endVertex();
        bb.vertex(m, cx, cy, 0).color(r, g, b, alpha).endVertex();
    }
}