package com.tyami.forlaism.network;

import com.tyami.forlaism.Forlaism;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;

public class FactotumPacketHandler {

    private static final String PROTOCOL_VERSION = "1";
    public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            new ResourceLocation(Forlaism.MOD_ID, "main"),
            () -> PROTOCOL_VERSION,
            PROTOCOL_VERSION::equals,
            PROTOCOL_VERSION::equals
    );

    private static int packetId = 0;

    private static int id() {
        return packetId++;
    }

    public static void register() {
        CHANNEL.messageBuilder(ChangeMiningRangePacket.class, id(), NetworkDirection.PLAY_TO_SERVER)
                .decoder(ChangeMiningRangePacket::new)
                .encoder(ChangeMiningRangePacket::encode)
                .consumerMainThread(ChangeMiningRangePacket::handle)
                .add();
    }
}
