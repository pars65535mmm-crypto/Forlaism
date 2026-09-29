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

        // 既存: ChangeMiningRangePacket
        CHANNEL.messageBuilder(ChangeMiningRangePacket.class, id(), NetworkDirection.PLAY_TO_SERVER)
                .decoder(ChangeMiningRangePacket::new)
                .encoder(ChangeMiningRangePacket::encode)
                .consumerMainThread(ChangeMiningRangePacket::handle)
                .add();

        // ★追加: CherenkovEffectPacket (SERVER → CLIENT)
        CHANNEL.messageBuilder(CherenkovEffectPacket.class, id(), NetworkDirection.PLAY_TO_CLIENT)
                .decoder(CherenkovEffectPacket::new)
                .encoder(CherenkovEffectPacket::encode)
                .consumerMainThread(CherenkovEffectPacket::handle)
                .add();

        CHANNEL.messageBuilder(RailgunLaserPacket.class, id(), NetworkDirection.PLAY_TO_CLIENT)
        .decoder(RailgunLaserPacket::new)
        .encoder(RailgunLaserPacket::encode)
        .consumerMainThread(RailgunLaserPacket::handle)
        .add();


                // ★追加: BossSyncPacket (SERVER → CLIENT)
        CHANNEL.messageBuilder(BossSyncPacket.class, id(), NetworkDirection.PLAY_TO_CLIENT)
                .decoder(BossSyncPacket::new)
                .encoder(BossSyncPacket::encode)
                .consumerMainThread(BossSyncPacket::handle)
                .add();
    }
}