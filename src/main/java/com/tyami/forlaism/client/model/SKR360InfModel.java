package com.tyami.forlaism.client.model;

import com.tyami.forlaism.Forlaism;
import com.tyami.forlaism.item.SKR360InfItem;

import net.minecraft.resources.ResourceLocation;

import software.bernie.geckolib.model.GeoModel;

/**
 * SKR360 Inf のモデル。
 */
public class SKR360InfModel extends GeoModel<SKR360InfItem> {

    @Override
    public ResourceLocation getModelResource(SKR360InfItem animatable) {
        return new ResourceLocation(
                Forlaism.MOD_ID,
                "geo/skr360_inf.geo.json"
        );
    }

    @Override
    public ResourceLocation getTextureResource(SKR360InfItem animatable) {
        return new ResourceLocation(
                Forlaism.MOD_ID,
                "textures/item/sakuragun.png"
        );
    }

    @Override
    public ResourceLocation getAnimationResource(SKR360InfItem animatable) {
        return new ResourceLocation(
                Forlaism.MOD_ID,
                "animations/skr360_inf.animation.json"
        );
    }
}