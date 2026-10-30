package com.tyami.forlaism.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * ジルスレイト自壊時のカメラ離脱シーケンス開始パケット。
 */
public class JillstlairleatSelfDestructPacket {

    public JillstlairleatSelfDestructPacket() {
    }

    public JillstlairleatSelfDestructPacket(FriendlyByteBuf buf) {
    }

    public void encode(FriendlyByteBuf buf) {
    }

    public void handle(Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() ->
                DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () ->
                        com.tyami.forlaism.client.JillstlairleatCinematicHandler.startSelfDestructSequence()
                )
        );
        ctx.get().setPacketHandled(true);
    }
}
