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
public final class HaloOfTheFirmamentTooltip {

    private static final long START_TIME_NANOS = System.nanoTime();

    private static int lastSelectedSlot = -1;
    private static long selectedItemNameStartNanos = Long.MIN_VALUE;

    private HaloOfTheFirmamentTooltip() {
    }

    /*
     * =========================================================
     * Tooltip本文
     * =========================================================
     */

    private static final String[] TOOLTIP_LINES = {

            "The Primal Monarch was shielded by the Creator’s grace,",
            "loathing, detesting, yet secretly relishing the blessing.",

            "",

            "When the Monarch falls, the Creator shall face demise.",
            "When the Creator perishes, the Monarch truly begins.",

            "",

            "To command the Demons, one must first slay the Hero.",
            "To command the Angels, one must first slay the Champion.",
            "To command the Pixies, one must consign their closest ally to the Abyss.",
            "To command the Spirits, one must send the masses to the Underworld.",
            "To command the Gods, one must lure the multitude into the Netherrealm.",

            "",

            "The Wanderer is guided only by those who have abandoned all things.",
            "To draw nigh unto the Gods, one must become a Jester.",
            "To draw nigh unto the Primordial, one cannot but become a Fool.",
            "The Devourer, in time, shall consume even himself."
    };

    /*
     * =========================================================
     * Tooltip
     * =========================================================
     */

    @SubscribeEvent
    public static void onRenderTooltip(RenderTooltipEvent.Pre event) {

        ItemStack stack = event.getItemStack();

        if (!stack.is(Items.HALO_OF_THE_FIRMAMENT.get())) {
            return;
        }

        Minecraft minecraft = Minecraft.getInstance();

        GuiGraphics graphics = event.getGraphics();
        Font font = minecraft.font;

        float animationTime = elapsedSeconds();

        /*
         * -----------------------------------------------------
         * タイトル
         * -----------------------------------------------------
         */

        AnimatedText title =
                AnimatedText.of("Halo of the Firmament")
                        .wave(3.0f, 1.00f, 0.50f)
                        .gradient(0xFF7A00, 0xFFD700)
                        .gradientSpeed(0.80f)
                        .gradientPhase(0.50f);

        /*
         * -----------------------------------------------------
         * 本文の幅
         * -----------------------------------------------------
         */

        int lineHeight = font.lineHeight;

        int tooltipWidth = title.getWidth(font);

        for (String line : TOOLTIP_LINES) {

            if (!line.isEmpty()) {

                AnimatedText text =
                        AnimatedText.of(line)
                                .wave(2.0f, 0.75f, 0.20f)
                                .gradient(0xFF8A00, 0xFFD700)
                                .gradientSpeed(0.35f)
                                .gradientPhase(0.15f);

                tooltipWidth =
                        Math.max(
                                tooltipWidth,
                                text.getWidth(font)
                        );
            }
        }

        /*
         * -----------------------------------------------------
         * 高さ
         * -----------------------------------------------------
         */

        int tooltipHeight =
                lineHeight
                        + 4
                        + TOOLTIP_LINES.length * lineHeight;

        int x = event.getX();
        int y = event.getY();

        /*
         * -----------------------------------------------------
         * 背景
         * -----------------------------------------------------
         */

        graphics.pose().pushPose();

        graphics.pose().translate(
                0.0f,
                0.0f,
                400.0f
        );

        drawTooltipBackground(
                graphics,
                x,
                y,
                tooltipWidth,
                tooltipHeight
        );

        graphics.pose().popPose();

        /*
         * -----------------------------------------------------
         * タイトル
         * -----------------------------------------------------
         */

        graphics.pose().pushPose();

        graphics.pose().translate(
                0.0f,
                0.0f,
                401.0f
        );

        title.render(
                graphics,
                x,
                y,
                animationTime
        );

        graphics.pose().popPose();

        /*
         * -----------------------------------------------------
         * 本文
         * -----------------------------------------------------
         */

        int textY =
                y
                        + lineHeight
                        + 4;

        for (String line : TOOLTIP_LINES) {

            if (!line.isEmpty()) {

                AnimatedText text =
                        AnimatedText.of(line)
                                .wave(
                                        2.0f,
                                        0.75f,
                                        0.20f
                                )
                                .gradient(
                                        0xFF8A00,
                                        0xFFD700
                                )
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
         * -----------------------------------------------------
         * バニラTooltipをキャンセル
         * -----------------------------------------------------
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

        if (!event.getOverlay()
                .id()
                .equals(VanillaGuiOverlay.ITEM_NAME.id())) {

            return;
        }

        Minecraft minecraft =
                Minecraft.getInstance();

        if (minecraft.player == null) {
            return;
        }

        ItemStack stack =
                minecraft.player.getMainHandItem();

        int selectedSlot =
                minecraft.player.getInventory().selected;

        /*
         * -----------------------------------------------------
         * スロット変更検知
         * -----------------------------------------------------
         */

        if (selectedSlot != lastSelectedSlot) {

            lastSelectedSlot =
                    selectedSlot;

            selectedItemNameStartNanos =
                    System.nanoTime();
        }

        /*
         * -----------------------------------------------------
         * Firmament以外は何もしない
         * -----------------------------------------------------
         */

        if (!stack.is(
                Items.HALO_OF_THE_FIRMAMENT.get()
        )) {
            return;
        }

        /*
         * -----------------------------------------------------
         * バニラの表示時間
         * -----------------------------------------------------
         */

        if (System.nanoTime()
                - selectedItemNameStartNanos
                > 2_000_000_000L) {

            return;
        }

        /*
         * -----------------------------------------------------
         * アイテム名
         * -----------------------------------------------------
         */

        AnimatedText text =
                AnimatedText.of(
                        "Halo of the Firmament"
                )
                .wave(
                        3.0f,
                        1.00f,
                        0.50f
                )
                .gradient(
                        0xFF7A00,
                        0xFFD700
                )
                .gradientSpeed(0.80f)
                .gradientPhase(0.50f);

        GuiGraphics graphics =
                event.getGuiGraphics();

        Font font =
                minecraft.font;

        /*
         * -----------------------------------------------------
         * 中央揃え
         * -----------------------------------------------------
         */

        int x =
                (
                        event.getWindow()
                                .getGuiScaledWidth()
                        - text.getWidth(font)
                ) / 2;

        int y =
                event.getWindow()
                        .getGuiScaledHeight()
                - 59;

        /*
         * -----------------------------------------------------
         * 描画
         * -----------------------------------------------------
         */

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
         * -----------------------------------------------------
         * バニラ名を消す
         * -----------------------------------------------------
         */

        event.setCanceled(true);
    }

    /*
     * =========================================================
     * 時間
     * =========================================================
     */

    private static float elapsedSeconds() {

        return (
                System.nanoTime()
                        - START_TIME_NANOS
        ) / 1_000_000_000.0f;
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

        int left =
                x - 4;

        int top =
                y - 5;

        int right =
                x + width + 4;

        int bottom =
                y + height + 4;

        /*
         * -----------------------------------------------------
         * 背景
         * -----------------------------------------------------
         */

        int background =
                0xF0100800;

        /*
         * オレンジ
         */

        int orange =
                0xFFFF8A00;

        /*
         * 金
         */

        int gold =
                0xFFFFD700;

        /*
         * 黒
         */

        int black =
                0xFF120700;

        /*
         * -----------------------------------------------------
         * 本体
         * -----------------------------------------------------
         */

        graphics.fill(
                left,
                top,
                right,
                bottom,
                background
        );

        /*
         * -----------------------------------------------------
         * 上端
         * オレンジ → 金
         * -----------------------------------------------------
         */

        graphics.fillGradient(
                left,
                top,
                right,
                top + 1,
                orange,
                gold
        );

        /*
         * -----------------------------------------------------
         * 下端
         * 金 → 黒
         * -----------------------------------------------------
         */

        graphics.fillGradient(
                left,
                bottom - 1,
                right,
                bottom,
                gold,
                black
        );

        /*
         * -----------------------------------------------------
         * 左端
         * -----------------------------------------------------
         */

        graphics.fillGradient(
                left,
                top,
                left + 1,
                bottom,
                orange,
                black
        );

        /*
         * -----------------------------------------------------
         * 右端
         * -----------------------------------------------------
         */

        graphics.fillGradient(
                right - 1,
                top,
                right,
                bottom,
                gold,
                black
        );
    }
}