package com.tyami.forlaism.client.renderer;

import com.tyami.forlaism.client.model.AethericGeneratorItemModel;
import com.tyami.forlaism.item.AethericGeneratorItem;

import software.bernie.geckolib.renderer.GeoItemRenderer;

/**
 * 電核機アイテムのGeckoLibレンダラ。
 */
public class AethericGeneratorItemRenderer extends GeoItemRenderer<AethericGeneratorItem> {

    public AethericGeneratorItemRenderer() {
        super(new AethericGeneratorItemModel());
    }
}