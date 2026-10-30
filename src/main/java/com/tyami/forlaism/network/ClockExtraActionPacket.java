package com.tyami.forlaism.network;

import com.tyami.forlaism.event.ClockLocateHandler;
import com.tyami.forlaism.event.ClockTeleportHandler;
import com.tyami.forlaism.world.PlayerClockData;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * 追加能力用パケット。
 */
public class ClockExtraActionPacket {

    public enum Action {
        TOGGLE_AUTO_TOTEM,
        TOGGLE_AUTO_AIM,
        TELEPORT_LOOKED_AT,
        TELEPORT_TO_COORD,
        LOCATE_STRUCTURE,
        LOCATE_BIOME,
        /** 画面を開いた時に状態を要求するだけ（トグルしない）。 */
        REQUEST_STATE
    }

    private final Action action;
    private final double x, y, z;
    private final String id;

    public ClockExtraActionPacket(Action action) {
        this(action, 0, 0, 0, "");
    }

    public ClockExtraActionPacket(Action action, double x, double y, double z, String id) {
        this.action = action;
        this.x = x;
        this.y = y;
        this.z = z;
        this.id = id;
    }

    public ClockExtraActionPacket(FriendlyByteBuf buf) {
        this.action = Action.values()[buf.readVarInt()];
        this.x = buf.readDouble();
        this.y = buf.readDouble();
        this.z = buf.readDouble();
        this.id = buf.readUtf();
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeVarInt(action.ordinal());
        buf.writeDouble(x);
        buf.writeDouble(y);
        buf.writeDouble(z);
        buf.writeUtf(id);
    }

    public void handle(Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
            if (player == null) return;

            boolean hasClock = top.theillusivec4.curios.api.CuriosApi
                    .getCuriosInventory(player)
                    .map(h -> h.findFirstCurio(
                            s -> s.getItem() instanceof com.tyami.forlaism.item.MasterpieceClockItem
                    ).isPresent())
                    .orElse(false);

            if (!hasClock) return;

            switch (action) {
                case TOGGLE_AUTO_TOTEM -> {
                    PlayerClockData.toggleAutoTotem(player);
                    // 個別状態だけをこのプレイヤーに返す（全体broadcastはしない）
                    FactotumPacketHandler.CHANNEL.sendTo(
                            new ClockPlayerStateSyncPacket(
                                    PlayerClockData.isAutoTotem(player),
                                    PlayerClockData.isAutoAim(player)
                            ),
                            player.connection.connection,
                            net.minecraftforge.network.NetworkDirection.PLAY_TO_CLIENT
                    );
                }
                case TOGGLE_AUTO_AIM -> {
                    PlayerClockData.toggleAutoAim(player);
                    FactotumPacketHandler.CHANNEL.sendTo(
                            new ClockPlayerStateSyncPacket(
                                    PlayerClockData.isAutoTotem(player),
                                    PlayerClockData.isAutoAim(player)
                            ),
                            player.connection.connection,
                            net.minecraftforge.network.NetworkDirection.PLAY_TO_CLIENT
                    );
                }
                case TELEPORT_LOOKED_AT -> {
                    ClockTeleportHandler.teleportLookedAt(player, 64.0D);
                }
                case TELEPORT_TO_COORD -> {
                    ClockTeleportHandler.teleportTo(player, x, y, z);
                }
                case LOCATE_STRUCTURE -> {
                    ClockLocateHandler.locateStructure(player, id);
                }
                case LOCATE_BIOME -> {
                    ClockLocateHandler.locateBiome(player, id);
                }
                case REQUEST_STATE -> {
                    // トグルせず、現在の自分の状態だけ返す
                    FactotumPacketHandler.CHANNEL.sendTo(
                            new ClockPlayerStateSyncPacket(
                                    PlayerClockData.isAutoTotem(player),
                                    PlayerClockData.isAutoAim(player)
                            ),
                            player.connection.connection,
                            net.minecraftforge.network.NetworkDirection.PLAY_TO_CLIENT
                    );
                }
            }
        });
        ctx.get().setPacketHandled(true);
    }
}