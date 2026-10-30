package com.tyami.forlaism.client;

import com.tyami.forlaism.world.BloodMoonManager;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.util.Mth;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ViewportEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * ブラッドムーン中のフォグ演出。
 */
@Mod.EventBusSubscriber(
        modid = "forlaism",
        value = Dist.CLIENT,
        bus = Mod.EventBusSubscriber.Bus.FORGE
)
public final class BloodMoonClientHandler {

    private BloodMoonClientHandler() {
    }

    private static boolean active() {
        Minecraft mc = Minecraft.getInstance();
        ClientLevel level = mc.level;
        if (level == null) return false;
        if (!BloodMoonManager.isClientActive()) return false;

        long time = level.getDayTime() % 24000L;
        return time >= 13000L && time < 23000L;
    }

    @SubscribeEvent
    public static void onFogColor(ViewportEvent.ComputeFogColor event) {
        if (!active()) return;

        event.setRed(Mth.lerp(0.7F, event.getRed(), 0.9F));
        event.setGreen(Mth.lerp(0.7F, event.getGreen(), 0.05F));
        event.setBlue(Mth.lerp(0.7F, event.getBlue(), 0.05F));
    }

    @SubscribeEvent
    public static void onRenderFog(ViewportEvent.RenderFog event) {
        if (!active()) return;

        event.setNearPlaneDistance(0.0F);
        event.setFarPlaneDistance(80.0F);
        event.setFogShape(com.mojang.blaze3d.shaders.FogShape.SPHERE);
        event.setCanceled(true);
    }
}