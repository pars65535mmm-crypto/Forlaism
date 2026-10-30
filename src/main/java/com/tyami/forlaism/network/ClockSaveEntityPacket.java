package com.tyami.forlaism.network;

import com.tyami.forlaism.item.SavedEntityRestorer;
import com.tyami.forlaism.world.SavedEntityData;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.UUID;
import java.util.function.Supplier;

/**
 * Entity保存・復活・削除の要求 (Client → Server)。
 */
public class ClockSaveEntityPacket {

    public enum Type {
        SAVE_LOOKED_AT,
        RESTORE,
        DELETE,
        CLEAR_ALL,
        REQUEST_LIST
    }

    private final Type type;
    private final UUID entryId;

    public ClockSaveEntityPacket(Type type, UUID entryId) {
        this.type = type;
        this.entryId = entryId;
    }

    public ClockSaveEntityPacket(FriendlyByteBuf buf) {
        this.type = Type.values()[buf.readVarInt()];
        this.entryId = buf.readBoolean() ? buf.readUUID() : null;
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeVarInt(type.ordinal());
        buf.writeBoolean(entryId != null);
        if (entryId != null) buf.writeUUID(entryId);
    }

    public void handle(Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
            if (player == null) return;

            // クロック装備チェック
            boolean hasClock = top.theillusivec4.curios.api.CuriosApi
                    .getCuriosInventory(player)
                    .map(h -> h.findFirstCurio(
                            s -> s.getItem() instanceof com.tyami.forlaism.item.MasterpieceClockItem
                    ).isPresent())
                    .orElse(false);

            if (!hasClock) return;

            switch (type) {
                case SAVE_LOOKED_AT -> {
                    boolean ok = SavedEntityRestorer.saveLookedAtEntity(player);
                    if (ok) {
                        player.displayClientMessage(
                                Component.literal("§d§lEntityを保存した…"), true);
                    } else {
                        player.displayClientMessage(
                                Component.literal("§c対象が見つからない"), true);
                    }
                }
                case RESTORE -> {
                    if (entryId == null) return;
                    boolean ok = SavedEntityRestorer.restore(player, entryId);
                    if (ok) {
                        player.displayClientMessage(
                                Component.literal("§d§lEntityが顕現した…"), true);
                    } else {
                        player.displayClientMessage(
                                Component.literal("§c復活に失敗"), true);
                    }
                }
                case DELETE -> {
                    if (entryId == null) return;
                    SavedEntityData.get(player.server).remove(entryId);
                    player.displayClientMessage(
                            Component.literal("§7削除した"), true);
                }
                case CLEAR_ALL -> {
                    SavedEntityData.get(player.server).clear();
                    player.displayClientMessage(
                            Component.literal("§7全削除した"), true);
                }
                case REQUEST_LIST -> {
                    // 何もしない（リストは画面側で取得済み）
                }
            }

            // リスト同期
            ClockEntityListSyncPacket.broadcast(player.server);
        });
        ctx.get().setPacketHandled(true);
    }
}