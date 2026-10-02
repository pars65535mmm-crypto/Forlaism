package com.tyami.forlaism.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.tyami.forlaism.Forlaism;
import com.tyami.forlaism.client.model.EndWardenModel;
import com.tyami.forlaism.entity.EndWardenEntity;

import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;

/**
 * EndWarden のレンダラ。
 *
 * テクスチャ : forlaism:textures/entity/end_warden.png
 * 影のサイズ : 3.0
 */
public class EndWardenRenderer
        extends MobRenderer<EndWardenEntity, EndWardenModel<EndWardenEntity>> {

    private static final ResourceLocation TEXTURE =
            new ResourceLocation(Forlaism.MOD_ID, "textures/entity/end_warden.png");

    public EndWardenRenderer(EntityRendererProvider.Context context) {
        super(context, new EndWardenModel<>(context.bakeLayer(EndWardenModel.LAYER_LOCATION)), 3.0F);
    }

    @Override
    public ResourceLocation getTextureLocation(EndWardenEntity entity) {
        return TEXTURE;
    }

    @Override
    protected void scale(EndWardenEntity entity, PoseStack poseStack, float partialTicks) {
        // モデルはブロック単位で組まれているので、ここで微調整可能
        // デフォルト 1.0 でよい
    }
}