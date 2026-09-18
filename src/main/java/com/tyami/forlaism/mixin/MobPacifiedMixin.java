package com.tyami.forlaism.mixin;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * ネギ包丁で「永続敵対破棄」されたMobは、
 * setTarget を完全に無効化され、一切ターゲットを取れなくなる。
 */
@Mixin(Mob.class)
public abstract class MobPacifiedMixin {

    /** 永続敵対破棄タグのキー。 */
    private static final String PACIFIED_TAG = "ForlaismPacified";

    /**
     * このMobが永続敵対破棄されているか判定する。
     */
    private boolean forlaism$isPacified() {
        Mob self = (Mob) (Object) this;
        return self.getPersistentData().getBoolean(PACIFIED_TAG);
    }

    /**
     * setTarget を完全に無効化。
     * これでAIがどんなに頑張ってもターゲットをセットできない。
     */
    @Inject(
            method = "setTarget(Lnet/minecraft/world/entity/LivingEntity;)V",
            at = @At("HEAD"),
            cancellable = true
    )
    private void forlaism$blockSetTarget(
            LivingEntity target,
            CallbackInfo ci
    ) {
        if (forlaism$isPacified()) {
            ci.cancel();
        }
    }
}