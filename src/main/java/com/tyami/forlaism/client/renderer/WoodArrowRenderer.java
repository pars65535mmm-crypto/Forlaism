package com.tyami.forlaism.client.renderer;

import com.tyami.forlaism.Forlaism;
import com.tyami.forlaism.entity.WoodArrowEntity;

import net.minecraft.client.renderer.entity.ArrowRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;

/**
 * 木の矢のレンダラ。
 *
 * バニラの ArrowRenderer を継承して、
 * テクスチャだけ wood_arrow.png に差し替える。
 */
public class WoodArrowRenderer extends ArrowRenderer<WoodArrowEntity> {

    private static final ResourceLocation TEXTURE =
            new ResourceLocation(Forlaism.MOD_ID, "textures/entity/wood_arrow.png");

    public WoodArrowRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public ResourceLocation getTextureLocation(WoodArrowEntity entity) {
        return TEXTURE;
    }
}