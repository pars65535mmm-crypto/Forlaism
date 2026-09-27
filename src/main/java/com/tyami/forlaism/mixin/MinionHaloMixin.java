package com.tyami.forlaism.mixin;

import com.tyami.forlaism.registry.Items;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 眷属の光輪を装備したMobの即死耐性。
 *
 * persistentData の "ForlaismImmortal" フラグで管理。
 *   true  → 不死（die キャンセル）
 *   false → 普通に死ぬ
 *
 * デフォルトは true。
 */
@Mixin(value = LivingEntity.class, priority = 1500)
public abstract class MinionHaloMixin {

    /** 不死フラグのキー。 */
    private static final String IMMORTAL_TAG = "ForlaismImmortal";

    private static boolean forlaism$hasMinionHalo(LivingEntity entity) {

        if (entity instanceof Player) return false;
        if (!(entity instanceof Mob)) return false;
        if (entity.level() == null || entity.level().isClientSide()) return false;

        try {
            ItemStack helmet = entity.getItemBySlot(EquipmentSlot.HEAD);
            return !helmet.isEmpty() && helmet.is(Items.MINION_HALO.get());
        } catch (Throwable t) {
            return false;
        }
    }

    /**
     * 不死フラグが ON か判定。
     *
     * フラグが未設定（初回）の場合はデフォルト true を返す。
     */
    private static boolean forlaism$isImmortal(LivingEntity entity) {
        CompoundTag data = entity.getPersistentData();
        if (!data.contains(IMMORTAL_TAG)) {
            // 初回は自動で true をセット
            data.putBoolean(IMMORTAL_TAG, true);
            return true;
        }
        return data.getBoolean(IMMORTAL_TAG);
    }

    /**
     * die() を無効化（不死フラグが ON の時のみ）。
     */
    @Inject(
            method = "die",
            at = @At("HEAD"),
            cancellable = true
    )
    private void forlaism$blockMinionDie(
            DamageSource source,
            CallbackInfo ci
    ) {
        LivingEntity self = (LivingEntity) (Object) this;

        if (!forlaism$hasMinionHalo(self)) return;

        // 不死フラグが OFF なら普通に死ぬ
        if (!forlaism$isImmortal(self)) return;

        // HPを1に保って死なないように
        if (self.getHealth() <= 0.0F) {
            self.setHealth(1.0F);
        }
        ci.cancel();
    }
}