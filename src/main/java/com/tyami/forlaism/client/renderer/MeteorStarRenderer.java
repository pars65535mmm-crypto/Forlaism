package com.tyami.forlaism.client.renderer;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.tyami.forlaism.entity.MeteorStarEntity;

import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.InventoryMenu;

import org.joml.Matrix4f;
import org.joml.Quaternionf;

/**
 * 流星のレンダラ。
 *
 * 中心の白熱コア + 星型の光条。
 */
public class MeteorStarRenderer extends EntityRenderer<MeteorStarEntity> {

    public MeteorStarRenderer(EntityRendererProvider.Context ctx) {
        super(ctx);
    }

    @Override
    public ResourceLocation getTextureLocation(MeteorStarEntity entity) {
        return InventoryMenu.BLOCK_ATLAS;
    }

    @Override
    public void render(
            MeteorStarEntity entity,
            float yaw,
            float partialTick,
            PoseStack poseStack,
            MultiBufferSource buffer,
            int light
    ) {
        super.render(entity, yaw, partialTick, poseStack, buffer, light);

        RenderSystem.enableBlend();
        RenderSystem.blendFunc(
                com.mojang.blaze3d.platform.GlStateManager.SourceFactor.SRC_ALPHA,
                com.mojang.blaze3d.platform.GlStateManager.DestFactor.ONE
        );
        RenderSystem.depthMask(false);
        RenderSystem.disableCull();
        RenderSystem.setShader(GameRenderer::getPositionColorShader);

        poseStack.pushPose();

        Quaternionf cam = this.entityRenderDispatcher.cameraOrientation();
        poseStack.mulPose(cam);

        Matrix4f matrix = poseStack.last().pose();
        Tesselator tess = Tesselator.getInstance();
        BufferBuilder bb = tess.getBuilder();

        float time = (entity.tickCount + partialTick) * 0.15F;

        // 外側の光
        bb.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
        addGlowQuad(bb, matrix, 1.2F, 1.0F, 0.9F, 0.4F, 0.35F);
        BufferUploader.drawWithShader(bb.end());

        // 中間
        bb.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
        addGlowQuad(bb, matrix, 0.7F, 1.0F, 1.0F, 0.8F, 0.7F);
        BufferUploader.drawWithShader(bb.end());

        // コア
        bb.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
        addGlowQuad(bb, matrix, 0.35F, 1.0F, 1.0F, 1.0F, 1.0F);
        BufferUploader.drawWithShader(bb.end());

        // 回転する星型光条
        bb.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
        for (int i = 0; i < 4; i++) {
            double angle = time + (i * Math.PI / 2);
            float len = 2.5F;
            float cx = (float) Math.cos(angle) * len;
            float cy = (float) Math.sin(angle) * len;

            bb.vertex(matrix, -cx * 0.08F, -cy * 0.08F, 0).color(1F, 1F, 1F, 0.9F).endVertex();
            bb.vertex(matrix, cx * 0.08F, cy * 0.08F, 0).color(1F, 1F, 1F, 0.9F).endVertex();
            bb.vertex(matrix, cx, cy, 0).color(1F, 1F, 1F, 0F).endVertex();
            bb.vertex(matrix, -cx, -cy, 0).color(1F, 1F, 1F, 0F).endVertex();
        }
        BufferUploader.drawWithShader(bb.end());

        poseStack.popPose();

        RenderSystem.depthMask(true);
        RenderSystem.enableCull();
        RenderSystem.disableBlend();
    }

    private static void addGlowQuad(
            BufferBuilder bb,
            Matrix4f m,
            float size,
            float r, float g, float b,
            float alpha
    ) {
        float x0 = -size, x1 = size;
        float y0 = -size, y1 = size;

        bb.vertex(m, x0, y1, 0).color(r, g, b, 0).endVertex();
        bb.vertex(m, x1, y1, 0).color(r, g, b, 0).endVertex();
        bb.vertex(m, x1, y0, 0).color(r, g, b, 0).endVertex();
        bb.vertex(m, x0, y0, 0).color(r, g, b, 0).endVertex();

        bb.vertex(m, x0, y1, 0).color(r, g, b, 0).endVertex();
        bb.vertex(m, x1, y1, 0).color(r, g, b, 0).endVertex();
        bb.vertex(m, 0, 0, 0).color(r, g, b, alpha).endVertex();
        bb.vertex(m, 0, 0, 0).color(r, g, b, alpha).endVertex();

        bb.vertex(m, x1, y1, 0).color(r, g, b, 0).endVertex();
        bb.vertex(m, x1, y0, 0).color(r, g, b, 0).endVertex();
        bb.vertex(m, 0, 0, 0).color(r, g, b, alpha).endVertex();
        bb.vertex(m, 0, 0, 0).color(r, g, b, alpha).endVertex();

        bb.vertex(m, x1, y0, 0).color(r, g, b, 0).endVertex();
        bb.vertex(m, x0, y0, 0).color(r, g, b, 0).endVertex();
        bb.vertex(m, 0, 0, 0).color(r, g, b, alpha).endVertex();
        bb.vertex(m, 0, 0, 0).color(r, g, b, alpha).endVertex();

        bb.vertex(m, x0, y0, 0).color(r, g, b, 0).endVertex();
        bb.vertex(m, x0, y1, 0).color(r, g, b, 0).endVertex();
        bb.vertex(m, 0, 0, 0).color(r, g, b, alpha).endVertex();
        bb.vertex(m, 0, 0, 0).color(r, g, b, alpha).endVertex();
    }
}