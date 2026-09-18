package com.tyami.forlaism.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import com.tyami.forlaism.client.magiceffect.MagicItemEffectRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.ItemEntityRenderer;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ItemEntityRenderer.class)
public abstract class ItemEntityRendererMixin {

    @Unique
    private static final ThreadLocal<Integer> forlaism$lastRenderedEntityId = ThreadLocal.withInitial(() -> -1);

    @Inject(
            method = "render(Lnet/minecraft/world/entity/item/ItemEntity;FFLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V",
            at = @At("HEAD")
    )
    private void forlaism$onRenderHead(ItemEntity entity, float entityYaw, float partialTicks, PoseStack poseStack, MultiBufferSource buffer, int packedLight, CallbackInfo ci) {
        forlaism$lastRenderedEntityId.set(-1);
    }

    @Inject(
            method = "render(Lnet/minecraft/world/entity/item/ItemEntity;FFLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/renderer/entity/ItemRenderer;render(Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/item/ItemDisplayContext;ZLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;IILnet/minecraft/client/resources/model/BakedModel;)V",
                    shift = At.Shift.AFTER
            )
    )
    private void forlaism$onRenderItemEntity(ItemEntity entity, float entityYaw, float partialTicks, PoseStack poseStack, MultiBufferSource buffer, int packedLight, CallbackInfo ci) {
        // 束ねられたアイテム（複数描画）の場合、最初の1回だけエフェクトを描画して重複描画を防ぐ
        if (forlaism$lastRenderedEntityId.get() == entity.getId()) {
            return;
        }
        forlaism$lastRenderedEntityId.set(entity.getId());

        ItemStack stack = entity.getItem();
        if (!stack.isEmpty()) {
            // アイテム描画のローカル位置でカメラビルボードエフェクトを描画
            MagicItemEffectRenderer.renderWorld(stack, poseStack, buffer, true);
        }
    }
}
