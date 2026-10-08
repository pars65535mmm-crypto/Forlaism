package com.tyami.forlaism.mixin;

import com.tyami.forlaism.event.AlliedMobHandler;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * ネギ包丁で「永続敵対破棄」されたMob、
 * およびアンビシレーバーで「味方化」されたMobの制御。
 *
 * - ForlaismPacified: setTarget を完全無効化（置物になる）
 * - ForlaismAllied:  プレイヤーを狙わない
 *
 * 注意:
 *   Mob クラスには setLastHurtByMob / setLastHurtByPlayer / canAttack が
 *   定義されていない（LivingEntity 側にある）ため、
 *   それらへの @Inject はここでは行わない。
 */
@Mixin(Mob.class)
public abstract class MobPacifiedMixin {

    /** 永続敵対破棄タグのキー。 */
    private static final String PACIFIED_TAG = "ForlaismPacified";

    private boolean forlaism$isPacified() {
        Mob self = (Mob) (Object) this;
        return self.getPersistentData().getBoolean(PACIFIED_TAG);
    }

    private boolean forlaism$isAllied() {
        Mob self = (Mob) (Object) this;
        return self.getPersistentData().getBoolean(AlliedMobHandler.ALLIED_TAG);
    }

    /**
     * ネギ包丁で敵対破棄されたMobは setTarget を完全無効化。
     * 味方化Mobがプレイヤーを狙おうとしたら弾く。
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
            return;
        }

        // 味方化Mobのターゲット制御
        if (forlaism$isAllied()) {
            // プレイヤーは狙わない
            if (target instanceof Player) {
                ci.cancel();
                return;
            }
            // 他の味方化Mobも狙わない
            if (target instanceof Mob mob && AlliedMobHandler.isAllied(mob)) {
                ci.cancel();
            }
        }
    }
}