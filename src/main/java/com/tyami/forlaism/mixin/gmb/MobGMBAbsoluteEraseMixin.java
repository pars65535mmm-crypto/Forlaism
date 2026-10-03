package com.tyami.forlaism.mixin.gmb;

import com.tyami.forlaism.annihilation.GMBEraseRegistry;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Mob固有のAI/攻撃経路をEntity.tickとは別に遮断する。 */
@Mixin(value = Mob.class, priority = 30000)
public abstract class MobGMBAbsoluteEraseMixin {

    private boolean forlaism$isGMBErased() {
        Mob self = (Mob) (Object) this;
        return self.level().getServer() != null
                && GMBEraseRegistry.isErased(self.level().getServer(), self.getUUID());
    }

    @Inject(method = "tick", at = @At("HEAD"), cancellable = true)
    private void forlaism$gmbStopMobTick(CallbackInfo ci) {
        Mob self = (Mob) (Object) this;
        if (forlaism$isGMBErased()
                || self.level() instanceof ServerLevel level
                && GMBEraseRegistry.isSuppressedReplacement(level, self, level.getGameTime())) ci.cancel();
    }

    @Inject(method = "serverAiStep", at = @At("HEAD"), cancellable = true)
    private void forlaism$gmbStopServerAi(CallbackInfo ci) {
        Mob self = (Mob) (Object) this;
        if (forlaism$isGMBErased()
                || self.level() instanceof ServerLevel level
                && GMBEraseRegistry.isSuppressedReplacement(level, self, level.getGameTime())) ci.cancel();
    }

    @Inject(method = "doHurtTarget", at = @At("HEAD"), cancellable = true)
    private void forlaism$gmbStopAttack(Entity target, CallbackInfoReturnable<Boolean> cir) {
        Mob self = (Mob) (Object) this;
        if (forlaism$isGMBErased()
                || self.level() instanceof ServerLevel level
                && GMBEraseRegistry.isSuppressedAttack(level, self, level.getGameTime())) {
            cir.setReturnValue(false);
        }
    }
}
