package com.tyami.forlaism.client;

import com.tyami.watelib.text.AnimatedText;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.event.RenderGuiOverlayEvent;
import net.minecraftforge.client.gui.overlay.VanillaGuiOverlay;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * wavetextlib を用いたジルスレイト専用の画面中央テキスト演出。
 *
 * チャットやアクションバーを使わず、画面上に直接幻想的な波テキストを表示する。
 */
@OnlyIn(Dist.CLIENT)
@Mod.EventBusSubscriber(
        modid = "forlaism",
        value = Dist.CLIENT,
        bus = Mod.EventBusSubscriber.Bus.FORGE
)
public final class JillstlairleatWaveTextRenderer {

    private static AnimatedText currentText = null;
    private static int remainingTicks = 0;
    private static int totalDuration = 60;

    private JillstlairleatWaveTextRenderer() {
    }

    /**
     * テキストの表示を開始する。
     *
     * @param message 表示する文字列
     * @param durationTicks 表示時間（デフォルト60tick = 3秒）
     */
    public static void showText(String message, int durationTicks) {
        // 物静かで神秘的なグラデーションと波のアニメーション
        currentText = AnimatedText.of(message)
                .wave(1.8f, 0.08f, 0.30f)
                .gradient(0xE0E0E0, 0x909090, 0xB0C4DE)
                .outline(0x000000, 1)
                .glow(0x40FFFFFF, 2);
        remainingTicks = durationTicks;
        totalDuration = durationTicks;
    }

    public static void showText(String message) {
        showText(message, 70);
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        if (remainingTicks > 0) {
            remainingTicks--;
            if (remainingTicks == 0) {
                currentText = null;
            }
        }
    }

    @SubscribeEvent
    public static void onRenderOverlay(RenderGuiOverlayEvent.Post event) {
        if (currentText == null || remainingTicks <= 0) return;
        if (!event.getOverlay().id().equals(VanillaGuiOverlay.HOTBAR.id())) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.options.hideGui) return;

        GuiGraphics graphics = event.getGuiGraphics();
        int screenWidth = event.getWindow().getGuiScaledWidth();
        int screenHeight = event.getWindow().getGuiScaledHeight();

        int textWidth = currentText.getWidth();
        int x = (screenWidth - textWidth) / 2;
        int y = screenHeight / 2 - 35; // 画面中央やや上

        currentText.render(graphics, x, y);
    }
}
