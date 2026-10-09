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

        CHANNEL.messageBuilder(BossSyncPacket.class, id(), NetworkDirection.PLAY_TO_CLIENT)
                .decoder(BossSyncPacket::new)
                .encoder(BossSyncPacket::encode)
                .consumerMainThread(BossSyncPacket::handle)
                .add();

        CHANNEL.messageBuilder(GrappleFirePacket.class, id(), NetworkDirection.PLAY_TO_SERVER)
                .decoder(GrappleFirePacket::new)
                .encoder(GrappleFirePacket::encode)
                .consumerMainThread(GrappleFirePacket::handle)
                .add();

        CHANNEL.messageBuilder(PrismLockPacket.class, id(), NetworkDirection.PLAY_TO_SERVER)
                .decoder(PrismLockPacket::new)
                .encoder(PrismLockPacket::encode)
                .consumerMainThread(PrismLockPacket::handle)
                .add();

        // ★ 追加: マスターピースクロック バインド画面
        CHANNEL.messageBuilder(OpenClockBindPacket.class, id(), NetworkDirection.PLAY_TO_SERVER)
                .decoder(OpenClockBindPacket::new)
                .encoder(OpenClockBindPacket::encode)
                .consumerMainThread(OpenClockBindPacket::handle)
                .add();

        CHANNEL.messageBuilder(OpenClockBindClientPacket.class, id(), NetworkDirection.PLAY_TO_CLIENT)
                .decoder(OpenClockBindClientPacket::new)
                .encoder(OpenClockBindClientPacket::encode)
                .consumerMainThread(OpenClockBindClientPacket::handle)
                .add();

        CHANNEL.messageBuilder(AltarScrollPacket.class, id(), NetworkDirection.PLAY_TO_SERVER)
                .decoder(AltarScrollPacket::new)
                .encoder(AltarScrollPacket::encode)
                .consumerMainThread(AltarScrollPacket::handle)
                .add();
    }
}