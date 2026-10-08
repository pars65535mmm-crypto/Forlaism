package com.tyami.forlaism.client.model;

import com.tyami.forlaism.Forlaism;
import com.tyami.forlaism.item.AethericGeneratorItem;

import net.minecraft.resources.ResourceLocation;

import software.bernie.geckolib.model.GeoModel;

public class AethericGeneratorItemModel extends GeoModel<AethericGeneratorItem> {

    @Override
    public ResourceLocation getModelResource(AethericGeneratorItem animatable) {
        return new ResourceLocation(
                Forlaism.MOD_ID,
                "geo/forlaism_aetheric_generator.geo.json"
        );
    }

    @Override
    public ResourceLocation getTextureResource(AethericGeneratorItem animatable) {
        return new ResourceLocation(
                Forlaism.MOD_ID,
                "textures/block/forlaism_aetheric_generator_p.png"
        );
    }

    @Override
    public ResourceLocation getAnimationResource(AethericGeneratorItem animatable) {
        return new ResourceLocation(
                Forlaism.MOD_ID,
                "animations/forlaism_aetheric_generator.animation_ekusupotobann.json"
        );
    }
}