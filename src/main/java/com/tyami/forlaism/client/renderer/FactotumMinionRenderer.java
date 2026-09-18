package com.tyami.forlaism.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.tyami.forlaism.entity.FactotumMinionEntity;
import com.tyami.forlaism.registry.Items;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

public class FactotumMinionRenderer extends EntityRenderer<FactotumMinionEntity> {

    private ItemStack itemStack;

    public FactotumMinionRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    private ItemStack getItem() {
        if (this.itemStack == null) {
            this.itemStack = new ItemStack(Items.REGALIA_OF_THE_FACTOTUM.get());
        }
        return this.itemStack;
    }

    @Override
    public ResourceLocation getTextureLocation(FactotumMinionEntity entity) {
        return InventoryMenu.BLOCK_ATLAS;
    }

    @Override
    public void render(FactotumMinionEntity entity, float entityYaw, float partialTick,
                       PoseStack poseStack, MultiBufferSource bufferSource, int packedLight) {
        super.render(entity, entityYaw, partialTick, poseStack, bufferSource, packedLight);

        poseStack.pushPose();

        // 待機時のフワフワ浮遊感
        float hover = (float) Math.sin((entity.tickCount + partialTick) * 0.15F + entity.getSlotIndex()) * 0.1F;
        poseStack.translate(0.0D, 0.4D + hover, 0.0D);

        // 向き
        poseStack.mulPose(Axis.YP.rotationDegrees(180.0F - entity.getYRot()));
        poseStack.mulPose(Axis.XP.rotationDegrees(entity.getXRot()));

        // 突撃中は剣先を前に向ける
        if (entity.getMinionState() == FactotumMinionEntity.State.ATTACKING) {
            poseStack.mulPose(Axis.XP.rotationDegrees(90.0F));
        } else {
            // 待機中は少し斜めに構える
            poseStack.mulPose(Axis.ZP.rotationDegrees((entity.getSlotIndex() % 2 == 0 ? 1 : -1) * 15.0F));
        }

        // サイズ調整
        poseStack.scale(1.4F, 1.4F, 1.4F);

        // アイテム本体の描画
        ItemStack stack = getItem();
        var itemRenderer = Minecraft.getInstance().getItemRenderer();
        BakedModel model = itemRenderer.getModel(stack, entity.level(), null, entity.getId());

        itemRenderer.render(
                stack,
                ItemDisplayContext.FIXED,
                false,
                poseStack,
                bufferSource,
                0xF000F0, // 発光（最大光度）
                OverlayTexture.NO_OVERLAY,
                model
        );

        poseStack.popPose();
    }
}
