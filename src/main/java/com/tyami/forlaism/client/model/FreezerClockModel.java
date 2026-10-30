package com.tyami.forlaism.client.model;

import com.tyami.forlaism.Forlaism;
import com.tyami.forlaism.item.FreezerClockItem;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

/**
 * 回る冷凍時計のGeckoLibモデル。
 */
public class FreezerClockModel extends GeoModel<FreezerClockItem> {

    @Override
    public ResourceLocation getModelResource(FreezerClockItem animatable) {
        return new ResourceLocation(
                Forlaism.MOD_ID,
                "geo/freezer_clock.geo.json"
        );
    }

    @Override
    public ResourceLocation getTextureResource(FreezerClockItem animatable) {
        return new ResourceLocation(
                Forlaism.MOD_ID,
                "textures/item/freezer_clocktex.png"
        );
    }

    @Override
    public ResourceLocation getAnimationResource(FreezerClockItem animatable) {
        return new ResourceLocation(
                Forlaism.MOD_ID,
                "animations/freezer_clock.animation.json"
        );
    }
}
