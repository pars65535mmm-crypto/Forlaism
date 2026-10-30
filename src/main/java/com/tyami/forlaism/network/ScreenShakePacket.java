package com.tyami.forlaism.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * 画面揺れ演出パケット。
 *
 * durationTicks: 揺れる時間
 * intensity    : 揺れの強さ (0.0 - 1.0)
 *
 * クライアント側は HaniwainiumScreenShakeClient で受け取る。
 */
public class ScreenShakePacket {

    private final int durationTicks;
    private final float intensity;

    public ScreenShakePacket(int durationTicks, float intensity) {
        this.durationTicks = durationTicks;
        this.intensity = intensity;
    }

    public ScreenShakePacket(FriendlyByteBuf buf) {
        this.durationTicks = buf.readInt();
        this.intensity = buf.readFloat();
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeInt(durationTicks);
        buf.writeFloat(intensity);
    }

    public void handle(Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() ->
                DistExecutor.unsafeRunWhenOn(
                        Dist.CLIENT,
                        () -> () -> com.tyami.forlaism.client.HaniwainiumScreenShakeClient
                                .start(durationTicks, intensity)
                )
        );
        ctx.get().setPacketHandled(true);
    }
}