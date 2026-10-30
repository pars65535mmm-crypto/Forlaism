package com.tyami.forlaism.mixin;

import com.tyami.forlaism.world.BloodMoonManager;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.Monster;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * ブラッドムーン中のモブ「集団行動・知性向上」。
 *
 * - ターゲットがいない場合、近くの同種族のターゲットを共有する
 * - プレイヤーを発見したら、仲間に伝播させる
 */
@Mixin(Mob.class)
public abstract class MobBloodMoonAIMixin {

    @Inject(method = "tick", at = @At("TAIL"))
    private void forlaism$bloodMoonPackBehavior(CallbackInfo ci) {
        Mob self = (Mob) (Object) this;
        if (self.level().isClientSide) return;
        if (!(self.level() instanceof net.minecraft.server.level.ServerLevel sl)) return;
        if (!BloodMoonManager.isActive(sl)) return;
        if (!(self instanceof Monster)) return;

        // 5tickごと
        if (self.tickCount % 5 != 0) return;

        // 既にターゲットがいるならスキップ
        LivingEntity current = self.getTarget();
        if (current != null && current.isAlive()) {
            // 仲間に伝播
            if (self.tickCount % 20 == 0) {
                spreadTarget(self, current);
            }
            return;
        }

        // ターゲットがいない → 近くの仲間のターゲットを拾う
        var nearby = sl.getEntitiesOfClass(
                Monster.class,
                self.getBoundingBox().inflate(16.0),
                m -> m != self && m.getTarget() != null && m.getTarget().isAlive()
        );

        if (!nearby.isEmpty()) {
            LivingEntity shared = nearby.get(0).getTarget();
            if (shared != null) {
                self.setTarget(shared);
            }
        }
    }

    /** 自分のターゲットを近くの仲間に伝播。 */
    private static void spreadTarget(Mob self, LivingEntity target) {
        var pack = self.level().getEntitiesOfClass(
                Monster.class,
                self.getBoundingBox().inflate(12.0),
                m -> m != self && m.getTarget() == null
        );
        for (Monster m : pack) {
            m.setTarget(target);
        }
    }
}