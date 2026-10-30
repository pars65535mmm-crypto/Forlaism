package com.tyami.forlaism.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * プレイヤー個別のクロック状態を同期。
 */
public class ClockPlayerStateSyncPacket {

    public final boolean autoTotem;
    public final boolean autoAim;

    public ClockPlayerStateSyncPacket(boolean autoTotem, boolean autoAim) {
        this.autoTotem = autoTotem;
        this.autoAim = autoAim;
    }

    public ClockPlayerStateSyncPacket(FriendlyByteBuf buf) {
        this.autoTotem = buf.readBoolean();
        this.autoAim = buf.readBoolean();
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeBoolean(autoTotem);
        buf.writeBoolean(autoAim);
    }

    public void handle(Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() ->
                DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () ->
                        com.tyami.forlaism.client.ClockStateCache.updatePlayerState(this)
                )
        );
        ctx.get().setPacketHandled(true);
    }
}