package com.tyami.forlaism.client.renderer;

import com.tyami.forlaism.client.model.ChickenCannonModel;
import com.tyami.forlaism.item.ChickenCannonItem;

import software.bernie.geckolib.renderer.GeoItemRenderer;

public class ChickenCannonRenderer extends GeoItemRenderer<ChickenCannonItem> {

    public ChickenCannonRenderer() {
        super(new ChickenCannonModel());
    }
}