package com.tyami.forlaism.mixin.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.tyami.forlaism.item.FistItem;

import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.ItemInHandRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.item.ItemStack;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 拳を持っている間、一人称の手の描画をスキップする。
 *
 * これで「何も持っていない素手」の見た目になる。
 */
@Mixin(ItemInHandRenderer.class)
public abstract class FistHandRenderMixin {

    @Inject(
            method = "renderArmWithItem",
            at = @At("HEAD"),
            cancellable = true
    )
    private void forlaism$hideFist(
            net.minecraft.client.player.AbstractClientPlayer player,
            float partialTicks,
            float pitch,
            InteractionHand hand,
            float swingProgress,
            ItemStack stack,
            float equippedProgress,
            PoseStack poseStack,
            MultiBufferSource buffer,
            int combinedLight,
            CallbackInfo ci
    ) {
        // 拳を持っていたら描画スキップ（= 素手の見た目）
        if (stack.getItem() instanceof FistItem) {
            ci.cancel();
        }
    }
}