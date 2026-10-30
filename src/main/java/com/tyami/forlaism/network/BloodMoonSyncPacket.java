package com.tyami.forlaism.network;

import com.tyami.forlaism.world.BloodMoonManager;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * サーバー → クライアントへのブラッドムーン状態同期パケット。
 */
public record BloodMoonSyncPacket(boolean active) {

    public static void encode(BloodMoonSyncPacket msg, FriendlyByteBuf buf) {
        buf.writeBoolean(msg.active);
    }

    public static BloodMoonSyncPacket decode(FriendlyByteBuf buf) {
        return new BloodMoonSyncPacket(buf.readBoolean());
    }

    public static void handle(BloodMoonSyncPacket msg, Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context ctx = supplier.get();
        ctx.enqueueWork(() ->
                DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () ->
                        BloodMoonManager.setClientActive(msg.active)
                )
        );
        ctx.setPacketHandled(true);
    }
}