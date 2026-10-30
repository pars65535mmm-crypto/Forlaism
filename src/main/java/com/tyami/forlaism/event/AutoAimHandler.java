package com.tyami.forlaism.event;

import com.tyami.forlaism.world.PlayerClockData;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.CrossbowItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * オートエイム。
 *
 * 変更点:
 *   - ロック中でもプレイヤーの手動視点操作を奪わない
 *   - 「射撃アイテムを使っている瞬間だけ」視線を補正する
 *   - ロック対象はShiftを1秒押してロック/解除
 */
@Mod.EventBusSubscriber(modid = "forlaism")
public final class AutoAimHandler {

    private static final int HOLD_TICKS = 20;
    private static final double LOCK_RANGE = 64.0D;
    private static final double AUTO_AIM_RANGE = 32.0D;

    /** 補正する最大角度（度）。これ以上ズレていたら無理に曲げない。 */
    private static final double MAX_AIM_ANGLE = 25.0D;

    private AutoAimHandler() {
    }

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        if (!(event.player instanceof ServerPlayer player)) return;
        if (player.level().isClientSide) return;

        boolean hasClock = top.theillusivec4.curios.api.CuriosApi
                .getCuriosInventory(player)
                .map(h -> h.findFirstCurio(
                        s -> s.getItem() instanceof com.tyami.forlaism.item.MasterpieceClockItem
                ).isPresent())
                .orElse(false);

        if (!hasClock) return;

        if (!PlayerClockData.isAutoAim(player)) return;

        // ---------------------------------------------------------
        // Shift 長押しでロック/解除
        // ---------------------------------------------------------
        if (player.isShiftKeyDown()) {
            int hold = PlayerClockData.getAimHold(player) + 1;
            PlayerClockData.setAimHold(player, hold);

            if (hold == HOLD_TICKS) {
                if (PlayerClockData.hasAimTarget(player)) {
                    PlayerClockData.setAimTarget(player, null);
                    player.displayClientMessage(
                            net.minecraft.network.chat.Component.literal("§cエイムロック解除"), true);
                } else {
                    LivingEntity target = findLookedAtEntity(player, LOCK_RANGE);
                    if (target != null) {
                        PlayerClockData.setAimTarget(player, target.getUUID());
                        player.displayClientMessage(
                                net.minecraft.network.chat.Component.literal(
                                        "§aエイムロック: §f" + target.getName().getString()), true);
                    }
                }
            }
        } else {
            PlayerClockData.resetAimHold(player);
        }

        // ---------------------------------------------------------
        // ★修正: 射撃アイテムを使っている瞬間だけ視線補正
        // ---------------------------------------------------------
        if (!isUsingRangedWeapon(player)) {
            return;
        }

        if (!PlayerClockData.hasAimTarget(player)) {
            return;
        }

        var targetUuid = PlayerClockData.getAimTarget(player);
        var target = player.serverLevel().getEntity(targetUuid);

        if (!(target instanceof LivingEntity living) || !living.isAlive()
                || living.distanceToSqr(player) > AUTO_AIM_RANGE * AUTO_AIM_RANGE) {
            PlayerClockData.setAimTarget(player, null);
            return;
        }

        // 既存の視線との角度差をチェック
        Vec3 currentLook = player.getLookAngle().normalize();
        Vec3 toTarget = living.getEyePosition().subtract(player.getEyePosition()).normalize();

        double dot = Math.max(-1.0, Math.min(1.0, currentLook.dot(toTarget)));
        double angleDeg = Math.toDegrees(Math.acos(dot));

        if (angleDeg > MAX_AIM_ANGLE) {
            // 大きくズレているときは補正しない（プレイヤーの操作を尊重）
            return;
        }

        // 視線をターゲットへ向ける
        double dx = living.getX() - player.getX();
        double dy = living.getEyeY() - player.getEyeY();
        double dz = living.getZ() - player.getZ();
        double horiz = Math.sqrt(dx * dx + dz * dz);
        float yaw = (float) (Math.atan2(dz, dx) * (180.0 / Math.PI)) - 90.0F;
        float pitch = (float) -(Math.atan2(dy, horiz) * (180.0 / Math.PI));

        player.setYRot(yaw);
        player.setYHeadRot(yaw);
        player.setXRot(pitch);
        player.connection.teleport(
                player.getX(), player.getY(), player.getZ(),
                yaw, pitch
        );
    }

    /**
     * 弓・クロスボウ・投擲物系を構えているか。
     */
    private static boolean isUsingRangedWeapon(ServerPlayer player) {
        if (!player.isUsingItem()) return false;
        ItemStack use = player.getUseItem();
        return use.getItem() instanceof BowItem
                || use.getItem() instanceof CrossbowItem
                || use.getItem() instanceof com.tyami.forlaism.item.PrismLightItem
                || use.getItem() instanceof com.tyami.forlaism.item.EnergyDaggerItem
                || use.getItem() instanceof com.tyami.forlaism.item.HaniwaNoYariItem
                || use.getItem() instanceof com.tyami.forlaism.item.CherenkovMetaAdamanediumRailgunBladeItem;
    }

    private static LivingEntity findLookedAtEntity(ServerPlayer player, double range) {
        Vec3 eye = player.getEyePosition();
        Vec3 look = player.getLookAngle().normalize();
        Vec3 end = eye.add(look.scale(range));

        AABB box = player.getBoundingBox().expandTowards(look.scale(range)).inflate(2.0);
        EntityHitResult hit = ProjectileUtil.getEntityHitResult(
                player.level(), player, eye, end, box,
                e -> e instanceof LivingEntity && e.isAlive() && e != player
        );
        return hit != null && hit.getEntity() instanceof LivingEntity l ? l : null;
    }
}