package com.tyami.forlaism.mixin;

import com.tyami.forlaism.damage.MadoromiDamageSource;
import com.tyami.forlaism.damage.MadoromiExecution;

import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 微睡ダメージの実処理。
 *
 * 光輪（Halo of the Advent / Firmament）の防御を
 * 「正面から」突破する。光輪のクラスは一切参照しない。
 *
 * priority = 5000:
 *   光輪の Mixin (デフォルト priority) より後に走らせる。
 *
 *   これにより:
 *     - hurt():     光輪の setReturnValue(false) を setReturnValue(true) で上書き
 *     - setHealth(): 光輪の preventHealthReduction (ci.cancel()) を
 *                    微睡実行中フラグで無効化
 *     - die():      光輪の reverseDeath (ci.cancel()) は priority で消せないため、
 *                    そもそも die を呼ばせず MadoromiExecution で直接死亡させる
 */
@Mixin(value = LivingEntity.class, priority = 5000)
public abstract class LivingEntityMadoromiMixin {

    // =========================================================
    // hurt: 微睡ダメージなら強制成功
    // =========================================================

    @Inject(
            method = "hurt",
            at = @At("HEAD"),
            cancellable = true
    )
    private void forlaism$madoromiHurt(
            DamageSource source,
            float amount,
            CallbackInfoReturnable<Boolean> cir
    ) {
        LivingEntity self = (LivingEntity) (Object) this;
        

        // クライアント除外
        if (self.level().isClientSide) return;

        // 微睡ダメージ以外は素通し
        if (!source.is(MadoromiDamageSource.MADOROMI)) return;

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
        // 微睡ダメージは「純粋にダメージを当てるだけ」で殺す
        // =========================================================
        // ここで MadoromiExecution に直接飛ばす。
        // バニラ hurt() のロジックも、光輪の防御も、全部迂回。
        MadoromiExecution.execute(self, source);

        // バニラ hurt() を完全にスキップ
        cir.setReturnValue(true);
    }

    // =========================================================
    // setHealth: 微睡実行中は減少を許可
    // =========================================================

    @Inject(
            method = "setHealth",
            at = @At("HEAD"),
            cancellable = true
    )
    private void forlaism$madoromiSetHealth(
            float health,
            CallbackInfo ci
    ) {
        LivingEntity self = (LivingEntity) (Object) this;

        if (self.level().isClientSide) return;

        // 微睡実行中は何もしない（バニラ setHealth を通す）
        if (MadoromiDamageSource.isExecuting()) return;

        // それ以外は素通し（光輪の Mixin が勝手に処理する）
    }

    // =========================================================
    // die: 微睡実行中は「die を通さない」
    // =========================================================

    /**
     * 微睡実行中に die() が呼ばれた場合、光輪の reverseDeath が
     * ci.cancel() する前に、我々が先に死亡状態を構築してしまう。
     *
     * ただし、MadoromiExecution.execute() は die() を呼ばないので、
     * このインターセプトは「保険」として機能する。
     *
     * 万が一 die() が呼ばれた場合でも、微睡実行中なら
     * 光輪の reverseDeath より先に死亡を確定させる。
     */
    @Inject(
            method = "die",
            at = @At("HEAD"),
            cancellable = true
    )
    private void forlaism$madoromiDie(
            DamageSource source,
            CallbackInfo ci
    ) {
        LivingEntity self = (LivingEntity) (Object) this;

        if (self.level().isClientSide) return;

        // 微睡実行中なら、死亡処理は既に MadoromiExecution 側で完了している
        // → die() は呼ばせない（光輪の reverseDeath も走らせない）
        if (MadoromiDamageSource.isExecuting()) {
            ci.cancel();
        }
    }
}