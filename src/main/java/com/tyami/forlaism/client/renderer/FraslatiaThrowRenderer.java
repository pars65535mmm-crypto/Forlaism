package com.tyami.forlaism.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.tyami.forlaism.entity.FraslatiaThrowEntity;
import com.tyami.forlaism.registry.Items;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

/**
 * フラスラティア投擲体のレンダラ。
 *
 * 【常時表示・カリング完全無効化版】
 *   shouldRender は常に true。
 *   距離判定すらしない。
 */
public class FraslatiaThrowRenderer extends EntityRenderer<FraslatiaThrowEntity> {

    private static final int FULL_BRIGHT = 0xF000F0;

    private ItemStack cachedStack;

    public FraslatiaThrowRenderer(EntityRendererProvider.Context ctx) {
        super(ctx);
    }

    @Override
    public ResourceLocation getTextureLocation(FraslatiaThrowEntity entity) {
        return InventoryMenu.BLOCK_ATLAS;
    }

    /**
     * ★ 常に true ★
     *
     * カリング完全無効化。
     * これで絶対に描画がスキップされない。
     */
    @Override
    public boolean shouldRender(
            FraslatiaThrowEntity entity,
            Frustum frustum,
            double camX,
            double camY,
            double camZ
    ) {
        // 常時表示
        return true;
    }

    @Override
    public void render(
            FraslatiaThrowEntity entity,
            float entityYaw,
            float partialTick,
            PoseStack poseStack,
            MultiBufferSource buffer,
            int packedLight
    ) {
        super.render(entity, entityYaw, partialTick, poseStack, buffer, packedLight);

        if (cachedStack == null) {
            cachedStack = new ItemStack(Items.FRASLATIA.get());
        }

        poseStack.pushPose();

        // 進行方向に向ける
        poseStack.mulPose(Axis.YP.rotationDegrees(-entityYaw));
        float spin = (entity.tickCount + partialTick) * 5.0F;
        poseStack.mulPose(Axis.ZP.rotationDegrees(spin));
        poseStack.mulPose(Axis.YP.rotationDegrees(90.0F));
        poseStack.mulPose(Axis.XP.rotationDegrees(90.0F));

        // サイズ
        poseStack.scale(1.2F, 1.2F, 1.2F);

        // アイテム描画
        Minecraft.getInstance().getItemRenderer().renderStatic(
                cachedStack,
                ItemDisplayContext.FIXED,
                FULL_BRIGHT,
                OverlayTexture.NO_OVERLAY,
                poseStack,
                buffer,
                entity.level(),
                entity.getId()
        );

        poseStack.popPose();
    }
}