package com.tyami.forlaism.network;

import com.tyami.forlaism.world.ClockTickManager;
import com.tyami.forlaism.world.GravityInversionManager;
import com.tyami.forlaism.world.WorldTimeFreezeManager;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * クロック画面の能力発動要求 (Client → Server)。
 */
public class ClockActionPacket {

    public enum Action {
        SET_WEATHER,
        SET_TIME,
        TOGGLE_TIME_FREEZE,
        TOGGLE_GRAVITY,
        TICK_FREEZE,
        TICK_UNFREEZE,
        TICK_RATE_INCREASE,
        TICK_RATE_DECREASE,
        TICK_STEP_20,
        TICK_STEP_100,
        TICK_SPRINT_100,
        TICK_SPRINT_STOP
    }

    private final Action action;
    /** SET_WEATHER / SET_TIME 用の値。 */
    private final int value;

    public ClockActionPacket(Action action) {
        this(action, 0);
    }

    public ClockActionPacket(Action action, int value) {
        this.action = action;
        this.value = value;
    }

    public ClockActionPacket(FriendlyByteBuf buf) {
        this.action = Action.values()[buf.readVarInt()];
        this.value = buf.readVarInt();
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeVarInt(action.ordinal());
        buf.writeVarInt(value);
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

            ServerLevel sl = player.serverLevel();
            var server = player.server;

            switch (action) {
                case SET_WEATHER -> {
                    // 0=晴れ, 1=雨, 2=雷雨
                    switch (value) {
                        case 0 -> {
                            sl.setWeatherParameters(12000, 0, false, false);
                            player.displayClientMessage(
                                    Component.literal("§e天候: §f晴れ"), true);
                        }
                        case 1 -> {
                            sl.setWeatherParameters(0, 6000, true, false);
                            player.displayClientMessage(
                                    Component.literal("§b天候: §f雨"), true);
                        }
                        case 2 -> {
                            sl.setWeatherParameters(0, 6000, true, true);
                            player.displayClientMessage(
                                    Component.literal("§5天候: §f雷雨"), true);
                        }
                    }
                    sl.playSound(null, player.getX(), player.getY(), player.getZ(),
                            net.minecraft.sounds.SoundEvents.BEACON_POWER_SELECT,
                            net.minecraft.sounds.SoundSource.PLAYERS, 1.0F, 1.5F);
                }
                case SET_TIME -> {
                    // 0=朝, 1=昼, 2=夕方, 3=夜
                    long target;
                    String label;
                    switch (value) {
                        case 0 -> { target = 0; label = "朝"; }
                        case 1 -> { target = 6000; label = "昼"; }
                        case 2 -> { target = 12000; label = "夕方"; }
                        case 3 -> { target = 18000; label = "夜"; }
                        default -> { return; }
                    }
                    sl.setDayTime(target);
                    player.displayClientMessage(
                            Component.literal("§e時間: §f" + label), true);
                    sl.playSound(null, player.getX(), player.getY(), player.getZ(),
                            net.minecraft.sounds.SoundEvents.BELL_BLOCK,
                            net.minecraft.sounds.SoundSource.PLAYERS, 1.0F, 1.5F);
                }
                case TOGGLE_TIME_FREEZE -> {
                    boolean nowFrozen = !WorldTimeFreezeManager.isFrozen();
                    WorldTimeFreezeManager.setFrozen(server, nowFrozen, player);
                }
                case TOGGLE_GRAVITY -> {
                    GravityInversionManager.toggle();
                    player.displayClientMessage(
                            Component.literal(
                                    GravityInversionManager.isActive()
                                            ? "§5重力反転: §cON"
                                            : "§5重力反転: §aOFF"
                            ), true);
                    sl.playSound(null, player.getX(), player.getY(), player.getZ(),
                            net.minecraft.sounds.SoundEvents.ENDERMAN_SCREAM,
                            net.minecraft.sounds.SoundSource.PLAYERS, 1.0F, 0.5F);
                }
                case TICK_FREEZE -> {
                    ClockTickManager.freeze(server);
                    player.displayClientMessage(
                            Component.literal("§bTick凍結: §cON"), true);
                }
                case TICK_UNFREEZE -> {
                    ClockTickManager.unfreeze(server);
                    player.displayClientMessage(
                            Component.literal("§bTick凍結: §aOFF"), true);
                }
                case TICK_RATE_INCREASE -> {
                    float rate = Math.min(10000.0F, ClockTickManager.getRate(server) * 2.0F);
                    ClockTickManager.setRate(server, rate);
                    player.displayClientMessage(
                            Component.literal("§bTick速度: §f" + rate), true);
                }
                case TICK_RATE_DECREASE -> {
                    float rate = Math.max(1.0F, ClockTickManager.getRate(server) / 2.0F);
                    ClockTickManager.setRate(server, rate);
                    player.displayClientMessage(
                            Component.literal("§bTick速度: §f" + rate), true);
                }
                case TICK_STEP_20 -> {
                    ClockTickManager.step(server, 20);
                    player.displayClientMessage(
                            Component.literal("§bTickステップ: §f20"), true);
                }
                case TICK_STEP_100 -> {
                    ClockTickManager.step(server, 100);
                    player.displayClientMessage(
                            Component.literal("§bTickステップ: §f100"), true);
                }
                case TICK_SPRINT_100 -> {
                    ClockTickManager.sprint(server, 100);
                    player.displayClientMessage(
                            Component.literal("§bTickスプリント: §f100"), true);
                }
                case TICK_SPRINT_STOP -> {
                    ClockTickManager.sprintStop(server);
                    player.displayClientMessage(
                            Component.literal("§bTickスプリント: §a停止"), true);
                }
            }

            ClockStateSyncPacket.broadcast(player.server);
        });
        ctx.get().setPacketHandled(true);
    }
}