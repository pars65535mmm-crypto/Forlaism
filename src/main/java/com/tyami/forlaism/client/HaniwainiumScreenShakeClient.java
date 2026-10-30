package com.tyami.forlaism.client;

import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.event.ViewportEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * 画面揺れ演出（クライアント側）。
 *
 * ScreenShakePacket を受信すると durationTicks の間、カメラを細かく揺らす。
 */
@OnlyIn(Dist.CLIENT)
@Mod.EventBusSubscriber(
        modid = "forlaism",
        value = Dist.CLIENT,
        bus = Mod.EventBusSubscriber.Bus.FORGE
)
public final class HaniwainiumScreenShakeClient {

    private static int remainingTicks = 0;
    private static float intensity = 0.0F;
    private static final java.util.Random RANDOM = new java.util.Random();

    private HaniwainiumScreenShakeClient() {
    }

    public static void start(int ticks, float intensity) {
        remainingTicks = ticks;
        HaniwainiumScreenShakeClient.intensity = intensity;
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        if (remainingTicks > 0) remainingTicks--;
    }

    @SubscribeEvent
    public static void onCameraAngles(ViewportEvent.ComputeCameraAngles event) {
        if (remainingTicks <= 0) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;

        float shake = intensity * 2.0F;
        float dy = (RANDOM.nextFloat() - 0.5F) * shake;
        float dp = (RANDOM.nextFloat() - 0.5F) * shake;

        event.setYaw(event.getYaw() + dy);
        event.setPitch(event.getPitch() + dp);
    }
}