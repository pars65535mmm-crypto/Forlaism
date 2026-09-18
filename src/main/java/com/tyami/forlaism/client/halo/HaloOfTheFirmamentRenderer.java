package com.tyami.forlaism.client.halo;

import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

import top.theillusivec4.curios.api.SlotContext;
import top.theillusivec4.curios.api.client.ICurioRenderer;

public class HaloOfTheFirmamentRenderer implements ICurioRenderer {

    private static final ResourceLocation TEXTURE =
            new ResourceLocation(
                    "forlaism",
                    "textures/entity/ar3.png"
            );

    private final HaloOfTheFirmament<LivingEntity> model;

    public HaloOfTheFirmamentRenderer(
            HaloOfTheFirmament<LivingEntity> model) {
        this.model = model;
    }

    @Override
    public <T extends LivingEntity, M extends EntityModel<T>> void render(
            ItemStack stack,
            SlotContext slotContext,
            PoseStack poseStack,
            RenderLayerParent<T, M> renderLayerParent,
            MultiBufferSource renderTypeBuffer,
            int light,
            float limbSwing,
            float limbSwingAmount,
            float partialTicks,
            float ageInTicks,
            float netHeadYaw,
            float headPitch
    ) {
        LivingEntity entity = slotContext.entity();

        if (entity == null) {
            return;
        }

        poseStack.pushPose();

        /*
         * プレイヤーの頭に追従
         */
        if (renderLayerParent.getModel()
                instanceof HumanoidModel<?> humanoidModel) {

            humanoidModel.head.translateAndRotate(poseStack);
        }

        /*
         * ========================================
         * 頭上の位置調整
         * ========================================
         *
         * ここはBlockbenchモデルの原点に合わせる。
         */
        poseStack.translate(
                0.0D,
                -2.39D,
                0.1D
        );

        /*
         * ========================================
         * モデルアニメーション
         * ========================================
         */
        model.setupAnim(
                entity,
                limbSwing,
                limbSwingAmount,
                ageInTicks,
                netHeadYaw,
                headPitch
        );

        /*
         * ========================================
         * 描画
         * ========================================
         */
        var vertexConsumer =
                renderTypeBuffer.getBuffer(
                        RenderType.entityTranslucentEmissive(TEXTURE)
                );

        model.renderToBuffer(
                poseStack,
                vertexConsumer,
                light,
                OverlayTexture.NO_OVERLAY,
                1.0F,
                1.0F,
                1.0F,
                1.0F
        );

        poseStack.popPose();
    }
}