package com.tyami.forlaism.mixin;

import com.tyami.forlaism.world.WorldTimeFreezeManager;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 全世界時間停止発動中、Entityのtick()をプレイヤー以外停止するMixin。
 * 落下や移動慣性もゼロに固定する。
 */
@Mixin(value = Entity.class, priority = 31000)
public abstract class EntityTimeFreezeMixin {

    @Inject(method = "tick", at = @At("HEAD"), cancellable = true)
    private void forlaism$timeFreezeEntityTick(CallbackInfo ci) {
        Entity self = (Entity) (Object) this;

        if (self instanceof Player) {
            return;
        }

        if (WorldTimeFreezeManager.isFrozen()) {
            self.setDeltaMovement(0, 0, 0);
            ci.cancel();
        }
    }
}
