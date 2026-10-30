package com.tyami.forlaism.network;

import com.tyami.forlaism.world.SavedEntityData;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.MinecraftServer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.PacketDistributor;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.Supplier;

/**
 * 保存Entityリストの同期 (Server → Client)。
 */
public class ClockEntityListSyncPacket {

    public static final class Entry {
        public final UUID id;
        public final String displayName;
        public final String entityTypeId;

        public Entry(UUID id, String displayName, String entityTypeId) {
            this.id = id;
            this.displayName = displayName;
            this.entityTypeId = entityTypeId;
        }
    }

    public final List<Entry> entries;

    public ClockEntityListSyncPacket(List<Entry> entries) {
        this.entries = entries;
    }

    public ClockEntityListSyncPacket(FriendlyByteBuf buf) {
        int size = buf.readVarInt();
        this.entries = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            entries.add(new Entry(
                    buf.readUUID(),
                    buf.readUtf(),
                    buf.readUtf()
            ));
        }
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeVarInt(entries.size());
        for (Entry e : entries) {
            buf.writeUUID(e.id);
            buf.writeUtf(e.displayName);
            buf.writeUtf(e.entityTypeId);
        }
    }

    public void handle(Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() ->
                DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () ->
                        com.tyami.forlaism.client.ClockStateCache.updateEntityList(entries)
                )
        );
        ctx.get().setPacketHandled(true);
    }

    public static void broadcast(MinecraftServer server) {
        SavedEntityData data = SavedEntityData.get(server);
        List<Entry> list = new ArrayList<>();
        for (SavedEntityData.Entry e : data.getAll()) {
            list.add(new Entry(e.id, e.displayName, e.entityTypeId));
        }
        FactotumPacketHandler.CHANNEL.send(
                PacketDistributor.ALL.noArg(),
                new ClockEntityListSyncPacket(list)
        );
    }
}