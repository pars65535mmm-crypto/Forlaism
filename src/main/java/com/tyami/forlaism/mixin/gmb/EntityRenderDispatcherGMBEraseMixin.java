package com.tyami.forlaism.mixin.gmb;

import com.mojang.blaze3d.vertex.PoseStack;
import com.tyami.forlaism.annihilation.GMBEraseRegistry;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** 消去済みEntityが管理側に一瞬残っても描画しない最後の防衛線。 */
@Mixin(value = EntityRenderDispatcher.class, priority = 30000)
public abstract class EntityRenderDispatcherGMBEraseMixin {

    @Inject(method = "render(Lnet/minecraft/world/entity/Entity;DDDFFLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V",
            at = @At("HEAD"), cancellable = true)
    private <E extends Entity> void forlaism$gmbHideErased(E entity, double x, double y, double z,
                                                            float yaw, float partialTick,
                                                            PoseStack poseStack, MultiBufferSource buffers,
                                                            int packedLight, CallbackInfo ci) {
        if (GMBEraseRegistry.isErasedClient(entity.getUUID())) ci.cancel();
    }
}
