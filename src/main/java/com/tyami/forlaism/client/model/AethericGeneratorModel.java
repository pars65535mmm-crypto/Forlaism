package com.tyami.forlaism.client.model;

import com.tyami.forlaism.Forlaism;
import com.tyami.forlaism.block.entity.AethericGeneratorBlockEntity;

import net.minecraft.resources.ResourceLocation;

import software.bernie.geckolib.model.GeoModel;

public class AethericGeneratorModel extends GeoModel<AethericGeneratorBlockEntity> {

    @Override
    public ResourceLocation getModelResource(AethericGeneratorBlockEntity animatable) {
        return new ResourceLocation(
                Forlaism.MOD_ID,
                "geo/forlaism_aetheric_generator.geo.json"
        );
    }

    @Override
    public ResourceLocation getTextureResource(AethericGeneratorBlockEntity animatable) {
        // ★ アイテム側と完全に同じパス
        return new ResourceLocation(
                Forlaism.MOD_ID,
                "textures/block/forlaism_aetheric_generator_p.png"
        );
    }

    @Override
    public ResourceLocation getAnimationResource(AethericGeneratorBlockEntity animatable) {
        return new ResourceLocation(
                Forlaism.MOD_ID,
                "animations/forlaism_aetheric_generator.animation_ekusupotobann.json"
        );
    }
}