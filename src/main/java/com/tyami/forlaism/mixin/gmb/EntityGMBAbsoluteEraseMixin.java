package com.tyami.forlaism.mixin.gmb;

import com.tyami.forlaism.annihilation.GMBEraseRegistry;

import net.minecraft.world.entity.Entity;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * GMB消去済みEntityは何をしても復活できない。
 *
 * priority = 30000 で他の全Mixinより後に走らせる。
 */
@Mixin(value = Entity.class, priority = 30000)
public abstract class EntityGMBAbsoluteEraseMixin {

    private boolean forlaism$isGMBErased() {
        Entity self = (Entity) (Object) this;
        if (self.level() == null) return false;
        if (self.level().getServer() == null) return false;
        return GMBEraseRegistry.isErased(self.level().getServer(), self.getUUID());
    }

    @Inject(method = "setRemoved", at = @At("HEAD"), cancellable = true)
    private void forlaism$gmbSetRemoved(Entity.RemovalReason reason, CallbackInfo ci) {
        if (forlaism$isGMBErased()) {
            // 素通し（消えるのは許可）
        }
    }

    @Inject(method = "discard", at = @At("HEAD"), cancellable = true)
    private void forlaism$gmbDiscard(CallbackInfo ci) {
        if (forlaism$isGMBErased()) {
            // 素通し
        }
    }

    @Inject(method = "remove", at = @At("HEAD"), cancellable = true)
    private void forlaism$gmbRemove(Entity.RemovalReason reason, CallbackInfo ci) {
        if (forlaism$isGMBErased()) {
            // 素通し
        }
    }

    @Inject(method = "kill", at = @At("HEAD"), cancellable = true)
    private void forlaism$gmbKill(CallbackInfo ci) {
        if (forlaism$isGMBErased()) {
            // 素通し（死ぬのは許可）
        }
    }

    /**
     * GMB消去済みEntityの teleportTo をブロック。
     */
    @Inject(method = "teleportTo(DDD)V", at = @At("HEAD"), cancellable = true)
    private void forlaism$gmbBlockTeleport(double x, double y, double z, CallbackInfo ci) {
        if (forlaism$isGMBErased()) {
            ci.cancel();
        }
    }

    @Inject(method = "teleportRelative(DDD)V", at = @At("HEAD"), cancellable = true)
    private void forlaism$gmbBlockRelativeTeleport(double x, double y, double z, CallbackInfo ci) {
        if (forlaism$isGMBErased()) {
            ci.cancel();
        }
    }

    /**
     * GMB消去済みEntityの startRiding をブロック。
     *
     * ※ startRiding は Entity 側のメソッド。
     *    LivingEntity には存在しないので、こちらに置く。
     */
    @Inject(method = "startRiding(Lnet/minecraft/world/entity/Entity;Z)Z",
            at = @At("HEAD"), cancellable = true)
    private void forlaism$gmbBlockRiding(Entity vehicle, boolean force,
                                         CallbackInfoReturnable<Boolean> cir) {
        if (forlaism$isGMBErased()) {
            cir.setReturnValue(false);
        }
    }
}