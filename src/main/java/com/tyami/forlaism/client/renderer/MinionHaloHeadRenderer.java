package com.tyami.forlaism.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.tyami.forlaism.registry.Items;

import net.minecraft.client.Minecraft;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

/**
 * 眷属の光輪を頭の上にアイテムとして描画するレイヤー。
 */
public class MinionHaloHeadRenderer<T extends LivingEntity, M extends EntityModel<T>>
        extends RenderLayer<T, M> {

    public MinionHaloHeadRenderer(RenderLayerParent<T, M> parent) {
        super(parent);
    }

        @Override
    public void render(
            PoseStack poseStack,
            MultiBufferSource buffer,
            int packedLight,
            T entity,
            float limbSwing,
            float limbSwingAmount,
            float partialTick,
            float ageInTicks,
            float netHeadYaw,
            float headPitch
    ) {
        ItemStack helmet = entity.getItemBySlot(EquipmentSlot.HEAD);

        if (helmet.isEmpty() || !helmet.is(Items.MINION_HALO.get())) {
            return;
        }

        // HumanoidModel 以外は頭パーツがわからないのでスキップ
        if (!(this.getParentModel() instanceof HumanoidModel<?> humanoidModel)) {
            return;
        }

        poseStack.pushPose();

        // 頭に追従
        humanoidModel.head.translateAndRotate(poseStack);

        // 頭上の位置調整
        poseStack.translate(0.0D, -0.75D, 0.0D);

        // 回転アニメーション
        float spin = (entity.tickCount + partialTick) * 2.0F;
        poseStack.mulPose(Axis.YP.rotationDegrees(spin));

        // サイズ
        poseStack.scale(0.8F, 0.8F, 0.8F);

        // アイテム描画（引数の順番に注意！）
        Minecraft.getInstance().getItemRenderer().renderStatic(
                helmet,
                ItemDisplayContext.HEAD,
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