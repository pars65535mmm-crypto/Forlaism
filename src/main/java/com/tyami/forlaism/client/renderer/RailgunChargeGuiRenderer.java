package com.tyami.forlaism.client.renderer;

import com.tyami.forlaism.item.CherenkovMetaAdamanediumRailgunBladeItem;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderGuiOverlayEvent;
import net.minecraftforge.client.gui.overlay.VanillaGuiOverlay;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * レールガン チャージ中のGUI魔法陣・照準。
 *
 * TODO: シェーダー実装は次段階。
 * 今は簡易照準だけ描画。
 */
@Mod.EventBusSubscriber(
        modid = "forlaism",
        value = Dist.CLIENT,
        bus = Mod.EventBusSubscriber.Bus.FORGE
)
public final class RailgunChargeGuiRenderer {

    private RailgunChargeGuiRenderer() {
    }

    @SubscribeEvent
    public static void onRenderGui(RenderGuiOverlayEvent.Post event) {
        if (!event.getOverlay().id().equals(VanillaGuiOverlay.CROSSHAIR.id())) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;

        var stack = mc.player.getMainHandItem();
        if (!(stack.getItem() instanceof CherenkovMetaAdamanediumRailgunBladeItem)) return;
        if (!mc.player.isUsingItem()) return;

        int used = stack.getUseDuration() - mc.player.getUseItemRemainingTicks();

        float progress = Math.min(1.0F,
                used / (float) CherenkovMetaAdamanediumRailgunBladeItem.CHARGE_REQUIRED_TICKS);

        GuiGraphics g = event.getGuiGraphics();
        int w = event.getWindow().getGuiScaledWidth();
        int h = event.getWindow().getGuiScaledHeight();
        int cx = w / 2;
        int cy = h / 2;

        // 照準（円）
        int radius = 20;
        int segments = 64;
        for (int i = 0; i < segments; i++) {
            double a = i / (double) segments * Math.PI * 2;
            int px = cx + (int) (Math.cos(a) * radius);
            int py = cy + (int) (Math.sin(a) * radius);
            g.fill(px, py, px + 1, py + 1, 0x80A0E8FF);
        }

        // チャージバー（下部）
        int barW = 200;
        int barH = 6;
        int barX = cx - barW / 2;
        int barY = cy + 60;
        g.fill(barX, barY, barX + barW, barY + barH, 0x80000000);
        g.fill(barX, barY, barX + (int) (barW * progress), barY + barH,
                progress >= 1.0F ? 0xFFA0E8FF : 0xFF40A0C0);

        if (progress >= 1.0F) {
            g.drawString(mc.font, "§bREADY", cx - 20, barY + 10, 0xFFFFFF, true);
        }
    }
}