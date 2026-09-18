package com.tyami.forlaism.magic;

import com.mojang.blaze3d.platform.InputConstants;

import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.Pig;
import net.minecraft.world.phys.Vec3;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;

import org.lwjgl.glfw.GLFW;

import java.util.function.Supplier;

@Mod.EventBusSubscriber(
        modid = "forlaism",
        bus = Mod.EventBusSubscriber.Bus.FORGE,
        value = Dist.CLIENT
)
public final class MagicCircleSummoner {

    private static final String PROTOCOL = "1";

    public static final SimpleChannel CHANNEL =
            NetworkRegistry.newSimpleChannel(
                    new ResourceLocation(
                            "forlaism",
                            "magic"
                    ),
                    () -> PROTOCOL,
                    PROTOCOL::equals,
                    PROTOCOL::equals
            );

    private static final int REQUEST_ID = 0;
    private static final int START_ID = 1;

    private static boolean registered;

    public static final KeyMapping SUMMON =
            new KeyMapping(
                    "key.forlaism.magic_circle",
                    InputConstants.Type.KEYSYM,
                    GLFW.GLFW_KEY_UNKNOWN,
                    "key.categories.forlaism"
            );

    private MagicCircleSummoner() {
    }

    /*
     * ============================================================
     * Network registration
     * ============================================================
     */
    public static void registerNetwork() {
        if (registered) {
            return;
        }
        registered = true;

        CHANNEL.registerMessage(
                REQUEST_ID,
                SummonRequest.class,
                SummonRequest::encode,
                SummonRequest::decode,
                SummonRequest::handle
        );

        CHANNEL.registerMessage(
                START_ID,
                SummonStart.class,
                SummonStart::encode,
                SummonStart::decode,
                SummonStart::handle
        );
    }

    /*
     * ============================================================
     * Key mapping
     * ============================================================
     */
    @Mod.EventBusSubscriber(
            modid = "forlaism",
            bus = Mod.EventBusSubscriber.Bus.MOD,
            value = Dist.CLIENT
    )
    public static final class ClientRegistration {
        @SubscribeEvent
        public static void registerKeys(
                RegisterKeyMappingsEvent event
        ) {
            event.register(SUMMON);
        }
    }

    /*
     * ============================================================
     * Client tick
     * ============================================================
     */
    @SubscribeEvent
    public static void clientTick(
            TickEvent.ClientTickEvent event
    ) {
        if (
                event.phase
                        != TickEvent.Phase.END
        ) {
            return;
        }

        Minecraft mc =
                Minecraft.getInstance();

        if (mc.player == null) {
            return;
        }

        while (SUMMON.consumeClick()) {
            Vec3 origin =
                    mc.player.getEyePosition()
                            .add(
                                    mc.player
                                            .getLookAngle()
                                            .scale(5.0)
                            );

            Vec3 forward =
                    mc.player
                            .getLookAngle()
                            .normalize();

            /*
             * サーバーへ。
             *
             * 視線方向と5m先の位置を
             * 送る。
             */
            CHANNEL.sendToServer(
                    new SummonRequest(
                            origin,
                            forward
                    )
            );
        }
    }

    /*
     * ============================================================
     * Client ← Server
     * ============================================================
     */
    public record SummonStart(
            Vec3 origin,
            Vec3 forward,
            long startTick
    ) {
        public static void encode(
                SummonStart msg,
                FriendlyByteBuf buf
        ) {
            buf.writeDouble(msg.origin.x);
            buf.writeDouble(msg.origin.y);
            buf.writeDouble(msg.origin.z);
            buf.writeDouble(msg.forward.x);
            buf.writeDouble(msg.forward.y);
            buf.writeDouble(msg.forward.z);
            buf.writeLong(msg.startTick);
        }

        public static SummonStart decode(
                FriendlyByteBuf buf
        ) {
            Vec3 origin =
                    new Vec3(
                            buf.readDouble(),
                            buf.readDouble(),
                            buf.readDouble()
                    );
            Vec3 forward =
                    new Vec3(
                            buf.readDouble(),
                            buf.readDouble(),
                            buf.readDouble()
                    );
            long tick =
                    buf.readLong();

            return new SummonStart(
                    origin,
                    forward,
                    tick
            );
        }

        public static void handle(
                SummonStart msg,
                Supplier<NetworkEvent.Context> supplier
        ) {
            NetworkEvent.Context context =
                    supplier.get();

            context.enqueueWork(
                    () -> {
                        Minecraft mc =
                                Minecraft.getInstance();

                        if (mc.level == null) {
                            return;
                        }

                        MagicCircleState.summon(
                                msg.origin,
                                msg.forward,
                                msg.startTick
                        );
                    }
            );

            context.setPacketHandled(true);
        }
    }

    /*
     * ============================================================
     * Client → Server
     * ============================================================
     */
    public record SummonRequest(
            Vec3 origin,
            Vec3 forward
    ) {
        public static void encode(
                SummonRequest msg,
                FriendlyByteBuf buf
        ) {
            buf.writeDouble(msg.origin.x);
            buf.writeDouble(msg.origin.y);
            buf.writeDouble(msg.origin.z);
            buf.writeDouble(msg.forward.x);
            buf.writeDouble(msg.forward.y);
            buf.writeDouble(msg.forward.z);
        }

        public static SummonRequest decode(
                FriendlyByteBuf buf
        ) {
            return new SummonRequest(
                    new Vec3(
                            buf.readDouble(),
                            buf.readDouble(),
                            buf.readDouble()
                    ),
                    new Vec3(
                            buf.readDouble(),
                            buf.readDouble(),
                            buf.readDouble()
                    )
            );
        }

        public static void handle(
                SummonRequest msg,
                Supplier<NetworkEvent.Context> supplier
        ) {
            NetworkEvent.Context context =
                    supplier.get();

            ServerPlayer sender =
                    context.getSender();

            if (sender == null) {
                context.setPacketHandled(true);
                return;
            }

            context.enqueueWork(
                    () -> {
                        ServerLevel level =
                                sender.serverLevel();

                        /*
                         * =================================================
                         * セキュリティチェック
                         * =================================================
                         *
                         * クライアントが勝手に1000m先などを
                         * 指定できないようにする。
                         */
                        Vec3 expectedOrigin =
                                sender.getEyePosition()
                                        .add(
                                                sender
                                                        .getLookAngle()
                                                        .scale(5.0)
                                        );

                        if (
                                expectedOrigin
                                        .distanceTo(
                                                msg.origin
                                        )
                                        > 1.0
                        ) {
                            return;
                        }

                        /*
                         * 視線方向の異常値対策。
                         */
                        if (
                                msg.forward.lengthSqr()
                                        < 0.99
                                ||
                                msg.forward.lengthSqr()
                                        > 1.01
                        ) {
                            return;
                        }

                        long startTick =
                                level.getGameTime();

                        /*
                         * 全クライアントへ
                         * 同じ演出を送信。
                         */
                        CHANNEL.send(
                                PacketDistributor.ALL.noArg(),
                                new SummonStart(
                                        expectedOrigin,
                                        sender.getLookAngle()
                                                .normalize(),
                                        startTick
                                )
                        );

                        /*
                         * 豚はアニメーション終了時に
                         * サーバー側から召喚する。
                         *
                         * 6.9秒後。
                         */
                                                spawnPigLater(
                                level,
                                expectedOrigin,
                                sender
                                        .getLookAngle()
                                        .normalize(),
                                startTick
                        );
                    }
            );

            context.setPacketHandled(true);
        }
    }

    /*
     * ============================================================
     * 豚召喚
     * ============================================================
     */
    private static void spawnPigLater(
            ServerLevel level,
            Vec3 origin,
            Vec3 forward,
            long startTick
    ) {
        ScheduledPig.schedule(
                level,
                origin,
                forward,
                startTick
        );
    }

    /*
     * ============================================================
     * 遅延豚召喚
     * ============================================================
     */
    private static final class ScheduledPig {

        private static final java.util.List<Entry> ENTRIES =
                new java.util.ArrayList<>();

        static void schedule(
                ServerLevel level,
                Vec3 origin,
                Vec3 forward,
                long startTick
        ) {
            ENTRIES.add(
                    new Entry(
                            level,
                            origin,
                            forward,
                            startTick + 138
                    )
            );
        }

        private record Entry(
                ServerLevel level,
                Vec3 origin,
                Vec3 forward,
                long targetTick
        ) {
        }

        static {
            net.minecraftforge.common.MinecraftForge
                    .EVENT_BUS
                    .register(
                            ScheduledPig.class
                    );
        }

        @SubscribeEvent
        public static void tick(
                TickEvent.ServerTickEvent event
        ) {
            if (
                    event.phase
                            != TickEvent.Phase.END
            ) {
                return;
            }

            ENTRIES.removeIf(
                    entry -> {
                        if (
                                entry.level
                                        .getGameTime()
                                        < entry.targetTick
                        ) {
                            return false;
                        }

                        /*
                         * 大魔法陣：
                         *
                         * 7m先
                         *
                         * 豚：
                         *
                         * さらに1m先
                         */
                        Vec3 large =
                                entry.origin.add(
                                        entry.forward
                                                .scale(2.0)
                                );

                        Vec3 pigPos =
                                large.add(
                                        entry.forward
                                                .scale(1.0)
                                );

                        Pig pig =
                                EntityType.PIG
                                        .create(
                                                entry.level
                                        );

                        if (pig != null) {
                            pig.moveTo(
                                    pigPos.x,
                                    pigPos.y,
                                    pigPos.z,
                                    0.0F,
                                    0.0F
                            );

                            entry.level
                                    .addFreshEntity(
                                            pig
                                    );
                        }

                        return true;
                    }
            );
        }
    }
}