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

                // ===== Masterpiece Clock 追加 =====
        CHANNEL.messageBuilder(ClockActionPacket.class, id(), NetworkDirection.PLAY_TO_SERVER)
                .decoder(ClockActionPacket::new)
                .encoder(ClockActionPacket::encode)
                .consumerMainThread(ClockActionPacket::handle)
                .add();

        CHANNEL.messageBuilder(ClockStateSyncPacket.class, id(), NetworkDirection.PLAY_TO_CLIENT)
                .decoder(ClockStateSyncPacket::new)
                .encoder(ClockStateSyncPacket::encode)
                .consumerMainThread(ClockStateSyncPacket::handle)
                .add();

        CHANNEL.messageBuilder(ClockSaveEntityPacket.class, id(), NetworkDirection.PLAY_TO_SERVER)
                .decoder(ClockSaveEntityPacket::new)
                .encoder(ClockSaveEntityPacket::encode)
                .consumerMainThread(ClockSaveEntityPacket::handle)
                .add();

        CHANNEL.messageBuilder(ClockEntityListSyncPacket.class, id(), NetworkDirection.PLAY_TO_CLIENT)
                .decoder(ClockEntityListSyncPacket::new)
                .encoder(ClockEntityListSyncPacket::encode)
                .consumerMainThread(ClockEntityListSyncPacket::handle)
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

        CHANNEL.messageBuilder(TimeFreezeSyncPacket.class, id(), NetworkDirection.PLAY_TO_CLIENT)
                .decoder(TimeFreezeSyncPacket::new)
                .encoder(TimeFreezeSyncPacket::encode)
                .consumerMainThread(TimeFreezeSyncPacket::handle)
                .add();

        CHANNEL.messageBuilder(JillstlairleatWaveTextPacket.class, id(), NetworkDirection.PLAY_TO_CLIENT)
                .decoder(JillstlairleatWaveTextPacket::new)
                .encoder(JillstlairleatWaveTextPacket::encode)
                .consumerMainThread(JillstlairleatWaveTextPacket::handle)
                .add();

        CHANNEL.messageBuilder(JillstlairleatDeathPacket.class, id(), NetworkDirection.PLAY_TO_CLIENT)
                .decoder(JillstlairleatDeathPacket::new)
                .encoder(JillstlairleatDeathPacket::encode)
                .consumerMainThread(JillstlairleatDeathPacket::handle)
                .add();

        CHANNEL.messageBuilder(JillstlairleatSelfDestructPacket.class, id(), NetworkDirection.PLAY_TO_CLIENT)
                .decoder(JillstlairleatSelfDestructPacket::new)
                .encoder(JillstlairleatSelfDestructPacket::encode)
                .consumerMainThread(JillstlairleatSelfDestructPacket::handle)
                .add();

        CHANNEL.messageBuilder(ClockExtraActionPacket.class, id(), NetworkDirection.PLAY_TO_SERVER)
                .decoder(ClockExtraActionPacket::new)
                .encoder(ClockExtraActionPacket::encode)
                .consumerMainThread(ClockExtraActionPacket::handle)
                .add();

        CHANNEL.messageBuilder(ClockPlayerStateSyncPacket.class, id(), NetworkDirection.PLAY_TO_CLIENT)
                .decoder(ClockPlayerStateSyncPacket::new)
                .encoder(ClockPlayerStateSyncPacket::encode)
                .consumerMainThread(ClockPlayerStateSyncPacket::handle)
                .add();

                // =========================================================
        // ハニワイニウム地震: 画面揺れ
        // =========================================================
        CHANNEL.messageBuilder(ScreenShakePacket.class, id(), NetworkDirection.PLAY_TO_CLIENT)
                .decoder(ScreenShakePacket::new)
                .encoder(ScreenShakePacket::encode)
                .consumerMainThread(ScreenShakePacket::handle)
                .add();

        

                // BloodMoon 同期
        CHANNEL.registerMessage(
          /*ID*/ 100,
          BloodMoonSyncPacket.class,
          BloodMoonSyncPacket::encode,
          BloodMoonSyncPacket::decode,
          BloodMoonSyncPacket::handle
        );
    }
}