package com.tyami.forlaism.mixin;

import com.tyami.forlaism.world.WorldTimeFreezeManager;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 全世界時間停止発動中、LevelのtickBlockEntities()を停止するMixin。
 * これにより全BlockEntity（かまど、工業機械、ホッパーなど）の動作が完全に停止する。
 */
@Mixin(value = Level.class, priority = 31000)
public abstract class LevelTimeFreezeMixin {

    @Inject(method = "tickBlockEntities", at = @At("HEAD"), cancellable = true)
    private void forlaism$timeFreezeBlockEntities(CallbackInfo ci) {
        if (WorldTimeFreezeManager.isFrozen()) {
            ci.cancel();
        }
    }
}
