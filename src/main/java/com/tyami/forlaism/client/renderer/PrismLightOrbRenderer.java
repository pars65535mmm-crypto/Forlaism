package com.tyami.forlaism.client.renderer;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.tyami.forlaism.entity.PrismLightOrbEntity;

import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.InventoryMenu;

import org.joml.Matrix4f;
import org.joml.Quaternionf;

/**
 * プリズムライトの光の玉。
 *
 * - 中心の白熱球体
 * - 周囲をまわる虹色の輪
 * - 加算合成で発光
 */
public class PrismLightOrbRenderer extends EntityRenderer<PrismLightOrbEntity> {

    public PrismLightOrbRenderer(EntityRendererProvider.Context ctx) {
        super(ctx);
    }

    @Override
    public ResourceLocation getTextureLocation(PrismLightOrbEntity entity) {
        return InventoryMenu.BLOCK_ATLAS;
    }

    @Override
    public void render(PrismLightOrbEntity entity, float yaw, float partialTick,
                       PoseStack poseStack, MultiBufferSource buffer, int light) {
        super.render(entity, yaw, partialTick, poseStack, buffer, light);

        float time = (entity.tickCount + partialTick) * 0.1F;

        poseStack.pushPose();

        // カメラを向くビルボード
        Quaternionf cam = this.entityRenderDispatcher.cameraOrientation();
        poseStack.mulPose(cam);

        // 加算合成で発光
        RenderSystem.enableBlend();
        RenderSystem.blendFunc(
                com.mojang.blaze3d.platform.GlStateManager.SourceFactor.SRC_ALPHA,
                com.mojang.blaze3d.platform.GlStateManager.DestFactor.ONE
        );
        RenderSystem.depthMask(false);
        RenderSystem.disableCull();

        Matrix4f matrix = poseStack.last().pose();
        Tesselator tess = Tesselator.getInstance();
        BufferBuilder bb = tess.getBuilder();

        // =====================================================
        // 1. 外側の大きな光の球（半透明の虹色）
        // =====================================================
        float outerSize = 0.9F;
        float hue = (time * 0.5F) % 1.0F;
        float[] outerRGB = hsvToRgb(hue, 0.6F, 1.0F);

        RenderSystem.setShader(GameRenderer::getPositionColorShader);

        bb.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
        addQuad(bb, matrix,
                -outerSize, -outerSize, outerSize, outerSize,
                outerRGB[0], outerRGB[1], outerRGB[2], 0.35F,
                outerRGB[0], outerRGB[1], outerRGB[2], 0.0F);
        BufferUploader.drawWithShader(bb.end());

        // =====================================================
        // 2. 中間の球（明るい青白）
        // =====================================================
        float midSize = 0.5F;
        bb.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
        addQuad(bb, matrix,
                -midSize, -midSize, midSize, midSize,
                0.7F, 0.9F, 1.0F, 0.8F,
                0.4F, 0.7F, 1.0F, 0.0F);
        BufferUploader.drawWithShader(bb.end());

        // =====================================================
        // 3. 中心の白熱コア
        // =====================================================
        float coreSize = 0.22F;
        bb.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
        addQuad(bb, matrix,
                -coreSize, -coreSize, coreSize, coreSize,
                1.0F, 1.0F, 1.0F, 1.0F,
                1.0F, 1.0F, 1.0F, 0.5F);
        BufferUploader.drawWithShader(bb.end());

        // =====================================================
        // 4. 周囲をまわる虹色のクロス（4芒星風）
        // =====================================================
        for (int i = 0; i < 4; i++) {
            float angle = time * 1.5F + (i * (float) (Math.PI / 2));
            float cx = (float) Math.cos(angle) * 0.7F;
            float cy = (float) Math.sin(angle) * 0.7F;

            float h = (hue + i * 0.25F) % 1.0F;
            float[] rgb = hsvToRgb(h, 1.0F, 1.0F);

            bb.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
            addQuad(bb, matrix,
                    cx - 0.15F, cy - 0.15F,
                    cx + 0.15F, cy + 0.15F,
                    rgb[0], rgb[1], rgb[2], 0.9F,
                    rgb[0], rgb[1], rgb[2], 0.0F);
            BufferUploader.drawWithShader(bb.end());
        }

        RenderSystem.depthMask(true);
        RenderSystem.enableCull();
        RenderSystem.disableBlend();

        poseStack.popPose();
    }

    /**
     * 中心が明るく、外側に向かって透明になる放射グラデのクアッド。
     */
    private static void addQuad(BufferBuilder bb, Matrix4f m,
                                float x0, float y0, float x1, float y1,
                                float r, float g, float b, float aInner,
                                float er, float eg, float eb, float aOuter) {
        // 中心の座標
        float cx = (x0 + x1) * 0.5F;
        float cy = (y0 + y1) * 0.5F;

        // 四隅（外側、透明）
        bb.vertex(m, x0, y1, 0).color(er, eg, eb, aOuter).endVertex();
        bb.vertex(m, x1, y1, 0).color(er, eg, eb, aOuter).endVertex();
        bb.vertex(m, x1, y0, 0).color(er, eg, eb, aOuter).endVertex();
        bb.vertex(m, x0, y0, 0).color(er, eg, eb, aOuter).endVertex();

        // 中心（明るい）
        // 中心を1頂点にした扇を4枚描く
        // 上
        bb.vertex(m, x0, y1, 0).color(er, eg, eb, aOuter).endVertex();
        bb.vertex(m, x1, y1, 0).color(er, eg, eb, aOuter).endVertex();
        bb.vertex(m, cx, cy, 0).color(r, g, b, aInner).endVertex();
        bb.vertex(m, cx, cy, 0).color(r, g, b, aInner).endVertex();

        // 右
        bb.vertex(m, x1, y1, 0).color(er, eg, eb, aOuter).endVertex();
        bb.vertex(m, x1, y0, 0).color(er, eg, eb, aOuter).endVertex();
        bb.vertex(m, cx, cy, 0).color(r, g, b, aInner).endVertex();
        bb.vertex(m, cx, cy, 0).color(r, g, b, aInner).endVertex();

        // 下
        bb.vertex(m, x1, y0, 0).color(er, eg, eb, aOuter).endVertex();
        bb.vertex(m, x0, y0, 0).color(er, eg, eb, aOuter).endVertex();
        bb.vertex(m, cx, cy, 0).color(r, g, b, aInner).endVertex();
        bb.vertex(m, cx, cy, 0).color(r, g, b, aInner).endVertex();

        // 左
        bb.vertex(m, x0, y0, 0).color(er, eg, eb, aOuter).endVertex();
        bb.vertex(m, x0, y1, 0).color(er, eg, eb, aOuter).endVertex();
        bb.vertex(m, cx, cy, 0).color(r, g, b, aInner).endVertex();
        bb.vertex(m, cx, cy, 0).color(r, g, b, aInner).endVertex();
    }

    private static float[] hsvToRgb(float h, float s, float v) {
        int i = (int) (h * 6);
        float f = h * 6 - i;
        float p = v * (1 - s);
        float q = v * (1 - f * s);
        float t = v * (1 - (1 - f) * s);
        return switch (i % 6) {
            case 0 -> new float[]{v, t, p};
            case 1 -> new float[]{q, v, p};
            case 2 -> new float[]{p, v, t};
            case 3 -> new float[]{p, q, v};
            case 4 -> new float[]{t, p, v};
            default -> new float[]{v, p, q};
        };
    }
}