package com.tyami.forlaism.network;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * チェレンコフ光演出の開始をクライアントに通知するパケット。
 */
public class CherenkovEffectPacket {

    private final BlockPos center;
    private final int duration;

    public CherenkovEffectPacket(BlockPos center, int duration) {
        this.center = center;
        this.duration = duration;
    }

    public CherenkovEffectPacket(FriendlyByteBuf buf) {
        this.center = buf.readBlockPos();
        this.duration = buf.readInt();
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeBlockPos(center);
        buf.writeInt(duration);
    }

    public void handle(Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() ->
                DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () ->
                        com.tyami.forlaism.client.renderer.CherenkovState
                                .start(center, duration)
                )
        );
        ctx.get().setPacketHandled(true);
    }
}