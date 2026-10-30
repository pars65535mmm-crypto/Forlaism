package com.tyami.forlaism.client.model;

import com.tyami.forlaism.Forlaism;
import com.tyami.forlaism.item.ChickenCannonItem;

import net.minecraft.resources.ResourceLocation;

import software.bernie.geckolib.model.GeoModel;

/**
 * チキンキャノンのGeckoLibモデル。
 */
public class ChickenCannonModel extends GeoModel<ChickenCannonItem> {

    @Override
    public ResourceLocation getModelResource(ChickenCannonItem animatable) {
        return new ResourceLocation(Forlaism.MOD_ID, "geo/re_chicken_cannon.geo.json");
    }

    @Override
    public ResourceLocation getTextureResource(ChickenCannonItem animatable) {
        return new ResourceLocation(Forlaism.MOD_ID, "textures/item/re_chicken_cannon.png");
    }

    @Override
    public ResourceLocation getAnimationResource(ChickenCannonItem animatable) {
        // アニメーションなし（静的）
        return new ResourceLocation(Forlaism.MOD_ID, "animations/chicken_cannon.animation.json");
    }
}