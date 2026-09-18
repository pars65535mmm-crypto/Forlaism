package com.tyami.forlaism.client;

import com.tyami.forlaism.registry.Items;
import com.tyami.watelib.text.AnimatedText;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.Font;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderTooltipEvent;
import net.minecraftforge.client.event.RenderGuiOverlayEvent;
import net.minecraftforge.client.gui.overlay.VanillaGuiOverlay;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(
        modid = "forlaism",
        value = Dist.CLIENT,
        bus = Mod.EventBusSubscriber.Bus.FORGE
)
public final class TrialHaloTooltipRenderer {

    /** Set this to false to use the previous transparent/no-background presentation. */
    public static boolean SHOW_TOOLTIP_BACKGROUND = true;
    private static final long START_TIME_NANOS = System.nanoTime();
    private static int lastSelectedSlot = -1;
    private static long selectedItemNameStartNanos = Long.MIN_VALUE;

    private TrialHaloTooltipRenderer() {
    }

    @SubscribeEvent
    public static void onRenderTooltip(RenderTooltipEvent.Pre event) {
        ItemStack stack = event.getItemStack();

        if (!stack.is(Items.TRIAL_HALO.get())) {
            return;
        }

        GuiGraphics graphics = event.getGraphics();
        Font font = Minecraft.getInstance().font;

        AnimatedText text = AnimatedText.of("試行乃光輪")
                .wave(3.0f, 1.00f, 0.50f)
                .gradient(0xFFFFFF, 0x00FFFF)
                .gradientSpeed(1.00f)
                .gradientPhase(0.50f);

        float animationTime = (System.nanoTime() - START_TIME_NANOS) / 1_000_000_000.0f;

        if (SHOW_TOOLTIP_BACKGROUND) {
            graphics.pose().pushPose();
            graphics.pose().translate(0.0f, 0.0f, 400.0f);
            drawTooltipBackground(graphics, event.getX(), event.getY(), text.getWidth(font), font.lineHeight);
            graphics.pose().popPose();
        }

        graphics.pose().pushPose();
        graphics.pose().translate(0.0f, 0.0f, 400.0f);
        text.render(
                graphics,
                event.getX(),
                event.getY(),
                animationTime
        );
        graphics.pose().popPose();

        // This custom renderer replaces the vanilla tooltip for this item.
        // Without cancelling Pre, Minecraft draws the normal item-name tooltip too.
        event.setCanceled(true);
    }

    @SubscribeEvent
    public static void onRenderItemName(RenderGuiOverlayEvent.Pre event) {
        if (!event.getOverlay().id().equals(VanillaGuiOverlay.ITEM_NAME.id())) {
            return;
        }

        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null) {
            return;
        }

        ItemStack stack = minecraft.player.getMainHandItem();
        int selectedSlot = minecraft.player.getInventory().selected;
        if (selectedSlot != lastSelectedSlot) {
            lastSelectedSlot = selectedSlot;
            selectedItemNameStartNanos = System.nanoTime();
        }

        if (!stack.is(Items.TRIAL_HALO.get())
                || System.nanoTime() - selectedItemNameStartNanos > 2_000_000_000L) {
            return;
        }

        AnimatedText text = AnimatedText.of("試行乃光輪")
                .wave(3.0f, 1.00f, 0.50f)
                .gradient(0xFFFFFF, 0x00FFFF)
                .gradientSpeed(1.00f)
                .gradientPhase(0.50f);

        GuiGraphics graphics = event.getGuiGraphics();
        Font font = minecraft.font;
        int x = (event.getWindow().getGuiScaledWidth() - text.getWidth(font)) / 2;
        int y = event.getWindow().getGuiScaledHeight() - 59;
        text.render(graphics, x, y, elapsedSeconds());
        event.setCanceled(true);
    }

    private static float elapsedSeconds() {
        return (System.nanoTime() - START_TIME_NANOS) / 1_000_000_000.0f;
    }

    private static void drawTooltipBackground(GuiGraphics graphics, int x, int y, int width, int height) {
        int left = x - 3;
        int top = y - 4;
        int right = x + width + 3;
        int bottom = y + height + 3;
        int background = 0xF0100010;
        int border = 0x505000FF;
        int borderDark = 0x5028007F;

        graphics.fill(left, top, right, bottom, background);
        graphics.fillGradient(left, top, right, top + 1, border, border);
        graphics.fillGradient(left, bottom - 1, right, bottom, borderDark, borderDark);
        graphics.fillGradient(left, top, left + 1, bottom, border, borderDark);
        graphics.fillGradient(right - 1, top, right, bottom, border, borderDark);
    }
}
