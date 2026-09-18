package com.tyami.forlaism.client.halo;

import com.tyami.forlaism.registry.Items;
import com.tyami.watelib.text.AnimatedText;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.item.ItemStack;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderGuiOverlayEvent;
import net.minecraftforge.client.event.RenderTooltipEvent;
import net.minecraftforge.client.gui.overlay.VanillaGuiOverlay;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(
        modid = "forlaism",
        value = Dist.CLIENT,
        bus = Mod.EventBusSubscriber.Bus.FORGE
)
public final class HaloOfTheAdventTooltip {

    private static final long START_TIME_NANOS = System.nanoTime();

    private static int lastSelectedSlot = -1;
    private static long selectedItemNameStartNanos = Long.MIN_VALUE;

    private HaloOfTheAdventTooltip() {
    }

    /*
     * =========================================================
     * Tooltip
     * =========================================================
     */

    @SubscribeEvent
    public static void onRenderTooltip(RenderTooltipEvent.Pre event) {

        ItemStack stack = event.getItemStack();

        if (!stack.is(Items.HALO_OF_THE_ADVENT.get())) {
            return;
        }

        Minecraft minecraft = Minecraft.getInstance();
        GuiGraphics graphics = event.getGraphics();
        Font font = minecraft.font;

        AnimatedText title =
                AnimatedText.of("Halo of the Advent")
                        .wave(3.0f, 1.00f, 0.50f)
                        .gradient(0xFFFFFF, 0x00FFFF)
                        .gradientSpeed(1.00f)
                        .gradientPhase(0.50f);

        float animationTime = elapsedSeconds();

        /*
         * =====================================================
         * Tooltip本文
         * =====================================================
         */

        String[] lines = {
                "You have finally grasped a power that transcends the Gods.",
                "",
                "It is the very “object” left behind by the Monarch for the future.",
                "",
                "This “object” surpasses divinity; it decays not,",
                "withers not, breaks not, and possesses the strength",
                "to overcome all existence.",
                "",
                "How much of this “power” will you be able to unleash?",
                "",
                "Can you, in truth, shatter the wall known as the “limit”?"
        };

        /*
         * タイトル + 本文を描画するための高さ
         */
        int lineHeight = font.lineHeight;

        int titleWidth = title.getWidth(font);

        int bodyWidth = 0;

        for (String line : lines) {
            bodyWidth = Math.max(
                    bodyWidth,
                    font.width(line)
            );
        }

        int tooltipWidth =
                Math.max(titleWidth, bodyWidth);

        int tooltipHeight =
                lineHeight + 4
                        + lines.length * lineHeight;

        int x = event.getX();
        int y = event.getY();

        /*
         * =====================================================
         * 背景
         * =====================================================
         */

        graphics.pose().pushPose();
        graphics.pose().translate(0.0f, 0.0f, 400.0f);

        drawTooltipBackground(
                graphics,
                x,
                y,
                tooltipWidth,
                tooltipHeight
        );

        graphics.pose().popPose();

        /*
         * =====================================================
         * タイトル
         * =====================================================
         */

        graphics.pose().pushPose();
        graphics.pose().translate(0.0f, 0.0f, 401.0f);

        title.render(
                graphics,
                x,
                y,
                animationTime
        );

        graphics.pose().popPose();

        /*
         * =====================================================
         * 本文
         * =====================================================
         */

        int textY = y + lineHeight + 4;

        for (String line : lines) {

            if (!line.isEmpty()) {

                AnimatedText text =
                        AnimatedText.of(line)
                                .gradient(0xFFFFFF, 0xB0EFFF)
                                .gradientSpeed(0.35f)
                                .gradientPhase(0.15f);

                graphics.pose().pushPose();
                graphics.pose().translate(
                        0.0f,
                        0.0f,
                        401.0f
                );

                text.render(
                        graphics,
                        x,
                        textY,
                        animationTime
                );

                graphics.pose().popPose();

            }

            textY += lineHeight;
        }

        /*
         * バニラTooltipを完全に置き換える
         */
        event.setCanceled(true);
    }

    /*
     * =========================================================
     * 選択中アイテム名
     * =========================================================
     */

    @SubscribeEvent
    public static void onRenderItemName(
            RenderGuiOverlayEvent.Pre event) {

        /*
         * ITEM_NAME以外は触らない
         */
        if (!event.getOverlay()
                .id()
                .equals(VanillaGuiOverlay.ITEM_NAME.id())) {
            return;
        }

        Minecraft minecraft = Minecraft.getInstance();

        if (minecraft.player == null) {
            return;
        }

        ItemStack stack =
                minecraft.player.getMainHandItem();

        int selectedSlot =
                minecraft.player.getInventory().selected;

        /*
         * スロットが変わった瞬間に表示タイマーをリセット
         */
        if (selectedSlot != lastSelectedSlot) {

            lastSelectedSlot = selectedSlot;
            selectedItemNameStartNanos =
                    System.nanoTime();
        }

        /*
         * Halo of the Advent以外なら通常処理
         */
        if (!stack.is(Items.HALO_OF_THE_ADVENT.get())) {
            return;
        }

        /*
         * バニラのアイテム名表示時間は約2秒
         */
        if (System.nanoTime()
                - selectedItemNameStartNanos
                > 2_000_000_000L) {
            return;
        }

        AnimatedText text =
                AnimatedText.of("Halo of the Advent")
                        .wave(3.0f, 1.00f, 0.50f)
                        .gradient(0xFFFFFF, 0x00FFFF)
                        .gradientSpeed(1.00f)
                        .gradientPhase(0.50f);

        GuiGraphics graphics =
                event.getGuiGraphics();

        Font font =
                minecraft.font;

        int x =
                (event.getWindow().getGuiScaledWidth()
                        - text.getWidth(font))
                        / 2;

        int y =
                event.getWindow().getGuiScaledHeight()
                        - 59;

        graphics.pose().pushPose();
        graphics.pose().translate(
                0.0f,
                0.0f,
                400.0f
        );

        text.render(
                graphics,
                x,
                y,
                elapsedSeconds()
        );

        graphics.pose().popPose();

        /*
         * バニラの
         *
         * Halo of the Advent
         *
         * を描画させない
         */
        event.setCanceled(true);
    }

    /*
     * =========================================================
     * 時間
     * =========================================================
     */

    private static float elapsedSeconds() {

        return (System.nanoTime()
                - START_TIME_NANOS)
                / 1_000_000_000.0f;
    }

    /*
     * =========================================================
     * Tooltip背景
     * =========================================================
     */

    private static void drawTooltipBackground(
            GuiGraphics graphics,
            int x,
            int y,
            int width,
            int height) {

        int left = x - 4;
        int top = y - 5;

        int right =
                x + width + 4;

        int bottom =
                y + height + 4;

        /*
         * Minecraft風の暗い背景
         */
        int background =
                0xF0100010;

        /*
         * Haloなので青～シアン系
         */
        int border =
                0x5000FFFF;

        int borderDark =
                0x502080FF;

        graphics.fill(
                left,
                top,
                right,
                bottom,
                background
        );

        /*
         * 上
         */
        graphics.fillGradient(
                left,
                top,
                right,
                top + 1,
                border,
                border
        );

        /*
         * 下
         */
        graphics.fillGradient(
                left,
                bottom - 1,
                right,
                bottom,
                borderDark,
                borderDark
        );

        /*
         * 左
         */
        graphics.fillGradient(
                left,
                top,
                left + 1,
                bottom,
                border,
                borderDark
        );

        /*
         * 右
         */
        graphics.fillGradient(
                right - 1,
                top,
                right,
                bottom,
                border,
                borderDark
        );
    }
}