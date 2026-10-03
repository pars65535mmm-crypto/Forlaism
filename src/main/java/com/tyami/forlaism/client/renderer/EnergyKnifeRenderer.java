package com.tyami.forlaism.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.tyami.forlaism.entity.EnergyKnifeEntity;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

/**
 * エネルギーナイフのレンダラー。
 *
 * アイテムモデルをそのまま回転させながら飛ばす。
 * 発光させたいので、light は FULL_BRIGHT を使う。
 */
public class EnergyKnifeRenderer extends EntityRenderer<EnergyKnifeEntity> {

    /** 発光用のライト値。 */
    private static final int FULL_BRIGHT = 0xF000F0;

    private ItemStack cachedStack;

    public EnergyKnifeRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public ResourceLocation getTextureLocation(EnergyKnifeEntity entity) {
        return InventoryMenu.BLOCK_ATLAS;
    }

    @Override
    public void render(
            EnergyKnifeEntity entity,
            float entityYaw,
            float partialTick,
            PoseStack poseStack,
            MultiBufferSource buffer,
            int packedLight
    ) {
        super.render(entity, entityYaw, partialTick, poseStack, buffer, packedLight);

        if (cachedStack == null) {
            cachedStack = new ItemStack(
                    com.tyami.forlaism.registry.Items.ENERGY_DAGGER.get()
            );
        }

        poseStack.pushPose();

        // 進行方向に向ける
        poseStack.mulPose(Axis.YP.rotationDegrees(-entityYaw));

        // 回転アニメーション（回転しながら飛ぶ）
        float spin = (entity.tickCount + partialTick) * 40.0F;
        poseStack.mulPose(Axis.ZP.rotationDegrees(spin));

        // 進行方向に寝かせる
        poseStack.mulPose(Axis.XP.rotationDegrees(90.0F));

        // サイズ
        poseStack.scale(0.7F, 0.7F, 0.7F);

        // アイテム描画（FULL_BRIGHT で発光）
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