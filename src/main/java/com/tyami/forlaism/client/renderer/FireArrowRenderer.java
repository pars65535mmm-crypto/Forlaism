package com.tyami.forlaism.client.renderer;

import com.tyami.forlaism.Forlaism;
import com.tyami.forlaism.entity.FireArrowEntity;

import net.minecraft.client.renderer.entity.ArrowRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;

/**
 * 火の矢のレンダラ。
 */
public class FireArrowRenderer extends ArrowRenderer<FireArrowEntity> {

    private static final ResourceLocation TEXTURE =
            new ResourceLocation(Forlaism.MOD_ID, "textures/entity/fire_arrow.png");

    public FireArrowRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public ResourceLocation getTextureLocation(FireArrowEntity entity) {
        return TEXTURE;
    }
}