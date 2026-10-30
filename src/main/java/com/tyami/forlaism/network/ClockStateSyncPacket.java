package com.tyami.forlaism.network;

import com.tyami.forlaism.world.ClockTickManager;
import com.tyami.forlaism.world.GravityInversionManager;
import com.tyami.forlaism.world.PlayerClockData;
import com.tyami.forlaism.world.WorldTimeFreezeManager;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.PacketDistributor;

import java.util.function.Supplier;

/**
 * 全体状態の同期。autoTotem/autoAim は個別(ClockPlayerStateSyncPacket)管理に変更。
 */
public class ClockStateSyncPacket {

    public final boolean timeFrozen;
    public final boolean gravityInverted;
    public final boolean tickFrozen;
    public final float tickRate;
    public final boolean tickSprinting;
    public final String weatherLabel;
    public final String timeLabel;

    public ClockStateSyncPacket(boolean timeFrozen, boolean gravityInverted,
                                boolean tickFrozen, float tickRate, boolean tickSprinting,
                                String weatherLabel, String timeLabel) {
        this.timeFrozen = timeFrozen;
        this.gravityInverted = gravityInverted;
        this.tickFrozen = tickFrozen;
        this.tickRate = tickRate;
        this.tickSprinting = tickSprinting;
        this.weatherLabel = weatherLabel;
        this.timeLabel = timeLabel;
    }

    public ClockStateSyncPacket(FriendlyByteBuf buf) {
        this.timeFrozen = buf.readBoolean();
        this.gravityInverted = buf.readBoolean();
        this.tickFrozen = buf.readBoolean();
        this.tickRate = buf.readFloat();
        this.tickSprinting = buf.readBoolean();
        this.weatherLabel = buf.readUtf();
        this.timeLabel = buf.readUtf();
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeBoolean(timeFrozen);
        buf.writeBoolean(gravityInverted);
        buf.writeBoolean(tickFrozen);
        buf.writeFloat(tickRate);
        buf.writeBoolean(tickSprinting);
        buf.writeUtf(weatherLabel);
        buf.writeUtf(timeLabel);
    }

    public void handle(Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() ->
                DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () ->
                        com.tyami.forlaism.client.ClockStateCache.update(this)
                )
        );
        ctx.get().setPacketHandled(true);
    }

    public static void broadcast(MinecraftServer server) {
        ClockStateSyncPacket packet = build(server);
        FactotumPacketHandler.CHANNEL.send(
                PacketDistributor.ALL.noArg(), packet
        );
    }

    public static ClockStateSyncPacket build(MinecraftServer server) {
        var overworld = server.overworld();
        long time = overworld.getDayTime() % 24000L;
        String timeLabel;
        if (time < 6000) timeLabel = "朝";
        else if (time < 12000) timeLabel = "昼";
        else if (time < 18000) timeLabel = "夕方";
        else timeLabel = "夜";

        String weatherLabel;
        if (overworld.isThundering()) weatherLabel = "雷雨";
        else if (overworld.isRaining()) weatherLabel = "雨";
        else weatherLabel = "晴れ";

        return new ClockStateSyncPacket(
                WorldTimeFreezeManager.isFrozen(),
                GravityInversionManager.isActive(),
                ClockTickManager.isFrozen(server),
                ClockTickManager.getRate(server),
                ClockTickManager.isSprinting(server),
                weatherLabel,
                timeLabel
        );
    }

    public static void sendToPlayer(ServerPlayer player) {
        ClockStateSyncPacket packet = build(player.server);
        FactotumPacketHandler.CHANNEL.sendTo(
                packet,
                player.connection.connection,
                net.minecraftforge.network.NetworkDirection.PLAY_TO_CLIENT
        );
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