package com.tyami.forlaism.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.tyami.forlaism.registry.Items;

import net.minecraft.client.Minecraft;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

/**
 * 魔法使いの帽子を頭の上にアイテムモデルとして描画するヘルパー。
 */
public final class MahouTsukaiNoBoushiArmorRenderer {

    private MahouTsukaiNoBoushiArmorRenderer() {
    }

    public static void render(
            PoseStack poseStack,
            HumanoidModel<?> humanoidModel,
            LivingEntity entity,
            MultiBufferSource buffer,
            int packedLight
    ) {
        ItemStack helmet = entity.getItemBySlot(EquipmentSlot.HEAD);
        if (helmet.isEmpty() || !helmet.is(Items.MAHOUTSUKAI_NO_BOUSHI.get())) {
            return;
        }

        poseStack.pushPose();

        /*
         * ========================================
         * 1. 頭パーツに追従
         * ========================================
         */
        humanoidModel.head.translateAndRotate(poseStack);

        /*
         * ========================================
         * 2. 天地反転（X軸180度）
         * ========================================
         */
        poseStack.mulPose(Axis.XP.rotationDegrees(180.0F));

        /*
         * ========================================
         * 3. 前後反転（Y軸180度）
         * ========================================
         */
        poseStack.mulPose(Axis.YP.rotationDegrees(180.0F));

        /*
         * ========================================
         * 4. モデルを原点中心に持ってくる
         * ========================================
         *
         * Blockbenchモデルは 0〜16 の空間で作られてるので、
         * 中心は (8, 8, 8)。
         *
         * アイテムモデルの座標系では 1 ブロック = 16 単位なので、
         * 8 単位 = 0.5 ブロック分、ズラしてやる必要がある。
         */


        /*
         * ========================================
         * 5. 位置調整
         * ========================================
         */
        poseStack.translate(0.0D, 1.0D, 0.0D);

        /*
         * ========================================
         * 6. サイズ調整
         * ========================================
         */
        float scale = 1.2F;
        poseStack.scale(scale, scale, scale);

        /*
         * ========================================
         * 7. アイテムモデルを描画
         * ========================================
         */
        Minecraft mc = Minecraft.getInstance();
        ItemRenderer itemRenderer = mc.getItemRenderer();

        itemRenderer.renderStatic(
                helmet,
                ItemDisplayContext.FIXED,
                packedLight,
                OverlayTexture.NO_OVERLAY,
                poseStack,
                buffer,
                entity.level(),
                entity.getId()
        );

        poseStack.popPose();
    }
}