package com.tyami.forlaism.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * サーバー → クライアント: バインド画面を開く。
 */
public class OpenClockBindClientPacket {

    public OpenClockBindClientPacket() {
    }

    public OpenClockBindClientPacket(FriendlyByteBuf buf) {
    }

    public void encode(FriendlyByteBuf buf) {
    }

    public void handle(Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() ->
                DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> {
                    // 状態を最新にしておく
                    if (net.minecraft.client.Minecraft.getInstance().player != null) {
                        // ClockStateCache はサーバーから同期済みなのでそのまま
                    }

                    net.minecraft.client.Minecraft.getInstance().setScreen(
                            new com.tyami.forlaism.client.ClockBindScreen()
                    );
                })
        );
        ctx.get().setPacketHandled(true);
    }
}