package com.tyami.forlaism.client;

import com.tyami.forlaism.client.renderer.AethericGeneratorItemRenderer;

import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.extensions.common.IClientItemExtensions;

/**
 * 電核機アイテムのクライアント拡張。
 */
@OnlyIn(Dist.CLIENT)
public class AethericGeneratorItemClientExtensions implements IClientItemExtensions {

    private static final AethericGeneratorItemRenderer RENDERER =
            new AethericGeneratorItemRenderer();

    @Override
    public BlockEntityWithoutLevelRenderer getCustomRenderer() {
        return RENDERER;
    }
}