package com.tyami.forlaism.network;

import com.tyami.forlaism.item.PrismLightItem;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * クライアント → サーバー: 視線の対象をロック/解除。
 */
public class PrismLockPacket {

    public PrismLockPacket() {
    }

    public PrismLockPacket(FriendlyByteBuf buf) {
    }

    public void encode(FriendlyByteBuf buf) {
    }

    public void handle(Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
            if (player == null) return;

            // 視線先のentityを取得
            Vec3 eye = player.getEyePosition();
            Vec3 look = player.getLookAngle();
            Vec3 end = eye.add(look.scale(64.0));

            AABB box = player.getBoundingBox().expandTowards(look.scale(64.0)).inflate(2.0);

            EntityHitResult hit = ProjectileUtil.getEntityHitResult(
                    player.level(), player, eye, end, box,
                    e -> e instanceof LivingEntity && e.isAlive() && e != player
            );

            if (hit != null && hit.getEntity() instanceof LivingEntity living) {
                PrismLightItem.toggleLock(player, living);
            } else {
                player.displayClientMessage(
                        net.minecraft.network.chat.Component.literal("§7対象が見つかりません"), true);
            }
        });
        ctx.get().setPacketHandled(true);
    }
}