package com.tyami.forlaism.mixin;

import com.tyami.forlaism.client.gmb.GMBRenderer;
import com.tyami.forlaism.client.magiceffect.MagicItemEffectRenderer;
import com.tyami.forlaism.registry.Items;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GuiGraphics.class)
public abstract class GuiGraphicsMixin {

    @Inject(
            method = "renderItem(Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/world/level/Level;Lnet/minecraft/world/item/ItemStack;IIII)V",
            at = @At("HEAD")
    )
    private void forlaism$onRenderItem(
            LivingEntity entity, Level level, ItemStack stack,
            int x, int y, int seed, int guiOffset, CallbackInfo ci
    ) {
        if (stack.isEmpty()) return;

        if (stack.is(Items.GAMING_MASTER_BLADE.get())) {
            // GUI内は poseStack + MultiBufferSource が必要だが、
            // GuiGraphics の renderItem は内部で独自バッファを使うため
            // ここでは既存の MagicItemEffectRenderer.renderGui に委譲する形でもOK。
            // 一旦、GMBRenderer を GUI でも使えるように軽量版で呼ぶ。
            MagicItemEffectRenderer.renderGui((GuiGraphics) (Object) this, stack, x, y);
            return;
        }

        MagicItemEffectRenderer.renderGui((GuiGraphics) (Object) this, stack, x, y);
    }
}