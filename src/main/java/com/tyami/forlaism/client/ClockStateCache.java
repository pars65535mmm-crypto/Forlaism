package com.tyami.forlaism.client;

import com.tyami.forlaism.network.ClockEntityListSyncPacket;
import com.tyami.forlaism.network.ClockPlayerStateSyncPacket;
import com.tyami.forlaism.network.ClockStateSyncPacket;
import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import java.util.ArrayList;
import java.util.List;

@OnlyIn(Dist.CLIENT)
public final class ClockStateCache {

    public static volatile boolean timeFrozen = false;
    public static volatile boolean gravityInverted = false;
    public static volatile boolean tickFrozen = false;
    public static volatile float tickRate = 20.0F;
    public static volatile boolean tickSprinting = false;
    public static volatile String weatherLabel = "晴れ";
    public static volatile String timeLabel = "朝";

    public static volatile boolean autoTotem = false;
    public static volatile boolean autoAim = false;

    public static volatile List<ClockEntityListSyncPacket.Entry> entityList = new ArrayList<>();

    private ClockStateCache() {
    }

    public static void update(ClockStateSyncPacket packet) {
        timeFrozen = packet.timeFrozen;
        gravityInverted = packet.gravityInverted;
        tickFrozen = packet.tickFrozen;
        tickRate = packet.tickRate;
        tickSprinting = packet.tickSprinting;
        weatherLabel = packet.weatherLabel;
        timeLabel = packet.timeLabel;
        // autoTotem / autoAim は ClockPlayerStateSyncPacket 側で更新する

        notifyScreen();
    }

    public static void updateEntityList(List<ClockEntityListSyncPacket.Entry> list) {
        entityList = new ArrayList<>(list);
        notifyScreen();
    }

    private static void notifyScreen() {
        Minecraft mc = Minecraft.getInstance();
        mc.execute(() -> {
            if (mc.screen instanceof ClockBindScreen screen) {
                screen.onStateChanged();
            }
        });
    }

    public static void updatePlayerState(ClockPlayerStateSyncPacket packet) {
        autoTotem = packet.autoTotem;
        autoAim = packet.autoAim;
        notifyScreen();
    }
}