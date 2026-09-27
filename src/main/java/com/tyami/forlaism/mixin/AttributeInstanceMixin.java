package com.tyami.forlaism.mixin;

import com.tyami.forlaism.damage.RinneRecoveryBlocker;

import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 輪廻ダメージで MAX_HEALTH を固定された Entity の
 * setBaseValue() を完全に無効化する。
 *
 * これにより、どんな手段（コマンド・MOD・AttributeModifier等）でも
 * 最大HPが勝手に増減しなくなる。
 */
@Mixin(AttributeInstance.class)
public abstract class AttributeInstanceMixin {

    @Inject(
            method = "setBaseValue",
            at = @At("HEAD"),
            cancellable = true
    )
    private void forlaism$blockLockedMaxHealth(
            double value,
            CallbackInfo ci
    ) {
        AttributeInstance self = (AttributeInstance) (Object) this;

        // MAX_HEALTH 以外は素通し
        Attribute attr = self.getAttribute();
        if (attr != Attributes.MAX_HEALTH) {
            return;
        }

        // 輪廻で固定済みなら完全拒否
        if (RinneRecoveryBlocker.isMaxHealthLocked(self)) {
            ci.cancel();
        }
    }
}