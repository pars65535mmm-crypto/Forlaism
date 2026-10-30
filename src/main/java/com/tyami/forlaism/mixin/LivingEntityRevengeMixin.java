package com.tyami.forlaism.mixin;

import com.tyami.forlaism.damage.RevengeDamageSource;
import com.tyami.forlaism.damage.RevengeTracker;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * リベンジダメージの実処理 Mixin。
 *
 * 優先度 2700 で輪廻 (2600) や Halo より後に走らせ、
 * 最上位のダメージ貫通処理を行う。
 */
@Mixin(value = LivingEntity.class, priority = 2700)
public abstract class LivingEntityRevengeMixin {

    @Inject(method = "hurt", at = @At("HEAD"), cancellable = true)
    private void forlaism$revengeHurt(
            DamageSource source,
            float amount,
            CallbackInfoReturnable<Boolean> cir
    ) {
        LivingEntity target = (LivingEntity) (Object) this;

        // クライアント除外
        if (target.level().isClientSide) return;

        // リベンジダメージ以外は素通し
        if (!source.is(RevengeDamageSource.REVENGE)) return;

        // 自傷除外
        if (source.getEntity() == target) {
            cir.setReturnValue(false);
            return;
        }

        // クリエイティブ除外
        if (target instanceof Player p && p.isCreative()) {
            cir.setReturnValue(false);
            return;
        }

        // リベンジダメージ実行
        RevengeTracker.applyRevengeDamage(target, source, amount);

        // バニラ hurt() をスキップ
        cir.setReturnValue(true);
    }

    // =========================================================
    // 回復阻害: heal()
    // =========================================================
    @Inject(method = "heal", at = @At("HEAD"), cancellable = true)
    private void forlaism$blockRevengeHeal(
            float amount,
            CallbackInfo ci
    ) {
        LivingEntity self = (LivingEntity) (Object) this;
        if (self.level().isClientSide) return;

        if (RevengeTracker.isRecoveryBlocked(self.getUUID())) {
            ci.cancel();
        }
    }

    // =========================================================
    // 回復阻害: setHealth()（HP増加を拒否）
    // =========================================================
    @Inject(method = "setHealth", at = @At("HEAD"), cancellable = true)
    private void forlaism$blockRevengeSetHealthIncrease(
            float health,
            CallbackInfo ci
    ) {
        LivingEntity self = (LivingEntity) (Object) this;
        if (self.level().isClientSide) return;

        if (!RevengeTracker.isRecoveryBlocked(self.getUUID())) {
            return;
        }

        // 現在HPより大きい値は拒否
        if (health > self.getHealth()) {
            ci.cancel();
        }
    }
}
