package com.tyami.forlaism.mixin;

import com.tyami.forlaism.item.MasterpieceClockItem;
import com.tyami.forlaism.registry.Items;

import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * /clear などで MasterpieceClockItem が消されるのを防ぐ。
 *
 * Curios スロットは別管理なので、ここでは通常インベントリのみ保護。
 */
@Mixin(Inventory.class)
public abstract class PlayerInventoryClockProtectionMixin {

    @Inject(method = "clearContent", at = @At("HEAD"), cancellable = true)
    private void forlaism$protectClockFromClear(CallbackInfo ci) {
        Inventory self = (Inventory) (Object) this;

        // プレイヤーインベントリにクロックがあるかチェック
        boolean hasClock = false;
        for (int i = 0; i < self.getContainerSize(); i++) {
            if (self.getItem(i).getItem() instanceof MasterpieceClockItem) {
                hasClock = true;
                break;
            }
        }

        if (hasClock) {
            // クロック以外を消す
            for (int i = 0; i < self.getContainerSize(); i++) {
                ItemStack stack = self.getItem(i);
                if (!(stack.getItem() instanceof MasterpieceClockItem)) {
                    self.setItem(i, ItemStack.EMPTY);
                }
            }
            ci.cancel();
        }
    }
}