package com.tyami.forlaism.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import com.tyami.forlaism.client.renderer.MahouTsukaiNoBoushiArmorRenderer;
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
 * 防具レイヤー描画の差し替え。
 *
 * - 眷属の光輪のときはバニラ描画をスキップ（MinionHaloHeadRenderer が担当）
 * - 魔法使いの帽子のときはバニラ描画をスキップして、
 *   代わりにアイテムモデルを頭に描画する
 */
@Mixin(HumanoidArmorLayer.class)
public abstract class HumanoidArmorLayerMixin {

    @Inject(
            method = "renderArmorPiece",
            at = @At("HEAD"),
            cancellable = true
    )
    private void forlaism$skipCustomHelmetArmor(
            PoseStack poseStack,
            MultiBufferSource buffer,
            LivingEntity entity,
            EquipmentSlot slot,
            int packedLight,
            HumanoidModel<?> model,
            CallbackInfo ci
    ) {
        // 頭スロット以外は無視
        if (slot != EquipmentSlot.HEAD) return;

        ItemStack stack = entity.getItemBySlot(slot);
        if (stack.isEmpty()) return;

        // =========================================================
        // 眷属の光輪 → バニラ描画スキップ（別レイヤーが担当）
        // =========================================================
        if (stack.is(Items.MINION_HALO.get())) {
            ci.cancel();
            return;
        }

        // =========================================================
        // 魔法使いの帽子 → バニラ描画スキップ + 自前描画
        // =========================================================
        if (stack.is(Items.MAHOUTSUKAI_NO_BOUSHI.get())) {
            // まずバニラ描画をキャンセル
            ci.cancel();

            // 自前でアイテムモデルを頭に描画
            MahouTsukaiNoBoushiArmorRenderer.render(
                    poseStack,
                    model,
                    entity,
                    buffer,
                    packedLight
            );
        }
    }
}