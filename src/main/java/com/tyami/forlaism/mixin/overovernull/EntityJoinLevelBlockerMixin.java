package com.tyami.forlaism.mixin.overovernull;

import com.tyami.forlaism.erase.EraseRegistry;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 抹消済みEntityの再追加を、レベル側で直接阻止する。
 *
 * EntityJoinLevelEvent より早い段階で弾く。
 */
@Mixin(ServerLevel.class)
public abstract class EntityJoinLevelBlockerMixin {

    @Inject(
            method = "addEntity",
            at = @At("HEAD"),
            cancellable = true
    )
    private void forlaism$blockErasedEntity(
            Entity entity,
            CallbackInfoReturnable<Boolean> cir
    ) {
        if (entity == null) return;

        ServerLevel self = (ServerLevel) (Object) this;

        if (EraseRegistry.isErased(self.getServer(), entity.getUUID())) {
            // 追加を拒否
            cir.setReturnValue(false);
        }
    }

    @Inject(
            method = "addFreshEntity",
            at = @At("HEAD"),
            cancellable = true
    )
    private void forlaism$blockErasedFreshEntity(
            Entity entity,
            CallbackInfoReturnable<Boolean> cir
    ) {
        if (entity == null) return;

        ServerLevel self = (ServerLevel) (Object) this;

        if (EraseRegistry.isErased(self.getServer(), entity.getUUID())) {
            cir.setReturnValue(false);
        }
    }
}