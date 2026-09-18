package com.tyami.forlaism.client.renderer;

import com.tyami.forlaism.block.entity.AethericGeneratorBlockEntity;
import com.tyami.forlaism.client.model.AethericGeneratorModel;

import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;

import software.bernie.geckolib.renderer.GeoBlockRenderer;

public class AethericGeneratorItemRenderer
        extends GeoBlockRenderer<AethericGeneratorBlockEntity> {

    public AethericGeneratorItemRenderer(BlockEntityRendererProvider.Context context) {
        super(new AethericGeneratorModel());
    }
}