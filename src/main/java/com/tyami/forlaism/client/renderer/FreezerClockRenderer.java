package com.tyami.forlaism.client.renderer;

import com.tyami.forlaism.client.model.FreezerClockModel;
import com.tyami.forlaism.item.FreezerClockItem;
import software.bernie.geckolib.renderer.GeoItemRenderer;

/**
 * 回る冷凍時計のGeckoLibレンダラー。
 */
public class FreezerClockRenderer extends GeoItemRenderer<FreezerClockItem> {

    public FreezerClockRenderer() {
        super(new FreezerClockModel());
    }
}
