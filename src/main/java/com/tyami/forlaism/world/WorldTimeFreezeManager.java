package com.tyami.forlaism.world;

import com.tyami.forlaism.network.FactotumPacketHandler;
import com.tyami.forlaism.network.TimeFreezeSyncPacket;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetSubtitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitlesAnimationPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.network.PacketDistributor;

/**
 * ワールドの時間停止 + tick制御を統括する。
 *
 * - freeze / step / sprint / rate
 * - ServerLevelTickControlMixin が ServerLevel.tick() を
 *   computeTickCount() 回呼ぶ
 */
@Mod.EventBusSubscriber(modid = "forlaism")
public final class WorldTimeFreezeManager {

    // =========================================================
    // 状態
    // =========================================================

    private static volatile boolean FROZEN = false;
    private static volatile boolean CLIENT_FROZEN = false;

    /** 現在のtick rate（1.0 = 通常, 20.0 がデフォルト）。 */
    private static volatile float TICK_RATE = 20.0F;

    /** freeze中の「残りstep数」。 */
    private static volatile int STEP_REMAINING = 0;

    /** sprint残りtick。 */
    private static volatile int SPRINT_REMAINING = 0;

    /** rate<20用の端数アキュムレータ。 */
    private static double tickAccumulator = 0.0;

    private WorldTimeFreezeManager() {
    }

    // =========================================================
    // 既存API
    // =========================================================

    public static boolean isFrozen() {
        return FROZEN;
    }

    public static boolean isClientFrozen() {
        return CLIENT_FROZEN;
    }

    public static void setClientFrozen(boolean frozen) {
        CLIENT_FROZEN = frozen;
        FROZEN = frozen;
    }

    @SubscribeEvent
    public static void onServerStarted(ServerStartedEvent event) {
        MinecraftServer server = event.getServer();
        TimeFreezeSavedData data = TimeFreezeSavedData.get(server);
        FROZEN = data.isFrozen();
    }

    @SubscribeEvent
    public static void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer sp) {
            FactotumPacketHandler.CHANNEL.send(
                    PacketDistributor.PLAYER.with(() -> sp),
                    new TimeFreezeSyncPacket(FROZEN)
            );
        }
    }

    public static void toggleFrozen(MinecraftServer server, Player activator) {
        setFrozen(server, !FROZEN, activator);
    }

    public static void setFrozen(MinecraftServer server, boolean frozen, Player activator) {
        FROZEN = frozen;
        TimeFreezeSavedData.get(server).setFrozen(frozen);

        FactotumPacketHandler.CHANNEL.send(
                PacketDistributor.ALL.noArg(),
                new TimeFreezeSyncPacket(frozen)
        );

        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            if (frozen) {
                player.connection.send(new ClientboundSetTitlesAnimationPacket(10, 70, 20));
                player.connection.send(new ClientboundSetTitleTextPacket(
                        Component.literal("【絶対時間凍結】").withStyle(ChatFormatting.AQUA, ChatFormatting.BOLD)
                ));
                player.connection.send(new ClientboundSetSubtitleTextPacket(
                        Component.literal("世界の全事象は停止した").withStyle(ChatFormatting.WHITE)
                ));
                player.playNotifySound(SoundEvents.END_PORTAL_SPAWN, SoundSource.PLAYERS, 1.2F, 0.5F);
                player.playNotifySound(SoundEvents.GLASS_BREAK, SoundSource.PLAYERS, 1.5F, 0.5F);
            } else {
                player.connection.send(new ClientboundSetTitlesAnimationPacket(10, 50, 15));
                player.connection.send(new ClientboundSetTitleTextPacket(
                        Component.literal("【凍結解除】").withStyle(ChatFormatting.GREEN, ChatFormatting.BOLD)
                ));
                player.connection.send(new ClientboundSetSubtitleTextPacket(
                        Component.literal("世界の時間が再び動き出す").withStyle(ChatFormatting.WHITE)
                ));
                player.playNotifySound(SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 1.0F, 1.2F);
            }
        }

        if (activator instanceof ServerPlayer sp) {
            ServerLevel sl = sp.serverLevel();
            sl.sendParticles(ParticleTypes.FLASH, sp.getX(), sp.getY() + 1.0, sp.getZ(), 8, 0.5, 0.5, 0.5, 0.0);
            sl.sendParticles(ParticleTypes.SNOWFLAKE, sp.getX(), sp.getY() + 1.0, sp.getZ(), 150, 2.0, 2.0, 2.0, 0.1);
        }
    }

    // =========================================================
    // tick制御API（Mixinから叩く）
    // =========================================================

    public static float getTickRate() {
        return SPRINT_REMAINING > 0 ? 10000.0F : TICK_RATE;
    }

    public static void setTickRate(MinecraftServer server, float rate) {
        TICK_RATE = Math.max(1.0F, Math.min(10000.0F, rate));
        tickAccumulator = 0.0;
    }

    public static void requestStep(MinecraftServer server, int ticks) {
        STEP_REMAINING += Math.max(1, ticks);
    }

    public static void requestSprint(MinecraftServer server, int ticks) {
        SPRINT_REMAINING = Math.max(1, ticks);
    }

    public static void stopSprint(MinecraftServer server) {
        SPRINT_REMAINING = 0;
    }

    public static boolean isSprinting() {
        return SPRINT_REMAINING > 0;
    }

    /**
     * ServerLevel.tick() を1tickあたり何回呼ぶか。
     *
     * - sprint中 → 最大
     * - freeze中 → step残があれば1回、なければ0回
     * - rate<20  → アキュムレータで間引き
     * - rate==20 → 1回
     * - rate>20  → rate/20 回（端数アキュムレータ）
     */
    public static int computeTickCount() {
        if (SPRINT_REMAINING > 0) {
            SPRINT_REMAINING--;
            return 1000;
        }

        if (FROZEN) {
            if (STEP_REMAINING > 0) {
                STEP_REMAINING--;
                return 1;
            }
            return 0;
        }

        float rate = TICK_RATE;

        if (rate == 20.0F) {
            return 1;
        }

        if (rate < 20.0F) {
            tickAccumulator += rate / 20.0F;
            if (tickAccumulator >= 1.0) {
                tickAccumulator -= 1.0;
                return 1;
            }
            return 0;
        }

        // rate > 20
        int count = (int) Math.floor(rate / 20.0F);
        tickAccumulator += (rate / 20.0F) - count;
        if (tickAccumulator >= 1.0) {
            tickAccumulator -= 1.0;
            count++;
        }
        return Math.max(1, Math.min(1000, count));
    }
}