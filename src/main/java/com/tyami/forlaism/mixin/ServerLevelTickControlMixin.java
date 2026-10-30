package com.tyami.forlaism.mixin;

import com.tyami.forlaism.util.ServerLevelTickControlGuard;  // ← 追加
import com.tyami.forlaism.world.WorldTimeFreezeManager;
import net.minecraft.server.level.ServerLevel;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.function.BooleanSupplier;

@Mixin(ServerLevel.class)
public abstract class ServerLevelTickControlMixin {

    @Inject(
            method = "tick",
            at = @At("HEAD"),
            cancellable = true
    )
    private void forlaism$tickControl(BooleanSupplier hasTimeLeft, CallbackInfo ci) {

        if (ServerLevelTickControlGuard.isActive()) {
            return;
        }

        int count = WorldTimeFreezeManager.computeTickCount();

        if (count == 1) return;

        if (count <= 0) {
            ci.cancel();
            return;
        }

        ci.cancel();

        ServerLevel self = (ServerLevel) (Object) this;

        ServerLevelTickControlGuard.BEGIN();
        try {
            for (int i = 0; i < count; i++) {
                self.tick(hasTimeLeft);
            }
        } catch (Throwable t) {
            System.err.println("[Forlaism] ServerLevel.tick() failed: " + t);
        } finally {
            ServerLevelTickControlGuard.END();
        }
    }
}