package com.tyami.forlaism.mixin;

import com.tyami.forlaism.item.HaloOfTheAdventAbility;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Entity.class)
public abstract class EntityMixin {

    /**
     * Prevent Entity.kill().
     *
     * This covers the normal /kill path before it can kill the player.
     */
    @Inject(
            method = "kill",
            at = @At("HEAD"),
            cancellable = true
    )
    private void forlaism$preventKill(
            CallbackInfo ci
    ) {

        Entity self =
                (Entity) (Object) this;

        if (!(self instanceof Player player)) {
            return;
        }

        if (!HaloOfTheAdventAbility.isProtected(player)) {
            return;
        }

        ci.cancel();
    }


    /**
     * Prevent discard().
     */
    @Inject(
            method = "discard",
            at = @At("HEAD"),
            cancellable = true
    )
    private void forlaism$preventDiscard(
            CallbackInfo ci
    ) {

        Entity self =
                (Entity) (Object) this;

        if (!(self instanceof Player player)) {
            return;
        }

        if (!HaloOfTheAdventAbility.isProtected(player)) {
            return;
        }

        ci.cancel();
    }


    /**
     * Prevent remove(RemovalReason).
     */
    @Inject(
            method = "remove",
            at = @At("HEAD"),
            cancellable = true
    )
    private void forlaism$preventRemove(
            Entity.RemovalReason reason,
            CallbackInfo ci
    ) {

        Entity self =
                (Entity) (Object) this;

        if (!(self instanceof Player player)) {
            return;
        }

        if (!HaloOfTheAdventAbility.isProtected(player)) {
            return;
        }

        ci.cancel();
    }


    /*
     * =========================================================
     * 温泉拘束UOM
     * =========================================================
     */

    /**
     * 温泉拘束されたEntityのteleportToを禁止する。
     *
     * teleportToはEntity側に存在するため、
     * LivingEntityMixinではなくここで処理する。
     */
    @Inject(
            method = "teleportTo(DDD)V",
            at = @At("HEAD"),
            cancellable = true
    )
    private void forlaism$blockOnsenTeleport(
            double x,
            double y,
            double z,
            CallbackInfo ci
    ) {

        Entity self =
                (Entity) (Object) this;

        if (!self.getTags().contains("forlaism:onsen_bound")) {
            return;
        }

        ci.cancel();
    }


    /**
     * 相対テレポートも禁止する。
     */
    @Inject(
            method = "teleportRelative(DDD)V",
            at = @At("HEAD"),
            cancellable = true
    )
    private void forlaism$blockOnsenRelativeTeleport(
            double x,
            double y,
            double z,
            CallbackInfo ci
    ) {

        Entity self =
                (Entity) (Object) this;

        if (!self.getTags().contains("forlaism:onsen_bound")) {
            return;
        }

        ci.cancel();
    }

        /*
     * =========================================================
     * 眷属の光輪耐性（discard / remove 無効化）
     * =========================================================
     */

    /**
     * 眷属の光輪を装備したMobの discard() を無効化。
     */
    @Inject(
            method = "discard",
            at = @At("HEAD"),
            cancellable = true
    )
    private void forlaism$blockMinionDiscard(
            CallbackInfo ci
    ) {
        Entity self = (Entity) (Object) this;

        // Player除外
        if (self instanceof Player) return;

        // Mobのみ
        if (!(self instanceof net.minecraft.world.entity.Mob)) return;

        // Level未ロード時は無視
        if (self.level() == null) return;

        if (!(self instanceof net.minecraft.world.entity.LivingEntity living)) return;

        net.minecraft.world.item.ItemStack helmet =
                living.getItemBySlot(net.minecraft.world.entity.EquipmentSlot.HEAD);

        if (helmet.isEmpty()
                || !helmet.is(com.tyami.forlaism.registry.Items.MINION_HALO.get())) {
            return;
        }

        ci.cancel();
    }

    /**
     * 眷属の光輪を装備したMobの remove(RemovalReason) を無効化。
     */
    @Inject(
            method = "remove",
            at = @At("HEAD"),
            cancellable = true
    )
    private void forlaism$blockMinionRemove(
            Entity.RemovalReason reason,
            CallbackInfo ci
    ) {
        Entity self = (Entity) (Object) this;

        // Player除外
        if (self instanceof Player) return;

        // Mobのみ
        if (!(self instanceof net.minecraft.world.entity.Mob)) return;

        // Level未ロード時は無視
        if (self.level() == null) return;

        if (!(self instanceof net.minecraft.world.entity.LivingEntity living)) return;

        net.minecraft.world.item.ItemStack helmet =
                living.getItemBySlot(net.minecraft.world.entity.EquipmentSlot.HEAD);

        if (helmet.isEmpty()
                || !helmet.is(com.tyami.forlaism.registry.Items.MINION_HALO.get())) {
            return;
        }

        ci.cancel();
    }
}