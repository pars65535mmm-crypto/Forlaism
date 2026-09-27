package com.tyami.forlaism.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import com.tyami.forlaism.registry.Items;

import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.layers.HumanoidArmorLayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 眷属の光輪の時、バニラ防具レイヤー描画をスキップする。
 *
 * 代わりに MinionHaloHeadRenderer が描画する。
 */
@Mixin(HumanoidArmorLayer.class)
public abstract class HumanoidArmorLayerMixin {

    @Inject(
            method = "renderArmorPiece",
            at = @At("HEAD"),
            cancellable = true
    )
    private void forlaism$skipMinionHaloArmor(
            PoseStack poseStack,
            MultiBufferSource buffer,
            LivingEntity entity,
            EquipmentSlot slot,
            int packedLight,
            HumanoidModel<?> model,
            CallbackInfo ci
    ) {
        if (slot != EquipmentSlot.HEAD) return;

        ItemStack stack = entity.getItemBySlot(slot);
        if (stack.isEmpty()) return;

        // 眷属の光輪の時はバニラ防具描画をスキップ
        if (stack.is(Items.MINION_HALO.get())) {
            ci.cancel();
        }
    }
}