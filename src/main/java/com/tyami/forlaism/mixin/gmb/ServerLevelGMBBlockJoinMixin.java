package com.tyami.forlaism.mixin.gmb;

import com.tyami.forlaism.annihilation.GMBEraseRegistry;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * GMB消去済みEntityのワールド追加を完全に阻止する。
 *
 * priority = 30000 で最優先。
 */
@Mixin(value = ServerLevel.class, priority = 30000)
public abstract class ServerLevelGMBBlockJoinMixin {

    @Inject(method = "addEntity", at = @At("HEAD"), cancellable = true)
    private void forlaism$gmbBlockAddEntity(Entity entity, CallbackInfoReturnable<Boolean> cir) {
        if (entity == null) return;
        ServerLevel self = (ServerLevel) (Object) this;
        if (GMBEraseRegistry.isErased(self.getServer(), entity.getUUID())) {
            // 追加拒否 + 物理削除
            com.tyami.forlaism.annihilation.GMBAnnihilation.forceRemove(self, entity);
            cir.setReturnValue(false);
        }
    }

    @Inject(method = "addFreshEntity", at = @At("HEAD"), cancellable = true)
    private void forlaism$gmbBlockAddFreshEntity(Entity entity, CallbackInfoReturnable<Boolean> cir) {
        if (entity == null) return;
        ServerLevel self = (ServerLevel) (Object) this;
        if (GMBEraseRegistry.isErased(self.getServer(), entity.getUUID())) {
            com.tyami.forlaism.annihilation.GMBAnnihilation.forceRemove(self, entity);
            cir.setReturnValue(false);
        }
    }
}