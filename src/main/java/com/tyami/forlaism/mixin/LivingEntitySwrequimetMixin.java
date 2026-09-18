package com.tyami.forlaism.mixin;

import com.tyami.forlaism.item.SwrequimetBladeItem;

import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Re糖分の剣による攻撃だけ、あらゆる防御を貫通させる。
 *
 * priority = 2000:
 *   LivingEntityMixin (priority = 1000) より後に実行される。
 *
 *   そのため、Halo of the Advent 持ちのように
 *   先に hurt() が false で確定される相手には貫通しない。
 *
 *   それ以外の敵（無敵時間・アーマー・isInvulnerable など）には
 *   強制的にダメージを通す。
 */
@Mixin(value = LivingEntity.class, priority = 2000)
public abstract class LivingEntitySwrequimetMixin {

    @Inject(
            method = "hurt",
            at = @At("HEAD"),
            cancellable = true
    )
    private void forlaism$swrequimetForceHurt(
            DamageSource source,
            float amount,
            CallbackInfoReturnable<Boolean> cir
    ) {

        LivingEntity target = (LivingEntity) (Object) this;

        // クライアント側では処理しない
        if (target.level().isClientSide) {
            return;
        }

        // 攻撃者を取得
        if (!(source.getEntity() instanceof Player player)) {
            return;
        }

        // メインハンドが Re糖分の剣かチェック
        ItemStack mainHand = player.getMainHandItem();
        if (!(mainHand.getItem() instanceof SwrequimetBladeItem)) {
            return;
        }

        // 自分自身への攻撃は除外
        if (target == player) {
            return;
        }

        // =========================================================
        // ここから強制ダメージ処理
        // =========================================================

        // 1. 無敵時間を全解除
        target.invulnerableTime = 0;
        target.hurtTime = 0;

        // 2. クリエイティブの Player は除外
        if (target instanceof Player p && p.isCreative()) {
            cir.setReturnValue(false);
            return;
        }

        // 3. HPを直接減らす（アーマー・エンチャント・耐性すべて無視）
        float currentHealth = target.getHealth();
        float newHealth = Math.max(
                0.0F,
                currentHealth - SwrequimetBladeItem.ATTACK_DAMAGE
        );

        target.setHealth(newHealth);

        // 4. 被弾演出
        target.hurtTime = 10;
        target.hurtDuration = 10;
        target.setLastHurtByPlayer(player);

        // 5. 死亡判定
        if (newHealth <= 0.0F && !target.isDeadOrDying()) {
            target.die(source);
        }

        // 6. バニラの hurt() を完全にスキップ
        cir.setReturnValue(true);
    }
}