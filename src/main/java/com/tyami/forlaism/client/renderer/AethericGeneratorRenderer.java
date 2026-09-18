package com.tyami.forlaism.client.renderer;

import com.tyami.forlaism.block.entity.AethericGeneratorBlockEntity;
import com.tyami.forlaism.client.model.AethericGeneratorModel;

import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;

import software.bernie.geckolib.renderer.GeoBlockRenderer;

public class AethericGeneratorRenderer
        extends GeoBlockRenderer<AethericGeneratorBlockEntity> {

    public AethericGeneratorRenderer(BlockEntityRendererProvider.Context context) {
        super(new AethericGeneratorModel());
    }
}