package com.tyami.forlaism.network;

import com.tyami.forlaism.item.MuratsukumoDashTracker;
import com.tyami.forlaism.item.MuratsukumoItem;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * ムラツクモの突進をクライアント→サーバーへ伝える。
 *
 * すでに use() の中でサーバー側でも処理しているので、
 * これは「二重発動防止」と「確実な同期」のための保険。
 * 実は use() だけで完結するなら不要。
 *
 * 今回は使わない（use() が両側で走るので）。
 * ファイルだけ用意しておくと将来拡張できる。
 */
public class MuratsukumoDashPacket {

    private final Vec3 direction;

    public MuratsukumoDashPacket(Vec3 direction) {
        this.direction = direction;
    }

    public MuratsukumoDashPacket(FriendlyByteBuf buf) {
        this.direction = new Vec3(buf.readDouble(), buf.readDouble(), buf.readDouble());
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeDouble(direction.x);
        buf.writeDouble(direction.y);
        buf.writeDouble(direction.z);
    }

    public void handle(Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
            if (player == null) return;

            // 既に use() で突進済みなので、ここでは何もしない
            // 必要なら強制突進を書く
        });
        ctx.get().setPacketHandled(true);
    }
}