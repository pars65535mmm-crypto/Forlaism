package com.tyami.forlaism.mixin;

import com.tyami.forlaism.world.EntityFreezeManager;

import net.minecraft.world.entity.Entity;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 凍結中の entity の tick() をスキップする。
 *
 * ポイント:
 *   - tick() 本体は実行しない → AI / 移動 / 重力 / 当たり判定 全て停止
 *   - tickCount だけは進める → 見た目のアニメーションやパーティクルは進む
 *   - 解除後の tickCount ズレを防ぐ
 *   - ダメージ拒否は LivingEntityFreezeMixin (priority=2500) 側で実施
 */
@Mixin(Entity.class)
public abstract class EntityFreezeMixin {

    @Shadow
    public int tickCount;

    /**
     * tick() の先頭で凍結判定。
     */
    @Inject(
            method = "tick",
            at = @At("HEAD"),
            cancellable = true
    )
    private void forlaism$skipTickIfFrozen(CallbackInfo ci) {

        Entity self = (Entity) (Object) this;

        // クライアントは通常通り
        if (self.level() == null || self.level().isClientSide) {
            return;
        }

        if (!EntityFreezeManager.isFrozen(self)) {
            return;
        }

        // 内部時間だけは進める
        this.tickCount++;

        // 移動・当たり判定も停止
        self.setDeltaMovement(0, 0, 0);

        // tick 本体を完全キャンセル
        ci.cancel();
    }
}