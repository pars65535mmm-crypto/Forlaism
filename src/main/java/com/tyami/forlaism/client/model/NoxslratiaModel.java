package com.tyami.forlaism.client.model;

import com.tyami.forlaism.Forlaism;
import com.tyami.forlaism.entity.NoxslratiaEntity;

import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

/**
 * ノクスラティアのGeckoLibモデル。
 */
public class NoxslratiaModel extends GeoModel<NoxslratiaEntity> {

    @Override
    public ResourceLocation getModelResource(NoxslratiaEntity animatable) {
        return new ResourceLocation(Forlaism.MOD_ID, "geo/noxslratia.geo.json");
    }

    @Override
    public ResourceLocation getTextureResource(NoxslratiaEntity animatable) {
        return new ResourceLocation(Forlaism.MOD_ID, "textures/entity/noxslratia.png");
    }

    @Override
    public ResourceLocation getAnimationResource(NoxslratiaEntity animatable) {
        return new ResourceLocation(Forlaism.MOD_ID, "animations/noxslratia.animation.json");
    }
}