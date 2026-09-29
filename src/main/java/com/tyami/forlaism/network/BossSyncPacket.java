package com.tyami.forlaism.network;

import com.tyami.forlaism.world.BossInstance;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.Supplier;

/**
 * サーバー → クライアント：ボスリストの同期。
 */
public class BossSyncPacket {

    public static class Entry {
        public UUID id;
        public String displayName;
        public String skinId;
        public double x, y, z;
        public float yaw;

        public Entry() {
        }

        public Entry(UUID id, String displayName, String skinId, double x, double y, double z, float yaw) {
            this.id = id;
            this.displayName = displayName;
            this.skinId = skinId;
            this.x = x;
            this.y = y;
            this.z = z;
            this.yaw = yaw;
        }
    }

    public final List<Entry> entries;

    public BossSyncPacket(List<Entry> entries) {
        this.entries = entries;
    }

    public BossSyncPacket(FriendlyByteBuf buf) {
        int n = buf.readVarInt();
        this.entries = new ArrayList<>(n);
        for (int i = 0; i < n; i++) {
            Entry e = new Entry();
            e.id = buf.readUUID();
            e.displayName = buf.readUtf();
            e.skinId = buf.readUtf();
            e.x = buf.readDouble();
            e.y = buf.readDouble();
            e.z = buf.readDouble();
            e.yaw = buf.readFloat();
            entries.add(e);
        }
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeVarInt(entries.size());
        for (Entry e : entries) {
            buf.writeUUID(e.id);
            buf.writeUtf(e.displayName);
            buf.writeUtf(e.skinId);
            buf.writeDouble(e.x);
            buf.writeDouble(e.y);
            buf.writeDouble(e.z);
            buf.writeFloat(e.yaw);
        }
    }

    public void handle(Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() ->
                DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () ->
                        com.tyami.forlaism.client.ClientBossCache.update(entries)
                )
        );
        ctx.get().setPacketHandled(true);
    }

    public static BossSyncPacket of(List<BossInstance> bosses) {
        List<Entry> list = new ArrayList<>(bosses.size());
        for (BossInstance b : bosses) {
            list.add(new Entry(b.id, b.displayName, b.skinId, b.x, b.y, b.z, b.yaw));
        }
        return new BossSyncPacket(list);
    }

    public static void sendTo(ServerPlayer player, BossSyncPacket packet) {
        com.tyami.forlaism.network.FactotumPacketHandler.CHANNEL.sendTo(
                packet,
                player.connection.connection,
                net.minecraftforge.network.NetworkDirection.PLAY_TO_CLIENT
        );
    }
}