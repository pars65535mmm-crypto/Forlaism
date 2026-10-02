package com.tyami.forlaism.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import com.tyami.forlaism.client.gmb.GMBRenderer;
import com.tyami.forlaism.client.magiceffect.MagicItemEffectRenderer;
import com.tyami.forlaism.registry.Items;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ItemRenderer.class)
public abstract class ItemRendererMixin {

    @Inject(
            method = "renderStatic(Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/item/ItemDisplayContext;ZLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;Lnet/minecraft/world/level/Level;III)V",
            at = @At("RETURN")
    )
    private void forlaism$onRenderStatic(
            LivingEntity entity, ItemStack stack, ItemDisplayContext context,
            boolean leftHand, PoseStack poseStack, MultiBufferSource buffer,
            Level level, int combinedLight, int combinedOverlay, int seed,
            CallbackInfo ci
    ) {
        if (stack.isEmpty()) return;

        // GMB専用シェーダー
        if (stack.is(Items.GAMING_MASTER_BLADE.get())) {
            if (context != ItemDisplayContext.GUI && context != ItemDisplayContext.GROUND) {
                GMBRenderer.render(stack, poseStack, buffer, true);
            }
            return;
        }

        // 既存の魔法エフェクト
        if (context != ItemDisplayContext.GUI && context != ItemDisplayContext.GROUND) {
            MagicItemEffectRenderer.renderWorld(stack, poseStack, buffer, true);
        }
    }
}