package com.tyami.forlaism.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * ジルスレイト所持時の死亡暗転シーケンス開始パケット。
 */
public class JillstlairleatDeathPacket {

    public JillstlairleatDeathPacket() {
    }

    public JillstlairleatDeathPacket(FriendlyByteBuf buf) {
    }

    public void encode(FriendlyByteBuf buf) {
    }

    public void handle(Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() ->
                DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () ->
                        com.tyami.forlaism.client.JillstlairleatCinematicHandler.startDeathSequence()
                )
        );
        ctx.get().setPacketHandled(true);
    }
}
