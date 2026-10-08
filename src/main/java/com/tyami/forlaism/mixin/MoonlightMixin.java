package com.tyami.forlaism.mixin;

import com.tyami.forlaism.damage.MoonlightDamageSource;

import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 月光ダメージの実処理。
 *
 * priority = 3000:
 *   LivingEntityMixin (1000) より後
 *   LivingEntityRinneMixin (2600) より後
 *   LivingEntitySwrequimetMixin (2000) より後
 *
 * あらゆる防御を貫通してHPを直接削る。
 */
@Mixin(value = LivingEntity.class, priority = 3000)
public abstract class MoonlightMixin {

    // 通常ダメージ1を再通入させる際、無限ループになるのを防ぐためのフラグ
    @Unique
    private boolean forlaism$isProcessingMoonlight = false;

    @Inject(
            method = "hurt",
            at = @At("HEAD"),
            cancellable = true
    )
    private void forlaism$moonlightHurt(
            DamageSource source,
            float amount,
            CallbackInfoReturnable<Boolean> cir
    ) {
        LivingEntity self = (LivingEntity) (Object) this;

        // クライアント除外
        if (self.level().isClientSide) {
            return;
        }

        // 月光以外は素通し
        if (!source.is(MoonlightDamageSource.MOONLIGHT)) {
            return;
        }

        // 通常ダメージ1の通入中なら、これ以上このMixinの処理は行わずバニラに任せる
        if (this.forlaism$isProcessingMoonlight) {
            return;
        }

        // 自傷除外
        if (source.getEntity() == self) {
            cir.setReturnValue(false);
            return;
        }

        // クリエイティブ除外
        if (self instanceof Player p && p.isCreative()) {
            cir.setReturnValue(false);
            return;
        }

        // =========================================================
        // 防御貫通: HPを直接削る
        // =========================================================
        self.invulnerableTime = 0;
        self.hurtTime = 10;
        self.hurtDuration = 10;

        // プレイヤーキル判定やドロップを出すため、最低「1.0F」の通常ダメージをバニラに残す
        float normalDamage = 1.0F;
        float directDamage = amount - normalDamage;

        if (directDamage > 0.0F) {
            // 1ダメージ分を残してHPを直接削る（最低でもHPは 0.0F より上で止まるようにする）
            float newHealth = Math.max(0.001F, self.getHealth() - directDamage);
            self.setHealth(newHealth);
        }

        if (source.getEntity() instanceof Player p) {
            self.setLastHurtByPlayer(p);
        }

        // 💡 ここがポイント：
        // 残った「1.0F」分のダメージを、バニラの hurt メソッドに流し直します。
        // これにより、死亡時も含めてバニラの正常なドロップ処理や実績解除がトリガーされます。
        try {
            this.forlaism$isProcessingMoonlight = true;
            boolean result = self.hurt(source, normalDamage);
            cir.setReturnValue(result); // バニラ側の処理結果を返す
        } finally {
            this.forlaism$isProcessingMoonlight = false;
        }
    }
}
