package com.tyami.forlaism.mixin;

import com.tyami.forlaism.item.FistItem;

import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 拳による攻撃の無敵時間を完全に無効化する。
 *
 * priority = 10000:
 *   LivingEntityMixin (1000) より後
 *   Overnull (10000) と同じくらい
 *   他の防御Mixinより後に走らせる
 */
@Mixin(value = LivingEntity.class, priority = 10000)
public abstract class FistMixin {

    /**
     * 拳で殴られた場合、hurt() の最初で無敵時間を全リセット。
     *
     * これでバニラ側の「同じtick内では1回だけ」判定をすり抜けて
     * 毎回ダメージが通る。
     */
    @Inject(
            method = "hurt",
            at = @At("HEAD")
    )
    private void forlaism$fistResetInvul(
            DamageSource source,
            float amount,
            CallbackInfoReturnable<Boolean> cir
    ) {
        LivingEntity self = (LivingEntity) (Object) this;

        // クライアントは無視
        if (self.level().isClientSide) {
            return;
        }

        // 攻撃者を取得
        Entity attackerEntity = source.getEntity();
        if (!(attackerEntity instanceof Player player)) {
            return;
        }

        // メインハンド or オフハンドが拳かチェック
        ItemStack main = player.getMainHandItem();
        ItemStack off = player.getOffhandItem();

        boolean isFist = main.getItem() instanceof FistItem
                || off.getItem() instanceof FistItem;

        if (!isFist) {
            return;
        }

        // 無敵時間を完全リセット
        self.invulnerableTime = 0;
        self.hurtTime = 0;
        self.hurtDuration = 0;
    }
}