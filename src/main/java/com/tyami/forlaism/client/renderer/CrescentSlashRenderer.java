package com.tyami.forlaism.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.tyami.forlaism.entity.CrescentSlashEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import org.joml.Matrix4f;

public class CrescentSlashRenderer extends EntityRenderer<CrescentSlashEntity> {

    private static final ResourceLocation BLANK_TEXTURE =
            new ResourceLocation("forlaism", "textures/entity/blank.png");

    public CrescentSlashRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public ResourceLocation getTextureLocation(CrescentSlashEntity entity) {
        return BLANK_TEXTURE;
    }

    @Override
    public void render(CrescentSlashEntity entity, float entityYaw, float partialTick,
                       PoseStack poseStack, MultiBufferSource bufferSource, int packedLight) {
        super.render(entity, entityYaw, partialTick, poseStack, bufferSource, packedLight);

        poseStack.pushPose();

        // 進行方向に向ける
        poseStack.mulPose(Axis.YP.rotationDegrees(entity.getSyncedYaw() - 90.0F));
        poseStack.mulPose(Axis.ZP.rotationDegrees(entity.getSyncedPitch()));

        // 三日月のポリゴンを動的に生成
        VertexConsumer consumer = bufferSource.getBuffer(RenderType.translucent());
        Matrix4f matrix = poseStack.last().pose();

        renderCrescent(consumer, matrix);

        // 裏面も描画
        poseStack.mulPose(Axis.XP.rotationDegrees(180.0F));
        renderCrescent(consumer, poseStack.last().pose());

        poseStack.popPose();
    }

    /**
     * 三日月（クレセント）形状の頂点を扇状に生成し描画する。
     * 赤・オレンジ・白・水色のグラデーションカラーを各頂点に付与。
     */
    private void renderCrescent(VertexConsumer consumer, Matrix4f matrix) {
        int segments = 24;
        float outerRadius = 1.8F;
        float innerRadius = 1.4F;
        float innerCenterOffset = 0.5F; // 内側の中心を前にずらすことで三日月になる

        // 赤(0xFFFF3333) -> オレンジ(0xFFFF8800) -> 白(0xFFFFFFFF) -> 水色(0xFF00FFFF)
        int r1 = 255, g1 = 50, b1 = 50, a1 = 200;   // 外側：赤
        int r2 = 255, g2 = 180, b2 = 50, a2 = 220; // 中間：オレンジ
        int r3 = 255, g3 = 255, b3 = 255, a3 = 240; // 刃先：白
        int r4 = 0, g4 = 255, b4 = 255, a4 = 220;   // 内側：水色

        for (int i = 0; i < segments; i++) {
            float t0 = (float) i / (float) segments;
            float t1 = (float) (i + 1) / (float) segments;

            float angle0 = (float) ((-0.5 + t0) * Math.PI * 0.85);
            float angle1 = (float) ((-0.5 + t1) * Math.PI * 0.85);

            // 外側の頂点
            float x0_out = (float) Math.cos(angle0) * outerRadius;
            float z0_out = (float) Math.sin(angle0) * outerRadius;

            float x1_out = (float) Math.cos(angle1) * outerRadius;
            float z1_out = (float) Math.sin(angle1) * outerRadius;

            // 内側の頂点（中心を前方にシフト）
            float x0_in = (float) Math.cos(angle0) * innerRadius + innerCenterOffset;
            float z0_in = (float) Math.sin(angle0) * (innerRadius * 0.7F);

            float x1_in = (float) Math.cos(angle1) * innerRadius + innerCenterOffset;
            float z1_in = (float) Math.sin(angle1) * (innerRadius * 0.7F);

            // 頂点ごとのグラデーション色補間
            float colorProgress = (float) Math.abs(t0 - 0.5) * 2.0F; // 中心が白/水色、端が赤/オレンジ
            int r = (int) (r3 * (1.0F - colorProgress) + r1 * colorProgress);
            int g = (int) (g4 * (1.0F - colorProgress) + g1 * colorProgress);
            int b = (int) (b4 * (1.0F - colorProgress) + b1 * colorProgress);

            consumer.vertex(matrix, x0_out, 0.0F, z0_out).color(r1, g1, b1, a1).uv(0, 0).uv2(0xF000F0).endVertex();
            consumer.vertex(matrix, x1_out, 0.0F, z1_out).color(r2, g2, b2, a2).uv(1, 0).uv2(0xF000F0).endVertex();
            consumer.vertex(matrix, x1_in, 0.0F, z1_in).color(r, g, b, a3).uv(1, 1).uv2(0xF000F0).endVertex();
            consumer.vertex(matrix, x0_in, 0.0F, z0_in).color(r4, g4, b4, a4).uv(0, 1).uv2(0xF000F0).endVertex();
        }
    }
}
