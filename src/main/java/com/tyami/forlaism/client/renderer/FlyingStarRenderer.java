package com.tyami.forlaism.client.renderer;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.tyami.forlaism.entity.FlyingStarEntity;

import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.InventoryMenu;

import org.joml.Matrix4f;
import org.joml.Quaternionf;

/**
 * 射出型の星のレンダラ。
 * SakuraBulletRenderer と同じ発光球スタイル。
 */
public class FlyingStarRenderer extends EntityRenderer<FlyingStarEntity> {

    public FlyingStarRenderer(EntityRendererProvider.Context ctx) {
        super(ctx);
    }

    @Override
    public ResourceLocation getTextureLocation(FlyingStarEntity entity) {
        return InventoryMenu.BLOCK_ATLAS;
    }

    @Override
    public void render(FlyingStarEntity entity, float yaw, float partialTick,
                       PoseStack poseStack, MultiBufferSource buffer, int light) {
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

        // 外側
        bb.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
        addGlowQuad(bb, matrix, 0.6F, 1.0F, 0.9F, 0.6F, 0.55F);
        BufferUploader.drawWithShader(bb.end());

        // 中間
        bb.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
        addGlowQuad(bb, matrix, 0.35F, 1.0F, 0.95F, 0.85F, 0.85F);
        BufferUploader.drawWithShader(bb.end());

        // コア
        bb.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
        addGlowQuad(bb, matrix, 0.15F, 1.0F, 1.0F, 1.0F, 1.0F);
        BufferUploader.drawWithShader(bb.end());

        poseStack.popPose();

        RenderSystem.depthMask(true);
        RenderSystem.enableCull();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableBlend();
    }

    private static void addGlowQuad(BufferBuilder bb, Matrix4f m,
                                    float size,
                                    float r, float g, float b, float alpha) {
        float x0 = -size, x1 = size, y0 = -size, y1 = size;
        bb.vertex(m, x0, y1, 0).color(r, g, b, 0F).endVertex();
        bb.vertex(m, x1, y1, 0).color(r, g, b, 0F).endVertex();
        bb.vertex(m, x1, y0, 0).color(r, g, b, 0F).endVertex();
        bb.vertex(m, x0, y0, 0).color(r, g, b, 0F).endVertex();

        bb.vertex(m, x0, y1, 0).color(r, g, b, 0F).endVertex();
        bb.vertex(m, x1, y1, 0).color(r, g, b, 0F).endVertex();
        bb.vertex(m, 0, 0, 0).color(r, g, b, alpha).endVertex();
        bb.vertex(m, 0, 0, 0).color(r, g, b, alpha).endVertex();

        bb.vertex(m, x1, y1, 0).color(r, g, b, 0F).endVertex();
        bb.vertex(m, x1, y0, 0).color(r, g, b, 0F).endVertex();
        bb.vertex(m, 0, 0, 0).color(r, g, b, alpha).endVertex();
        bb.vertex(m, 0, 0, 0).color(r, g, b, alpha).endVertex();

        bb.vertex(m, x1, y0, 0).color(r, g, b, 0F).endVertex();
        bb.vertex(m, x0, y0, 0).color(r, g, b, 0F).endVertex();
        bb.vertex(m, 0, 0, 0).color(r, g, b, alpha).endVertex();
        bb.vertex(m, 0, 0, 0).color(r, g, b, alpha).endVertex();

        bb.vertex(m, x0, y0, 0).color(r, g, b, 0F).endVertex();
        bb.vertex(m, x0, y1, 0).color(r, g, b, 0F).endVertex();
        bb.vertex(m, 0, 0, 0).color(r, g, b, alpha).endVertex();
        bb.vertex(m, 0, 0, 0).color(r, g, b, alpha).endVertex();
    }
}