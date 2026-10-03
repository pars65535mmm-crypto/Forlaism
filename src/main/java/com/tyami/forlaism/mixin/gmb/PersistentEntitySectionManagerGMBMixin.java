package com.tyami.forlaism.mixin.gmb;

import com.tyami.forlaism.annihilation.GMBEraseRegistry;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.entity.EntityAccess;
import net.minecraft.world.level.entity.PersistentEntitySectionManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Entityを管理する最後の層そのものへの再登録阻止。 */
@Mixin(value = PersistentEntitySectionManager.class, priority = 30000)
public abstract class PersistentEntitySectionManagerGMBMixin {
    private static boolean forlaism$isBlocked(EntityAccess access) {
        if (!(access instanceof Entity entity) || !(entity.level() instanceof ServerLevel level)) return false;
        return GMBEraseRegistry.isSuppressedReplacement(level, entity, level.getGameTime());
    }

    @Inject(method = "addEntityUuid", at = @At("HEAD"), cancellable = true)
    private void forlaism$blockUuid(EntityAccess access, CallbackInfoReturnable<Boolean> cir) {
        if (forlaism$isBlocked(access)) cir.setReturnValue(false);
    }

    @Inject(method = "addNewEntity", at = @At("HEAD"), cancellable = true)
    private void forlaism$blockNew(EntityAccess access, CallbackInfoReturnable<Boolean> cir) {
        if (forlaism$isBlocked(access)) cir.setReturnValue(false);
    }

    @Inject(method = "addEntity", at = @At("HEAD"), cancellable = true)
    private void forlaism$blockAdd(EntityAccess access, boolean worldGen, CallbackInfoReturnable<Boolean> cir) {
        if (forlaism$isBlocked(access)) cir.setReturnValue(false);
    }

    @Inject(method = "startTicking", at = @At("HEAD"), cancellable = true)
    private void forlaism$blockStartTick(EntityAccess access, CallbackInfo ci) {
        if (forlaism$isBlocked(access)) ci.cancel();
    }

    @Inject(method = "startTracking", at = @At("HEAD"), cancellable = true)
    private void forlaism$blockStartTracking(EntityAccess access, CallbackInfo ci) {
        if (forlaism$isBlocked(access)) ci.cancel();
    }
}
