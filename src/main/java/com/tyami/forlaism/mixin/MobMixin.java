package com.tyami.forlaism.mixin;

import com.tyami.forlaism.event.OnsenEffectHandler;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Mob.class)
public abstract class MobMixin {

    /**
     * 温泉に浸かっているMobは
     * 攻撃そのものを実行できない。
     */
    @Inject(
            method = "doHurtTarget",
            at = @At("HEAD"),
            cancellable = true
    )
    private void forlaism$cancelOnsenAttack(
            Entity target,
            CallbackInfoReturnable<Boolean> cir
    ) {

        Mob self =
                (Mob) (Object) this;

        /*
         * 攻撃する側が温泉に入っている
         */
        if (OnsenEffectHandler.isInOnsen(self)) {
            cir.setReturnValue(false);
            return;
        }

        /*
         * 攻撃される側が温泉に入っている
         */
        if (target instanceof LivingEntity living
                && OnsenEffectHandler.isInOnsen(living)) {

            cir.setReturnValue(false);
        }
    }
}