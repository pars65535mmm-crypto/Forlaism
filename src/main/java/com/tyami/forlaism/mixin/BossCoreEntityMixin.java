package com.tyami.forlaism.mixin;

import com.tyami.forlaism.entity.BossCoreEntity;
import com.tyami.forlaism.world.BossDeathDetector;
import com.tyami.forlaism.world.BossDeathRecord;

import net.minecraft.world.entity.Entity;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.UUID;

/**
 * BossCoreEntity の setRemoved / discard を検出する Mixin。
 *
 * Entity#setRemoved / Entity#discard は final なので、
 * BossCoreEntity 側で override できない。
 * また BossCoreEntity には定義がないので @Mixin(BossCoreEntity.class) では注入できない。
 *
 * そこで Entity 全体に注入し、中で instanceof BossCoreEntity チェックする。
 */
@Mixin(value = Entity.class, priority = 100)
public abstract class BossCoreEntityMixin {

    /**
     * setRemoved(RemovalReason)V を検出。
     */
    @Inject(
            method = "setRemoved",
            at = @At("HEAD"),
            cancellable = true
    )
    private void forlaism$detectSetRemoved(
            Entity.RemovalReason reason,
            CallbackInfo ci
    ) {
        Entity self = (Entity) (Object) this;

        if (!(self instanceof BossCoreEntity bossCore)) return;

        UUID bossId = bossCore.getBossId();
        if (bossId != null) {
            BossDeathDetector.detect(
                    bossId,
                    BossDeathRecord.Type.SET_REMOVED_CALL,
                    "reason=" + reason.name()
            );
        }

        if (reason == Entity.RemovalReason.KILLED
                || reason == Entity.RemovalReason.DISCARDED) {
            ci.cancel();
        }
    }

    /**
     * discard()V を検出。
     */
    @Inject(
            method = "discard",
            at = @At("HEAD"),
            cancellable = true
    )
    private void forlaism$detectDiscard(
            CallbackInfo ci
    ) {
        Entity self = (Entity) (Object) this;

        if (!(self instanceof BossCoreEntity bossCore)) return;

        UUID bossId = bossCore.getBossId();
        if (bossId != null) {
            BossDeathDetector.detect(
                    bossId,
                    BossDeathRecord.Type.DISCARD_CALL,
                    ""
            );
        }

        // 消さない
        ci.cancel();
    }
}