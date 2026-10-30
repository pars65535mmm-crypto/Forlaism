package com.tyami.forlaism.client;

import com.tyami.forlaism.client.renderer.FreezerClockRenderer;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.extensions.common.IClientItemExtensions;

/**
 * 回る冷凍時計のクライアント拡張。
 */
@OnlyIn(Dist.CLIENT)
public class FreezerClockClientExtensions implements IClientItemExtensions {

    private static final FreezerClockRenderer RENDERER = new FreezerClockRenderer();

    @Override
    public BlockEntityWithoutLevelRenderer getCustomRenderer() {
        return RENDERER;
    }
}
