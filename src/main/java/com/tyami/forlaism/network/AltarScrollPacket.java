package com.tyami.forlaism.network;

import com.tyami.forlaism.block.entity.AltarBlockEntity;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * クライアント → サーバー: 祭壇カーソルの移動。
 */
public class AltarScrollPacket {

    private final BlockPos pos;
    private final int dRow;
    private final int dCol;

    public AltarScrollPacket(BlockPos pos, int dRow, int dCol) {
        this.pos = pos;
        this.dRow = dRow;
        this.dCol = dCol;
    }

    public AltarScrollPacket(FriendlyByteBuf buf) {
        this.pos = buf.readBlockPos();
        this.dRow = buf.readInt();
        this.dCol = buf.readInt();
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeBlockPos(pos);
        buf.writeInt(dRow);
        buf.writeInt(dCol);
    }

    public void handle(Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
            if (player == null) return;

            if (player.distanceToSqr(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5) > 100) {
                return;
            }

            BlockEntity be = player.level().getBlockEntity(pos);
            if (!(be instanceof AltarBlockEntity altar)) return;

            altar.moveCursor(dRow, dCol);

            player.displayClientMessage(
                    Component.literal(
                            "§bカーソル §f(" + altar.getCursorCol() + ", " + altar.getCursorRow() + ")"),
                    true);
        });
        ctx.get().setPacketHandled(true);
    }
}