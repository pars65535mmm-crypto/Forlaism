package com.tyami.forlaism.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * クライアントの視界を暗転させるパケット。
 */
public class DarkenScreenPacket {

    /** 暗転の持続tick。 */
    private final int durationTicks;

    public DarkenScreenPacket(int durationTicks) {
        this.durationTicks = durationTicks;
    }

    public DarkenScreenPacket(FriendlyByteBuf buf) {
        this.durationTicks = buf.readInt();
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeInt(durationTicks);
    }

    public void handle(Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() ->
                DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () ->
                        com.tyami.forlaism.client.ClientDarkenHandler.start(durationTicks)
                )
        );
        ctx.get().setPacketHandled(true);
    }
}