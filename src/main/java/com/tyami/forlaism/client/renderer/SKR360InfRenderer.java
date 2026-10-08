package com.tyami.forlaism.client.renderer;

import com.tyami.forlaism.client.model.SKR360InfModel;
import com.tyami.forlaism.item.SKR360InfItem;

import software.bernie.geckolib.renderer.GeoItemRenderer;

/**
 * SKR360 Inf のレンダラ。
 */
public class SKR360InfRenderer extends GeoItemRenderer<SKR360InfItem> {

    public SKR360InfRenderer() {
        super(new SKR360InfModel());
    }
}