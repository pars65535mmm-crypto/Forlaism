package com.tyami.forlaism.client;

import com.mojang.blaze3d.systems.RenderSystem;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.event.RenderGuiOverlayEvent;
import net.minecraftforge.client.gui.overlay.VanillaGuiOverlay;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import com.tyami.forlaism.client.ClientDarkenHandler;

/**
 * 画面暗転演出。
 *
 * 指定tickの間、画面全体を黒くフェードさせる。
 */
@OnlyIn(Dist.CLIENT)
@Mod.EventBusSubscriber(
        modid = "forlaism",
        value = Dist.CLIENT,
        bus = Mod.EventBusSubscriber.Bus.FORGE
)
public final class ClientDarkenHandler {

    /** 残りtick。 */
    private static int remainingTicks = 0;

    /** 総tick（フェード計算用）。 */
    private static int totalTicks = 0;

    private ClientDarkenHandler() {
    }

    /**
     * 暗転開始。
     */
    public static void start(int durationTicks) {
        remainingTicks = durationTicks;
        totalTicks = durationTicks;
    }

    // =========================================================
    // tick
    // =========================================================

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;

        if (remainingTicks > 0) {
            remainingTicks--;
        }
    }

    // =========================================================
    // 描画
    // =========================================================

    @SubscribeEvent
    public static void onRenderOverlay(RenderGuiOverlayEvent.Post event) {

        if (remainingTicks <= 0) return;
        if (!event.getOverlay().id().equals(VanillaGuiOverlay.HOTBAR.id())
                && !event.getOverlay().id().equals(VanillaGuiOverlay.CROSSHAIR.id())) {
            // 一番上のレイヤーで描画したいので、HOTBAR描画後に1回だけ描画
        }

        // ホットバーの後ろで1回だけ
        if (!event.getOverlay().id().equals(VanillaGuiOverlay.HOTBAR.id())) {
            return;
        }

        Minecraft mc = Minecraft.getInstance();
        if (mc.options.hideGui) return;

        GuiGraphics graphics = event.getGuiGraphics();

        int w = event.getWindow().getGuiScaledWidth();
        int h = event.getWindow().getGuiScaledHeight();

        // =========================================================
        // フェード計算
        // =========================================================
        float progress = (float) remainingTicks / (float) totalTicks;

        // 前半で暗くなり、後半で明るくなる
        float alpha;
        if (progress > 0.5F) {
            // 暗くなる（0 → 1）
            alpha = (1.0F - progress) * 2.0F;
        } else {
            // 明るくなる（1 → 0）
            alpha = progress * 2.0F;
        }

        alpha = Math.max(0.0F, Math.min(1.0F, alpha));

        int color = ((int) (alpha * 255.0F) << 24) | 0x000000;

        // =========================================================
        // 画面全体を黒で塗りつぶし
        // =========================================================
        graphics.fill(0, 0, w, h, color);
    }
}