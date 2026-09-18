package com.tyami.forlaism.network;

import com.tyami.forlaism.item.RegaliaOfTheFactotumItem;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class ChangeMiningRangePacket {

    private final int delta;

    public ChangeMiningRangePacket(int delta) {
        this.delta = delta;
    }

    public ChangeMiningRangePacket(FriendlyByteBuf buf) {
        this.delta = buf.readInt();
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeInt(this.delta);
    }

    public void handle(Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player == null) {
                return;
            }

            ItemStack stack = player.getMainHandItem();
            if (stack.getItem() instanceof RegaliaOfTheFactotumItem factotumItem) {
                if (factotumItem.getMode(stack) == RegaliaOfTheFactotumItem.Mode.PAXEL) {
                    int currentRange = factotumItem.getMiningRange(stack);
                    int newRange = Math.max(1, Math.min(16, currentRange + this.delta));
                    factotumItem.setMiningRange(stack, newRange);

                    player.displayClientMessage(
                            Component.literal("§6[Forlaism] §eMining Range: §f" + newRange + " × " + newRange + " × " + newRange),
                            true
                    );
                }
            }
        });
        context.setPacketHandled(true);
    }
}
