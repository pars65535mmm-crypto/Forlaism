package com.tyami.forlaism.mixin.gmb;

import com.tyami.forlaism.annihilation.GMBEraseRegistry;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** EntityTickListから直接呼ばれる経路も止める。 */
@Mixin(value = ServerLevel.class, priority = 30000)
public abstract class ServerLevelGMBTickBlockMixin {

    @Inject(method = "tickNonPassenger", at = @At("HEAD"), cancellable = true)
    private void forlaism$gmbBlockTick(Entity entity, CallbackInfo ci) {
        ServerLevel level = (ServerLevel) (Object) this;
        if (entity != null && GMBEraseRegistry.isErased(level.getServer(), entity.getUUID())) {
            ci.cancel();
        }
    }
}
