package com.tyami.forlaism.client.renderer;

import com.tyami.forlaism.client.model.NoxslratiaModel;
import com.tyami.forlaism.entity.NoxslratiaEntity;

import net.minecraft.client.renderer.entity.EntityRendererProvider;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

/**
 * ノクスラティアのGeckoLibレンダラ。
 */
public class NoxslratiaRenderer extends GeoEntityRenderer<NoxslratiaEntity> {

    public NoxslratiaRenderer(EntityRendererProvider.Context context) {
        super(context, new NoxslratiaModel());
        this.shadowRadius = 1.5F;
    }
}