package com.tyami.forlaism.mixin;

import com.tyami.forlaism.item.SovereignScepterSwordItem;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LivingEntity.class)
public abstract class LivingEntitySovereignMixin {

    /**
     * 君主乃笏剣による攻撃だけ、
     * 通常の無敵時間を無視する。
     */
    @Inject(
            method = "hurt",
            at = @At("HEAD"),
            cancellable = true
    )
    private void forlaism$sovereignHurt(
            DamageSource source,
            float amount,
            CallbackInfoReturnable<Boolean> cir
    ) {

        LivingEntity target = (LivingEntity) (Object) this;

        /*
         * ダメージを与えたEntityを取得
         */
        if (!(source.getEntity() instanceof Player player)) {
            return;
        }

        /*
         * メインハンドのアイテムを確認
         */
        ItemStack stack = player.getMainHandItem();

        /*
         * 君主乃笏剣以外は通常処理
         */
        if (!(stack.getItem() instanceof SovereignScepterSwordItem)) {
            return;
        }

        /*
         * クライアントでは処理しない
         */
        if (target.level().isClientSide) {
            return;
        }

        /*
         * 攻撃されたことを記録
         */
        target.setLastHurtByPlayer(player);

        /*
         * 無敵時間を強制解除
         */
        target.invulnerableTime = 0;
        target.hurtTime = 0;

        /*
         * =====================================================
         * 君主乃笏剣専用ダメージ処理（兼・体力増強）
         * =====================================================
         */
        float currentHealth = target.getHealth();
        
        // 元の計算式： target.getHealth() + sovereignDamage * target.getHealth()
        // ※ sovereignDamage が正の数の場合、実質「大回復＋最大HP上昇」の処理になります
        float nextHealth = Math.max(0.0F, currentHealth + amount * currentHealth);

        // 1. 最大HPを管理する属性インスタンスを取得
        net.minecraft.world.entity.ai.attributes.AttributeInstance maxHealthAttr = 
                target.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.MAX_HEALTH);

        if (maxHealthAttr != null) {
            // 2. バニラの1024上限をリフレクションで強制破壊（初回のみでも可ですが、ここで安全に実行）
            try {
                // net.minecraft.world.entity.ai.attributes.RangedAttribute の maxValue フィールド
                // 1.20.1 ForgeのMojangマッピングでは "maxValue" または "f_22240_"
                java.lang.reflect.Field maxValueField = net.minecraft.world.entity.ai.attributes.RangedAttribute.class.getDeclaredField("f_22240_");
                maxValueField.setAccessible(true);
                maxValueField.set(net.minecraft.world.entity.ai.attributes.Attributes.MAX_HEALTH, 100000000.0D); // 1億など十分な数を設定
            } catch (Exception e) {
                // 環境によってはマッピング名が異なる場合のフォールバック
                try {
                    java.lang.reflect.Field maxValueField = net.minecraft.world.entity.ai.attributes.RangedAttribute.class.getDeclaredField("maxValue");
                    maxValueField.setAccessible(true);
                    maxValueField.set(net.minecraft.world.entity.ai.attributes.Attributes.MAX_HEALTH, 100000000.0D);
                } catch (Exception ignored) {}
            }

            // 3. 先に「最大体力」を設定する（1024の壁を超えた値がセット可能になる）
            float nextMaxHealth = nextHealth * nextHealth;
            maxHealthAttr.setBaseValue(nextMaxHealth);
        }

        /*
         * 4. 実際のHPを減少・増加させる
         * 先に最大体力を増やしてあるので、制限（クランプ）されずに代入できます
         */
        target.setHealth(nextHealth);

        /*
         * 被弾状態を更新
         */
        target.hurtTime = 10;
        target.hurtDuration = 10;

        /*
         * 死亡判定
         */
        if (target.getHealth() <= 0.0F) {
            target.die(source);
        }

        /*
         * 通常のhurt()を実行させない
         */
        cir.setReturnValue(true);
    }
}
