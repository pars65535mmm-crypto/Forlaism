package com.tyami.forlaism.client;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import java.util.ArrayList;
import java.util.List;

/**
 * 汎用の見開き本GUI。
 *
 * 使い方:
 *   BookScreen.builder()
 *       .title("日記")
 *       .text("Hodie mihi primum...")
 *       .headers(List.of("Dies I", "Dies II", ...))
 *       .open();
 *
 * 見開き1ページ = 左→右へ文章が流れる。
 * 文字はページ内で自動折り返し・自動ページ送り。
 */
@OnlyIn(Dist.CLIENT)
public class BookScreen extends Screen {

    // =========================================================
    // レイアウト定数
    // =========================================================

    private static final int BOOK_W = 240;
    private static final int BOOK_H = 180;
    private static final int PAGE_MARGIN = 6;
    private static final int SPINE_W = 8;

    private static final int TEXT_PADDING_X = 14;
    private static final int TEXT_PADDING_Y_TOP = 36;
    private static final int TEXT_PADDING_Y_BOTTOM = 22;
    private static final int LINE_SPACING = 2;

    // =========================================================
    // カラーパレット（デフォルト）
    // =========================================================

    private static final int DEFAULT_PARCHMENT         = 0xFFEFE0BC;
    private static final int DEFAULT_PARCHMENT_EDGE    = 0xFFC9B58A;
    private static final int DEFAULT_PARCHMENT_OUTLINE = 0xFF8B6F3E;
    private static final int DEFAULT_INK               = 0xFF4A3520;
    private static final int DEFAULT_INK_FAINT         = 0xFF7A6248;
    private static final int DEFAULT_COVER_DARK        = 0xFF2E1B10;
    private static final int DEFAULT_COVER_LIGHT       = 0xFF5C3A22;
    private static final int DEFAULT_RIBBON            = 0xFF8B2020;

    // =========================================================
    // 内容
    // =========================================================

    private final String rawText;
    private final List<String> headers;

    // カラー（Builder で差し替え可能）
    private int parchmentColor;
    private int parchmentEdgeColor;
    private int parchmentOutlineColor;
    private int inkColor;
    private int inkFaintColor;
    private int coverDarkColor;
    private int coverLightColor;
    private int ribbonColor;

    // =========================================================
    // 状態
    // =========================================================

    private int currentSpread = 0;
    private int totalSpreads = 1;

    private final List<List<FormattedCharSequence>> pages = new ArrayList<>();

    private float flipProgress = 0.0F;
    private int flipDirection = 0;

    // =========================================================
    // Builder
    // =========================================================

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String title = "Book";
        private String text = "";
        private List<String> headers = new ArrayList<>();
        private int parchmentColor = DEFAULT_PARCHMENT;
        private int parchmentEdgeColor = DEFAULT_PARCHMENT_EDGE;
        private int parchmentOutlineColor = DEFAULT_PARCHMENT_OUTLINE;
        private int inkColor = DEFAULT_INK;
        private int inkFaintColor = DEFAULT_INK_FAINT;
        private int coverDarkColor = DEFAULT_COVER_DARK;
        private int coverLightColor = DEFAULT_COVER_LIGHT;
        private int ribbonColor = DEFAULT_RIBBON;

        public Builder title(String title) {
            this.title = title;
            return this;
        }

        public Builder text(String text) {
            this.text = text;
            return this;
        }

        public Builder headers(List<String> headers) {
            this.headers = new ArrayList<>(headers);
            return this;
        }

        /** 羊皮紙・インク・表紙のカラーを一括で変えたい時に使う。 */
        public Builder palette(int parchment, int parchmentEdge, int parchmentOutline,
                               int ink, int inkFaint,
                               int coverDark, int coverLight, int ribbon) {
            this.parchmentColor = parchment;
            this.parchmentEdgeColor = parchmentEdge;
            this.parchmentOutlineColor = parchmentOutline;
            this.inkColor = ink;
            this.inkFaintColor = inkFaint;
            this.coverDarkColor = coverDark;
            this.coverLightColor = coverLight;
            this.ribbonColor = ribbon;
            return this;
        }

        public void open() {
            net.minecraft.client.Minecraft.getInstance().setScreen(
                    new BookScreen(
                            title, text, headers,
                            parchmentColor, parchmentEdgeColor, parchmentOutlineColor,
                            inkColor, inkFaintColor,
                            coverDarkColor, coverLightColor, ribbonColor
                    )
            );
        }
    }

    // =========================================================
    // コンストラクタ
    // =========================================================

    private BookScreen(
            String title, String text, List<String> headers,
            int parchmentColor, int parchmentEdgeColor, int parchmentOutlineColor,
            int inkColor, int inkFaintColor,
            int coverDarkColor, int coverLightColor, int ribbonColor
    ) {
        super(Component.literal(title));
        this.rawText = text == null ? "" : text;
        this.headers = (headers == null || headers.isEmpty())
                ? List.of("")
                : new ArrayList<>(headers);

        this.parchmentColor        = parchmentColor;
        this.parchmentEdgeColor    = parchmentEdgeColor;
        this.parchmentOutlineColor = parchmentOutlineColor;
        this.inkColor              = inkColor;
        this.inkFaintColor         = inkFaintColor;
        this.coverDarkColor        = coverDarkColor;
        this.coverLightColor       = coverLightColor;
        this.ribbonColor           = ribbonColor;
    }

    // =========================================================
    // init: レイアウト計算
    // =========================================================

    @Override
    protected void init() {
        super.init();
        layoutPages();
    }

    private void layoutPages() {
        pages.clear();

        int pageWidth = (BOOK_W - SPINE_W - PAGE_MARGIN * 3) / 2;
        int textMaxWidth = pageWidth - TEXT_PADDING_X * 2;
        int pageHeight = BOOK_H - PAGE_MARGIN * 2;
        int textMaxHeight = pageHeight - TEXT_PADDING_Y_TOP - TEXT_PADDING_Y_BOTTOM;
        int lineHeight = this.font.lineHeight + LINE_SPACING;
        int maxLinesPerPage = Math.max(1, textMaxHeight / lineHeight);

        // 見出しの分、最初のページだけ本文開始位置が下がる
        int firstPageTopOffset = 26;
        int firstPageMaxLines = Math.max(1,
                (textMaxHeight - firstPageTopOffset + TEXT_PADDING_Y_TOP) / lineHeight);

        List<FormattedCharSequence> allLines =
                this.font.split(Component.literal(rawText), textMaxWidth);

        List<FormattedCharSequence> currentPage = new ArrayList<>();
        int limit = firstPageMaxLines;

        for (FormattedCharSequence line : allLines) {
            if (currentPage.size() >= limit) {
                pages.add(new ArrayList<>(currentPage));
                currentPage.clear();
                limit = maxLinesPerPage;
            }
            currentPage.add(line);
        }
        if (!currentPage.isEmpty()) {
            pages.add(new ArrayList<>(currentPage));
        }
        if (pages.isEmpty()) {
            pages.add(new ArrayList<>());
        }

        totalSpreads = Math.max(1, (pages.size() + 1) / 2);
        totalSpreads = Math.min(totalSpreads, Math.max(1, headers.size()));
    }

    // =========================================================
    // render
    // =========================================================

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(graphics);

        if (flipProgress > 0.0F) {
            flipProgress = Math.max(0.0F, flipProgress - 0.06F);
        }

        int x = (this.width - BOOK_W) / 2;
        int y = (this.height - BOOK_H) / 2;

        drawBookFrame(graphics, x, y, BOOK_W, BOOK_H);

        int pageWidth = (BOOK_W - SPINE_W - PAGE_MARGIN * 3) / 2;
        int pageHeight = BOOK_H - PAGE_MARGIN * 2;

        int leftPageX = x + PAGE_MARGIN;
        int leftPageY = y + PAGE_MARGIN;
        int rightPageX = leftPageX + pageWidth + PAGE_MARGIN + SPINE_W;
        int rightPageY = leftPageY;

        drawParchmentPage(graphics, leftPageX, leftPageY, pageWidth, pageHeight, false);
        drawParchmentPage(graphics, rightPageX, rightPageY, pageWidth, pageHeight, true);

        // 見出し
        String header = headers.get(Math.min(currentSpread, headers.size() - 1));
        if (!header.isEmpty()) {
            int headerWidth = this.font.width(header);
            graphics.drawString(
                    this.font, header,
                    leftPageX + pageWidth / 2 - headerWidth / 2,
                    leftPageY + 12,
                    inkFaintColor, false
            );
            graphics.fill(
                    leftPageX + 16,
                    leftPageY + 26,
                    leftPageX + pageWidth - 16,
                    leftPageY + 27,
                    inkFaintColor
            );
        }

        // 左ページ本文
        int leftIndex = currentSpread * 2;
        if (leftIndex < pages.size()) {
            drawPageText(
                    graphics, pages.get(leftIndex),
                    leftPageX + TEXT_PADDING_X,
                    leftPageY + TEXT_PADDING_Y_TOP,
                    false
            );
        }

        // 右ページ本文 or 飾り
        int rightIndex = currentSpread * 2 + 1;
        if (rightIndex < pages.size()) {
            drawPageText(
                    graphics, pages.get(rightIndex),
                    rightPageX + TEXT_PADDING_X,
                    rightPageY + TEXT_PADDING_Y_TOP - 20,
                    true
            );
        } else {
            drawRightPageDecoration(graphics, rightPageX, rightPageY, pageWidth, pageHeight);
        }

        // ページ番号
        String pageNum = (currentSpread + 1) + " / " + totalSpreads;
        int pnWidth = this.font.width(pageNum);
        graphics.drawString(
                this.font, pageNum,
                rightPageX + pageWidth - pnWidth - 10,
                rightPageY + pageHeight - 14,
                inkFaintColor, false
        );

        if (flipProgress > 0.0F) {
            drawFlipOverlay(graphics, x, y, BOOK_W, BOOK_H);
        }

        drawNavigationHints(graphics, x, y, BOOK_W, BOOK_H);

        super.render(graphics, mouseX, mouseY, partialTick);
    }

    // =========================================================
    // 描画ヘルパ
    // =========================================================

    private void drawPageText(GuiGraphics graphics,
                              List<FormattedCharSequence> lines,
                              int textX, int textY,
                              boolean isRightPage) {

        int pageWidth = (BOOK_W - SPINE_W - PAGE_MARGIN * 3) / 2;
        int pageHeight = BOOK_H - PAGE_MARGIN * 2;
        int textMaxHeight = pageHeight - TEXT_PADDING_Y_TOP - TEXT_PADDING_Y_BOTTOM;
        int lineHeight = this.font.lineHeight + LINE_SPACING;

        int availableHeight = isRightPage
                ? textMaxHeight + 20
                : textMaxHeight;

        int maxLines = Math.max(1, availableHeight / lineHeight);

        int lineY = textY;
        int drawn = 0;
        for (FormattedCharSequence line : lines) {
            if (drawn >= maxLines) break;
            graphics.drawString(this.font, line, textX, lineY, inkColor, false);
            lineY += lineHeight;
            drawn++;
        }
    }

    private void drawBookFrame(GuiGraphics graphics, int x, int y, int w, int h) {
        graphics.fill(x - 3, y - 3, x + w + 3, y + h + 3, 0x66000000);
        graphics.fill(x, y, x + w, y + h, 0xFF3A2418);
        graphics.fillGradient(x + 2, y + 2, x + w - 2, y + h - 2,
                coverLightColor, coverDarkColor);
    }

    private void drawParchmentPage(GuiGraphics graphics, int x, int y, int w, int h, boolean isRight) {
        graphics.fill(x - 1, y - 1, x + w + 1, y + h + 1, parchmentOutlineColor);
        graphics.fill(x, y, x + w, y + h, parchmentColor);
        graphics.fillGradient(x, y, x + w, y + 12, parchmentEdgeColor, parchmentColor);
        graphics.fillGradient(x, y + h - 12, x + w, y + h, parchmentColor, parchmentEdgeColor);

        int shadowWidth = 5;
        if (isRight) {
            graphics.fillGradient(x, y, x + shadowWidth, y + h, 0x44000000, 0x00000000);
        } else {
            graphics.fillGradient(x + w - shadowWidth, y, x + w, y + h, 0x00000000, 0x44000000);
        }
    }

    private void drawRightPageDecoration(GuiGraphics graphics, int x, int y, int w, int h) {
        int cx = x + w / 2;
        int cy = y + h / 2 + 10;

        graphics.fill(cx - 8, cy - 6, cx + 8, cy + 6, 0x22A07040);
        graphics.fill(cx - 6, cy - 8, cx + 6, cy + 8, 0x22A07040);
        graphics.fill(cx - 4, cy - 4, cx + 4, cy + 4, 0x33A07040);

        int fx = x + w / 2 - 3;
        int fy = y + 32;
        graphics.fill(fx - 3, fy, fx + 3, fy + 1, 0xFFAA5577);
        graphics.fill(fx, fy - 3, fx + 1, fy + 4, 0xFFAA5577);
        graphics.fill(fx - 1, fy - 1, fx + 2, fy + 2, 0xFFFFDD66);

        graphics.fill(x + 6, y + 20, x + 7, y + h - 20, 0x33A07040);

        graphics.fill(x + w / 2 - 1, y - 2, x + w / 2 + 1, y + 26, ribbonColor);
        graphics.fill(x + w / 2 - 3, y + 24, x + w / 2 + 3, y + 28, ribbonColor);
    }

    private void drawFlipOverlay(GuiGraphics graphics, int x, int y, int w, int h) {
        int bandWidth = (int)(w * flipProgress);
        int fromX, toX;

        if (flipDirection > 0) {
            fromX = x + w - bandWidth;
            toX = x + w;
        } else {
            fromX = x;
            toX = x + bandWidth;
        }

        graphics.fillGradient(fromX, y, toX, y + h, 0xCCDDC9A0, 0x88DDC9A0);

        if (flipDirection > 0) {
            graphics.fillGradient(fromX - 4, y, fromX, y + h, 0x00000000, 0x55000000);
        } else {
            graphics.fillGradient(toX, y, toX + 4, y + h, 0x55000000, 0x00000000);
        }
    }

    private void drawNavigationHints(GuiGraphics graphics, int x, int y, int w, int h) {
        if (currentSpread > 0) {
            graphics.drawString(this.font, "◀", x - 14, y + h / 2 - 4, 0xCCCCCC, false);
        }
        if (currentSpread < totalSpreads - 1) {
            graphics.drawString(this.font, "▶", x + w + 6, y + h / 2 - 4, 0xCCCCCC, false);
        } else {
            String hint = "◀ クリックで閉じる ▶";
            int hw = this.font.width(hint);
            graphics.drawString(this.font, hint,
                    x + w / 2 - hw / 2, y + h + 10, 0xAAAAAA, false);
        }
    }

    // =========================================================
    // 入力
    // =========================================================

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0) {
            if (currentSpread < totalSpreads - 1) {
                currentSpread++;
                flipProgress = 1.0F;
                flipDirection = 1;
            } else {
                this.onClose();
            }
            return true;
        } else if (button == 1) {
            if (currentSpread > 0) {
                currentSpread--;
                flipProgress = 1.0F;
                flipDirection = -1;
            }
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}