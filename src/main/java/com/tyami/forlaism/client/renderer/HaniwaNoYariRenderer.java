package com.tyami.forlaism.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.tyami.forlaism.entity.HaniwaNoYariProjectile;
import com.tyami.forlaism.registry.Items;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

/**
 * ハニワノヤリの投擲体レンダラ。
 *
 * - でかく（2.0倍）
 * - 進行方向に寝かせる
 * - 軸回転（回転しながら飛ぶ）
 * - 発光（FULL_BRIGHT）
 * - 残像（パーティクル）
 */
public class HaniwaNoYariRenderer extends EntityRenderer<HaniwaNoYariProjectile> {

    /** 発光用ライト値。 */
    private static final int FULL_BRIGHT = 0xF000F0;

    /** 表示スケール。でかく。 */
    private static final float SCALE = 2.0F;

    /** 回転速度（度/tick）。 */
    private static final float SPIN_SPEED = 35.0F;

    private ItemStack cachedStack;

    public HaniwaNoYariRenderer(EntityRendererProvider.Context ctx) {
        super(ctx);
    }

    @Override
    public ResourceLocation getTextureLocation(HaniwaNoYariProjectile entity) {
        return InventoryMenu.BLOCK_ATLAS;
    }

    @Override
    public void render(
            HaniwaNoYariProjectile entity,
            float entityYaw,
            float partialTick,
            PoseStack poseStack,
            MultiBufferSource buffer,
            int packedLight
    ) {
        super.render(entity, entityYaw, partialTick, poseStack, buffer, packedLight);

        if (cachedStack == null) {
            cachedStack = new ItemStack(Items.HANIWA_NO_YARI.get());
        }

        poseStack.pushPose();

        // =========================================================
        // 1. 進行方向に向ける
        // =========================================================
        poseStack.mulPose(Axis.YP.rotationDegrees(-entityYaw));
        poseStack.mulPose(Axis.XP.rotationDegrees(entity.getXRot()));

        // =========================================================
        // 2. 槍を進行方向に寝かせる（槍の軸をZ方向に）
        // =========================================================
        poseStack.mulPose(Axis.XP.rotationDegrees(90.0F));

        // =========================================================
        // 3. 軸回転（回転しながら飛ぶ）
        // =========================================================
        float spin = (entity.tickCount + partialTick) * SPIN_SPEED;
        poseStack.mulPose(Axis.ZP.rotationDegrees(spin));

        // =========================================================
        // 4. でかく
        // =========================================================
        poseStack.scale(SCALE, SCALE, SCALE);

        // =========================================================
        // 5. 描画（FULL_BRIGHT で発光）
        // =========================================================
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