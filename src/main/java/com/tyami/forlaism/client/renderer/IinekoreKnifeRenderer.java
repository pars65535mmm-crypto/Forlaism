package com.tyami.forlaism.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.tyami.forlaism.client.model.IinekoreKnifeModel;
import com.tyami.forlaism.entity.IinekoreKnifeEntity;

import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;

/**
 * 投げナイフのレンダラ。
 *
 * - 飛行中：Z軸でくるくる回転
 * - 刺さった時：回転停止
 */
public class IinekoreKnifeRenderer extends EntityRenderer<IinekoreKnifeEntity> {

    private static final ResourceLocation TEXTURE =
            new ResourceLocation("forlaism", "textures/entity/iinekore_knife.png");

    /** Z軸回転速度（度/tick）。 */
    private static final float SPIN_SPEED = 40.0F;

    private final IinekoreKnifeModel<IinekoreKnifeEntity> model;

    public IinekoreKnifeRenderer(EntityRendererProvider.Context ctx) {
        super(ctx);
        this.model = new IinekoreKnifeModel<>(
                ctx.bakeLayer(IinekoreKnifeModel.LAYER_LOCATION)
        );
    }

    @Override
    public ResourceLocation getTextureLocation(IinekoreKnifeEntity entity) {
        return TEXTURE;
    }

    @Override
    public void render(
            IinekoreKnifeEntity entity,
            float entityYaw,
            float partialTick,
            PoseStack poseStack,
            MultiBufferSource buffer,
            int packedLight
    ) {
        super.render(entity, entityYaw, partialTick, poseStack, buffer, packedLight);

        poseStack.pushPose();

        // =========================================================
        // 進行方向に向ける
        // =========================================================
        poseStack.mulPose(Axis.YP.rotationDegrees(-entityYaw));
        poseStack.mulPose(Axis.XP.rotationDegrees(entity.getXRot()));

        // =========================================================
        // Z軸回転（飛行中のみ）
        // =========================================================
        if (!entity.isStuck()) {
            float spin = (entity.tickCount + partialTick) * SPIN_SPEED;
            poseStack.mulPose(Axis.ZP.rotationDegrees(spin));
        }

        // =========================================================
        // サイズ調整（好みで）
        // =========================================================
        poseStack.scale(1.0F, 1.0F, 1.0F);

        if (entity.isStuck()) {
    // 保存された向きで固定
    poseStack.mulPose(Axis.YP.rotationDegrees(
            -entity.getStuckYaw() + 180.0F
    ));
    poseStack.mulPose(Axis.XP.rotationDegrees(
            -entity.getStuckPitch()
    ));
}

        // =========================================================
        // 描画
        // =========================================================
        var vertexConsumer = buffer.getBuffer(
                RenderType.entityCutoutNoCull(TEXTURE)
        );

        model.setupAnim(entity, 0, 0, entity.tickCount + partialTick, 0, 0);
        model.renderToBuffer(
                poseStack,
                vertexConsumer,
                packedLight,
                OverlayTexture.NO_OVERLAY,
                1.0F, 1.0F, 1.0F, 1.0F
        );

        poseStack.popPose();
    }
}