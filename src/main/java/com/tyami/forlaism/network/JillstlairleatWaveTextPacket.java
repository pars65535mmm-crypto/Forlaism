package com.tyami.forlaism.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * サーバーからクライアントへ wavetextlib テキスト表示を要求するパケット。
 */
public class JillstlairleatWaveTextPacket {

    private final String text;
    private final int durationTicks;

    public JillstlairleatWaveTextPacket(String text, int durationTicks) {
        this.text = text;
        this.durationTicks = durationTicks;
    }

    public JillstlairleatWaveTextPacket(FriendlyByteBuf buf) {
        this.text = buf.readUtf();
        this.durationTicks = buf.readInt();
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeUtf(this.text);
        buf.writeInt(this.durationTicks);
    }

    public void handle(Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() ->
                DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () ->
                        com.tyami.forlaism.client.JillstlairleatWaveTextRenderer.showText(text, durationTicks)
                )
        );
        ctx.get().setPacketHandled(true);
    }
}
