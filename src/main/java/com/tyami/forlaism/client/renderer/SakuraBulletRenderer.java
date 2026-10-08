package com.tyami.forlaism.client.renderer;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.tyami.forlaism.entity.SakuraBulletEntity;

import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.InventoryMenu;

import org.joml.Matrix4f;
import org.joml.Quaternionf;

/**
 * 桜の弾丸のレンダラ。
 *
 * - 中心の白熱コア
 * - 周囲の桜色のオーラ
 * - 進行方向に伸びる尾
 * - 加算合成で発光
 */
public class SakuraBulletRenderer extends EntityRenderer<SakuraBulletEntity> {

    public SakuraBulletRenderer(EntityRendererProvider.Context ctx) {
        super(ctx);
    }

    @Override
    public ResourceLocation getTextureLocation(SakuraBulletEntity entity) {
        return InventoryMenu.BLOCK_ATLAS;
    }

    @Override
    public void render(
            SakuraBulletEntity entity,
            float entityYaw,
            float partialTick,
            PoseStack poseStack,
            MultiBufferSource buffer,
            int packedLight
    ) {
        super.render(entity, entityYaw, partialTick, poseStack, buffer, packedLight);

        // 発光させる
        RenderSystem.enableBlend();
        RenderSystem.blendFunc(
                com.mojang.blaze3d.platform.GlStateManager.SourceFactor.SRC_ALPHA,
                com.mojang.blaze3d.platform.GlStateManager.DestFactor.ONE
        );
        RenderSystem.depthMask(false);
        RenderSystem.disableCull();
        RenderSystem.setShader(GameRenderer::getPositionColorShader);

        poseStack.pushPose();

        // カメラを向くビルボード
        Quaternionf cam = this.entityRenderDispatcher.cameraOrientation();
        poseStack.mulPose(cam);

        Matrix4f matrix = poseStack.last().pose();
        Tesselator tess = Tesselator.getInstance();
        BufferBuilder bb = tess.getBuilder();

        // =====================================================
        // 1. 外周の桜色オーラ（大きめ、半透明）
        // =====================================================
        bb.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
        addGlowQuad(bb, matrix,
                0.55F,          // サイズ
                1.0F, 0.6F, 0.8F,   // 桜色
                0.55F           // 中心アルファ
        );
        BufferUploader.drawWithShader(bb.end());

        // =====================================================
        // 2. 中間層（ピンク〜白）
        // =====================================================
        bb.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
        addGlowQuad(bb, matrix,
                0.32F,
                1.0F, 0.85F, 0.95F,
                0.85F
        );
        BufferUploader.drawWithShader(bb.end());

        // =====================================================
        // 3. 白熱コア（極小・純白）
        // =====================================================
        bb.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
        addGlowQuad(bb, matrix,
                0.14F,
                1.0F, 1.0F, 1.0F,
                1.0F
        );
        BufferUploader.drawWithShader(bb.end());

        // =====================================================
        // 4. クロス（十字の光条）
        // =====================================================
        bb.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
        // 縦
        bb.vertex(matrix, -0.02F, -0.9F, 0).color(1.0F, 0.85F, 1.0F, 0.0F).endVertex();
        bb.vertex(matrix,  0.02F, -0.9F, 0).color(1.0F, 0.85F, 1.0F, 0.0F).endVertex();
        bb.vertex(matrix,  0.02F,  0.9F, 0).color(1.0F, 0.85F, 1.0F, 0.0F).endVertex();
        bb.vertex(matrix, -0.02F,  0.9F, 0).color(1.0F, 0.85F, 1.0F, 0.0F).endVertex();
        // 横
        bb.vertex(matrix, -0.9F, -0.02F, 0).color(1.0F, 0.85F, 1.0F, 0.0F).endVertex();
        bb.vertex(matrix,  0.9F, -0.02F, 0).color(1.0F, 0.85F, 1.0F, 0.0F).endVertex();
        bb.vertex(matrix,  0.9F,  0.02F, 0).color(1.0F, 0.85F, 1.0F, 0.0F).endVertex();
        bb.vertex(matrix, -0.9F,  0.02F, 0).color(1.0F, 0.85F, 1.0F, 0.0F).endVertex();
        BufferUploader.drawWithShader(bb.end());

        poseStack.popPose();

        RenderSystem.depthMask(true);
        RenderSystem.enableCull();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableBlend();
    }

    /**
     * 中心が明るく、外側に向かって透明になる放射グラデのクアッド。
     */
    private static void addGlowQuad(
            BufferBuilder bb,
            Matrix4f m,
            float size,
            float r, float g, float b,
            float alpha
    ) {
        float x0 = -size;
        float x1 =  size;
        float y0 = -size;
        float y1 =  size;

        // 四隅（外側、透明）
        bb.vertex(m, x0, y1, 0).color(r, g, b, 0.0F).endVertex();
        bb.vertex(m, x1, y1, 0).color(r, g, b, 0.0F).endVertex();
        bb.vertex(m, x1, y0, 0).color(r, g, b, 0.0F).endVertex();
        bb.vertex(m, x0, y0, 0).color(r, g, b, 0.0F).endVertex();

        // 中心から4枚の扇で中心の明るい頂点を作る
        float cx = 0.0F;
        float cy = 0.0F;

        // 上
        bb.vertex(m, x0, y1, 0).color(r, g, b, 0.0F).endVertex();
        bb.vertex(m, x1, y1, 0).color(r, g, b, 0.0F).endVertex();
        bb.vertex(m, cx, cy, 0).color(r, g, b, alpha).endVertex();
        bb.vertex(m, cx, cy, 0).color(r, g, b, alpha).endVertex();

        // 右
        bb.vertex(m, x1, y1, 0).color(r, g, b, 0.0F).endVertex();
        bb.vertex(m, x1, y0, 0).color(r, g, b, 0.0F).endVertex();
        bb.vertex(m, cx, cy, 0).color(r, g, b, alpha).endVertex();
        bb.vertex(m, cx, cy, 0).color(r, g, b, alpha).endVertex();

        // 下
        bb.vertex(m, x1, y0, 0).color(r, g, b, 0.0F).endVertex();
        bb.vertex(m, x0, y0, 0).color(r, g, b, 0.0F).endVertex();
        bb.vertex(m, cx, cy, 0).color(r, g, b, alpha).endVertex();
        bb.vertex(m, cx, cy, 0).color(r, g, b, alpha).endVertex();

        // 左
        bb.vertex(m, x0, y0, 0).color(r, g, b, 0.0F).endVertex();
        bb.vertex(m, x0, y1, 0).color(r, g, b, 0.0F).endVertex();
        bb.vertex(m, cx, cy, 0).color(r, g, b, alpha).endVertex();
        bb.vertex(m, cx, cy, 0).color(r, g, b, alpha).endVertex();
    }
}