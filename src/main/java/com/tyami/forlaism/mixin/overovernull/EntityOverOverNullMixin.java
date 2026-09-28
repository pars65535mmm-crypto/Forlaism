package com.tyami.forlaism.mixin.overovernull;

import com.tyami.forlaism.erase.EraseTracker;

import net.minecraft.world.entity.Entity;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * OverOverNull 実行中は discard / remove を素通しさせる。
 *
 * priority = 30000 で全防御を突破。
 */
@Mixin(value = Entity.class, priority = 30000)
public abstract class EntityOverOverNullMixin {

    @Inject(
            method = "discard",
            at = @At("HEAD"),
            cancellable = true
    )
    private void forlaism$overOverNullDiscard(
            CallbackInfo ci
    ) {
        if (!EraseTracker.isExecuting()) return;
        // 素通し（キャンセルしない）
    }

    @Inject(
            method = "remove",
            at = @At("HEAD"),
            cancellable = true
    )
    private void forlaism$overOverNullRemove(
            Entity.RemovalReason reason,
            CallbackInfo ci
    ) {
        if (!EraseTracker.isExecuting()) return;
        // 素通し
    }

    @Inject(
            method = "setRemoved",
            at = @At("HEAD"),
            cancellable = true
    )
    private void forlaism$overOverNullSetRemoved(
            Entity.RemovalReason reason,
            CallbackInfo ci
    ) {
        if (!EraseTracker.isExecuting()) return;
        // 素通し
    }
}