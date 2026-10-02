package com.tyami.forlaism.network;

import com.tyami.forlaism.event.GrappleRingHandler;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * クライアント → サーバー: グラップル発射要求。
 */
public class GrappleFirePacket {

    public GrappleFirePacket() {
    }

    public GrappleFirePacket(FriendlyByteBuf buf) {
    }

    public void encode(FriendlyByteBuf buf) {
    }

    public void handle(Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
            if (player == null) return;

            GrappleRingHandler.fire(player);
        });
        ctx.get().setPacketHandled(true);
    }
}