package com.tyami.forlaism.client;

import com.tyami.forlaism.item.SKR360InfItem;
import com.tyami.forlaism.client.renderer.SKR360InfRenderer;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.world.item.Item;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.extensions.common.IClientItemExtensions;

/**
 * SKR360 Inf のクライアント拡張。
 *
 * GeckoLib のアイテムレンダラを BuiltinItemRendererRegistry 経由で
 * バニラの ItemRenderer に差し込む。
 */
@OnlyIn(Dist.CLIENT)
public class SKR360InfClientExtensions implements IClientItemExtensions {

    private static final SKR360InfRenderer RENDERER = new SKR360InfRenderer();

    @Override
    public BlockEntityWithoutLevelRenderer getCustomRenderer() {
        return RENDERER;
    }
}