package com.tyami.forlaism.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.tyami.forlaism.entity.ChickenCannonProjectileEntity;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/**
 * 飛んでく鶏のレンダラー。
 * 原版の鶏アイテムモデルをそのまま使う（シンプル）。
 */
public class ChickenCannonProjectileRenderer extends EntityRenderer<ChickenCannonProjectileEntity> {

    private static final int FULL_BRIGHT = 0xF000F0;
    private ItemStack cachedStack;

    public ChickenCannonProjectileRenderer(EntityRendererProvider.Context ctx) {
        super(ctx);
    }

    @Override
    public ResourceLocation getTextureLocation(ChickenCannonProjectileEntity entity) {
        return InventoryMenu.BLOCK_ATLAS;
    }

    @Override
    public void render(
            ChickenCannonProjectileEntity entity,
            float entityYaw,
            float partialTick,
            PoseStack poseStack,
            MultiBufferSource buffer,
            int packedLight
    ) {
        super.render(entity, entityYaw, partialTick, poseStack, buffer, packedLight);

        if (cachedStack == null) {
            cachedStack = new ItemStack(Items.CHICKEN);
        }

        poseStack.pushPose();

        // 向き
        poseStack.mulPose(Axis.YP.rotationDegrees(-entityYaw));
        poseStack.mulPose(Axis.XP.rotationDegrees(entity.getXRot()));

        // 回転
        float spin = (entity.tickCount + partialTick) * 20.0F;
        poseStack.mulPose(Axis.ZP.rotationDegrees(spin));

        // サイズ
        poseStack.scale(1.5F, 1.5F, 1.5F);

        // 描画
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