package com.tyami.forlaism.mixin;

import com.tyami.forlaism.event.OnsenEffectHandler;
import com.tyami.forlaism.world.EntityFreezeManager;

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

    /**
     * 凍結中のMobは攻撃できない。
     *
     * EntityFreezeMixin は Entity.tick() を止めているが、
     * doHurtTarget は別経路 (AI Goal 等) から呼ばれることがあるため、
     * ここでも念のためガードする。
     */
    @Inject(
            method = "doHurtTarget",
            at = @At("HEAD"),
            cancellable = true
    )
    private void forlaism$cancelFrozenAttack(
            Entity target,
            CallbackInfoReturnable<Boolean> cir
    ) {

        Mob self = (Mob) (Object) this;

        // 自分が凍結中なら攻撃不可
        if (EntityFreezeManager.isFrozen(self)) {
            cir.setReturnValue(false);
            return;
        }

        // 相手が凍結中なら一方的に殴れない
        if (target != null && EntityFreezeManager.isFrozen(target)) {
            cir.setReturnValue(false);
        }
    }
}