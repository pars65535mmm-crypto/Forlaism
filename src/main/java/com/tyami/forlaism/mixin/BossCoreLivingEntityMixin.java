package com.tyami.forlaism.mixin;

import com.tyami.forlaism.entity.BossCoreEntity;
import com.tyami.forlaism.world.BossDeathDetector;
import com.tyami.forlaism.world.BossDeathRecord;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.UUID;

/**
 * BossCoreEntity の setHealth を検出する Mixin。
 *
 * LivingEntity#setHealth は final なので override できない。
 * また BossCoreEntity には定義がないので @Mixin(BossCoreEntity.class) では注入できない。
 *
 * そこで LivingEntity 全体に注入し、中で instanceof BossCoreEntity チェックする。
 */
@Mixin(value = LivingEntity.class, priority = 100)
public abstract class BossCoreLivingEntityMixin {

    /** setHealth 再入防止フラグ。 */
    @Unique
    private boolean forlaism$settingHealth = false;

    /**
     * setHealth(F)V を検出。
     */
    @Inject(
            method = "setHealth",
            at = @At("HEAD"),
            cancellable = true
    )
    private void forlaism$detectSetHealthZero(
            float health,
            CallbackInfo ci
    ) {
        LivingEntity self = (LivingEntity) (Object) this;

        if (!(self instanceof BossCoreEntity bossCore)) return;

        // 再入防止
        if (forlaism$settingHealth) return;

        UUID bossId = bossCore.getBossId();

        if (bossId != null && health <= 0.0F) {
            BossDeathDetector.detect(
                    bossId,
                    BossDeathRecord.Type.SET_HEALTH_ZERO,
                    "health=" + health
            );
        }

        // 自分のHPは1で固定
        if (health < 1.0F) {
            ci.cancel();

            forlaism$settingHealth = true;
            try {
                // super.setHealth(1.0F) を呼ぶ代わりに直接セット
                bossCore.setHealth(1.0F);
            } finally {
                forlaism$settingHealth = false;
            }
        }
    }
}