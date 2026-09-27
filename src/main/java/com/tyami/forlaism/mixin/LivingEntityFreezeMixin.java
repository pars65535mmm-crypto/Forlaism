package com.tyami.forlaism.mixin;

import com.tyami.forlaism.world.EntityFreezeManager;

import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 凍結中の LivingEntity は一切のダメージを受けない。
 *
 * priority = 2500:
 *   LivingEntityMixin (priority = 1000) や
 *   LivingEntitySwrequimetMixin (priority = 2000) より後に実行される。
 *
 * そのため、他の Mixin が hurt() を確定させた後でも、
 * ここで強制的にダメージを拒否できる。
 */
@Mixin(value = LivingEntity.class, priority = 2500)
public abstract class LivingEntityFreezeMixin {

    @Inject(
            method = "hurt",
            at = @At("HEAD"),
            cancellable = true
    )
    private void forlaism$blockDamageIfFrozen(
            DamageSource source,
            float amount,
            CallbackInfoReturnable<Boolean> cir
    ) {

        LivingEntity self = (LivingEntity) (Object) this;

        // クライアント側は無視
        if (self.level().isClientSide) {
            return;
        }

        // 凍結中は一切のダメージを拒否
        if (EntityFreezeManager.isFrozen(self)) {
            cir.setReturnValue(false);
        }
    }
}