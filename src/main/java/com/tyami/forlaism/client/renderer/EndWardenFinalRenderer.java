package com.tyami.forlaism.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.tyami.forlaism.Forlaism;
import com.tyami.forlaism.client.model.EndWardenModel;
import com.tyami.forlaism.entity.EndWardenFinalEntity;

import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;

/**
 * エンディストウォーデンのレンダラ。
 *
 * モデルは既存の EndWardenModel を流用（同じ見た目）。
 */
public class EndWardenFinalRenderer
        extends MobRenderer<EndWardenFinalEntity, EndWardenModel<EndWardenFinalEntity>> {

    private static final ResourceLocation TEXTURE =
            new ResourceLocation(Forlaism.MOD_ID, "textures/entity/end_warden.png");

    public EndWardenFinalRenderer(EntityRendererProvider.Context context) {
        super(context,
                new EndWardenModel<>(context.bakeLayer(EndWardenModel.LAYER_LOCATION)),
                4.0F);  // 影のサイズ
    }

    @Override
    public ResourceLocation getTextureLocation(EndWardenFinalEntity entity) {
        return TEXTURE;
    }

    @Override
    protected void scale(EndWardenFinalEntity entity, PoseStack poseStack, float partialTicks) {
        // 少し大きめ
        poseStack.scale(1.2F, 1.2F, 1.2F);
    }
}