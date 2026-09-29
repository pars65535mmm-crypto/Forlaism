package com.tyami.forlaism.client.renderer;

import com.tyami.forlaism.entity.BossCoreEntity;

import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;

/**
 * BossCoreEntity 用の「何も描画しない」レンダラ。
 *
 * Minecraftは EntityType にレンダラが登録されていないと
 * LevelRenderer で NPE を踏むため、空のレンダラを登録する。
 *
 * shouldRender を false にすることで、
 * 実際の描画処理は一切走らない。
 */
public class BossCoreEmptyRenderer extends EntityRenderer<BossCoreEntity> {

    private static final ResourceLocation BLANK =
            new ResourceLocation("minecraft", "textures/misc/white.png");

    public BossCoreEmptyRenderer(EntityRendererProvider.Context ctx) {
        super(ctx);
    }

    @Override
    public boolean shouldRender(BossCoreEntity entity, Frustum frustum, double x, double y, double z) {
        // 描画しない
        return false;
    }

    @Override
    public ResourceLocation getTextureLocation(BossCoreEntity entity) {
        return BLANK;
    }
}