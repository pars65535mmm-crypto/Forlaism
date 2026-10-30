package com.tyami.forlaism.network;

import com.tyami.forlaism.world.WorldTimeFreezeManager;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * サーバーからクライアントへ時間停止フラグを同期するパケット。
 */
public class TimeFreezeSyncPacket {

    private final boolean frozen;

    public TimeFreezeSyncPacket(boolean frozen) {
        this.frozen = frozen;
    }

    public TimeFreezeSyncPacket(FriendlyByteBuf buf) {
        this.frozen = buf.readBoolean();
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeBoolean(this.frozen);
    }

    public void handle(Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context ctx = contextSupplier.get();
        ctx.enqueueWork(() -> {
            WorldTimeFreezeManager.setClientFrozen(this.frozen);
        });
        ctx.setPacketHandled(true);
    }
}
