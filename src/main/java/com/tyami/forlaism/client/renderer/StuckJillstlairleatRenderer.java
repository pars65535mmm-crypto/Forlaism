package com.tyami.forlaism.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.tyami.forlaism.entity.StuckJillstlairleatEntity;
import com.tyami.forlaism.registry.Items;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

public class StuckJillstlairleatRenderer extends EntityRenderer<StuckJillstlairleatEntity> {

    private static final int FULL_BRIGHT = 0xF000F0;
    private ItemStack cachedStack;

    public StuckJillstlairleatRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public ResourceLocation getTextureLocation(StuckJillstlairleatEntity entity) {
        return InventoryMenu.BLOCK_ATLAS;
    }

    @Override
    public void render(
            StuckJillstlairleatEntity entity,
            float entityYaw,
            float partialTick,
            PoseStack poseStack,
            MultiBufferSource buffer,
            int packedLight
    ) {
        super.render(entity, entityYaw, partialTick, poseStack, buffer, packedLight);

        if (cachedStack == null) {
            cachedStack = new ItemStack(Items.JILLSTLAIRLEAT.get());
        }

        poseStack.pushPose();

        int ticks = entity.getAnimTicks();
        float currentTick = ticks + partialTick;

        if (!entity.isStuck()) {
            if (ticks < 60) {
                // 0 ~ 3秒: 乱回転
                poseStack.mulPose(Axis.XP.rotationDegrees(currentTick * 33.0F));
                poseStack.mulPose(Axis.YP.rotationDegrees(currentTick * 47.0F));
                poseStack.mulPose(Axis.ZP.rotationDegrees(currentTick * 29.0F));
            } else if (ticks < 120) {
                // 3 ~ 6秒: 超高速乱回転＋発光
                poseStack.mulPose(Axis.XP.rotationDegrees(currentTick * 60.0F));
                poseStack.mulPose(Axis.YP.rotationDegrees(currentTick * 85.0F));
                poseStack.mulPose(Axis.ZP.rotationDegrees(currentTick * 52.0F));
            } else {
                // 6 ~ 9秒: 抜け出し浮遊＋姿勢調整
                float progress = (ticks - 120 + partialTick) / 60.0F;
                poseStack.mulPose(Axis.YP.rotationDegrees(currentTick * 20.0F));
                poseStack.mulPose(Axis.ZP.rotationDegrees(45.0F + (1.0F - progress) * 180.0F));
            }
        } else {
            // 刺さった後: 地面に45度傾いて固定
            poseStack.translate(0.0, -0.2, 0.0);
            poseStack.mulPose(Axis.ZP.rotationDegrees(135.0F));
            poseStack.mulPose(Axis.YP.rotationDegrees(45.0F));
        }

        poseStack.scale(1.2F, 1.2F, 1.2F);

        int renderLight = (ticks >= 60 || entity.isStuck()) ? FULL_BRIGHT : packedLight;

        Minecraft.getInstance().getItemRenderer().renderStatic(
                cachedStack,
                ItemDisplayContext.FIXED,
                renderLight,
                OverlayTexture.NO_OVERLAY,
                poseStack,
                buffer,
                entity.level(),
                entity.getId()
        );

        poseStack.popPose();
    }
}
