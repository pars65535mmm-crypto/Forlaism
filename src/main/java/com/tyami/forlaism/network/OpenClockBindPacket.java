package com.tyami.forlaism.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * クライアント → サーバー: バインド画面を開く要求。
 * サーバー → クライアント: 実際に画面を開く。
 */
public class OpenClockBindPacket {

    public OpenClockBindPacket() {
    }

    public OpenClockBindPacket(FriendlyByteBuf buf) {
    }

    public void encode(FriendlyByteBuf buf) {
    }

    public void handle(Supplier<NetworkEvent.Context> ctx) {
        // クライアント → サーバー
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
            if (player == null) return;

            // Curios の clock スロットにクロックがあるか再確認
            boolean hasClock = top.theillusivec4.curios.api.CuriosApi
                    .getCuriosInventory(player)
                    .map(handler -> handler.findFirstCurio(
                            stack -> stack.getItem() instanceof com.tyami.forlaism.item.MasterpieceClockItem
                    ).isPresent())
                    .orElse(false);

            if (!hasClock) return;

            // クライアントへ「画面を開け」と返す
            FactotumPacketHandler.CHANNEL.sendTo(
                    new OpenClockBindClientPacket(),
                    player.connection.connection,
                    net.minecraftforge.network.NetworkDirection.PLAY_TO_CLIENT
            );
        });
        ctx.get().setPacketHandled(true);
    }
}