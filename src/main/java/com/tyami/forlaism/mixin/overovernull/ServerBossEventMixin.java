package com.tyami.forlaism.mixin.overovernull;

import com.tyami.forlaism.erase.EraseTracker;

import net.minecraft.server.level.ServerBossEvent;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * OverOverNull 実行中はボスバーの操作を素通しさせる。
 */
@Mixin(value = ServerBossEvent.class, priority = 30000)
public abstract class ServerBossEventMixin {

    @Inject(
            method = "setProgress",
            at = @At("HEAD"),
            cancellable = true
    )
    private void forlaism$overOverNullSetProgress(
            float progress,
            CallbackInfo ci
    ) {
        if (!EraseTracker.isExecuting()) return;

        // 実行中は強制的に進捗0
        if (progress > 0.0F) {
            // 元のsetProgressは呼ばれるが、EraseHelper側でremoveAllPlayersするのでOK
        }
    }
}