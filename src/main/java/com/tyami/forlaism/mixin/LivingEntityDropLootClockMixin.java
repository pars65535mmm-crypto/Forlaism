package com.tyami.forlaism.mixin;

import com.tyami.forlaism.item.MasterpieceClockItem;

import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 死亡時に MasterpieceClockItem をドロップしないようにする。
 *
 * Curios スロットは LivingEntity#dropAllDeathLoot の対象外だが、
 * 念のため通常インベントリ側も保護する。
 */
@Mixin(LivingEntity.class)
public abstract class LivingEntityDropLootClockMixin {

    @Inject(method = "dropAllDeathLoot", at = @At("HEAD"))
    private void forlaism$protectClockOnDeath(DamageSource source, CallbackInfo ci) {
        LivingEntity self = (LivingEntity) (Object) this;

        if (!(self instanceof Player player)) return;

        // インベントリを走査してクロックを退避
        var inv = player.getInventory();
        for (int i = 0; i < inv.getContainerSize(); i++) {
            ItemStack stack = inv.getItem(i);
            if (stack.getItem() instanceof MasterpieceClockItem) {
                // 一度退避して、後で復元
                // → シンプルに「死亡時のドロップ対象から除外」するため、
                //   ここでいったんスロットを空にして退避リストへ
                MasterpieceClockDeathCache.store(player.getUUID(), stack.copy());
                inv.setItem(i, ItemStack.EMPTY);
            }
        }
    }

    @Inject(method = "dropAllDeathLoot", at = @At("RETURN"))
    private void forlaism$restoreClockAfterDrop(DamageSource source, CallbackInfo ci) {
        LivingEntity self = (LivingEntity) (Object) this;

        if (!(self instanceof Player player)) return;

        ItemStack cached = MasterpieceClockDeathCache.consume(player.getUUID());
        if (cached == null || cached.isEmpty()) return;

        // インベントリに戻す（空きスロットへ）
        if (!player.getInventory().add(cached)) {
            // 入らなければCuriosのclockスロットへ
            top.theillusivec4.curios.api.CuriosApi
                    .getCuriosInventory(player)
                    .ifPresent(handler -> handler.setEquippedCurio("clock", 0, cached));
        }
    }
}