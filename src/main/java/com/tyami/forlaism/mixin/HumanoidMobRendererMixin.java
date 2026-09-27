package com.tyami.forlaism.mixin;

import com.tyami.forlaism.client.renderer.MinionHaloHeadRenderer;

import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.world.entity.LivingEntity;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 全てのLivingEntityRendererに対して、
 * MinionHaloHeadRenderer を後付けで追加する。
 *
 * HumanoidModel を使うレンダラー（= 人型Mob・プレイヤー）のみ対象。
 */
@Mixin(net.minecraft.client.renderer.entity.HumanoidMobRenderer.class)
public abstract class HumanoidMobRendererMixin<T extends net.minecraft.world.entity.Mob, M extends HumanoidModel<T>> {

    @Inject(method = "<init>", at = @At("RETURN"))
    private void forlaism$addMinionHaloLayer(CallbackInfo ci) {
        ((net.minecraft.client.renderer.entity.LivingEntityRenderer<T, M>) (Object) this)
                .addLayer(new MinionHaloHeadRenderer<>(
                        (net.minecraft.client.renderer.entity.RenderLayerParent<T, M>) (Object) this
                ));
    }
}