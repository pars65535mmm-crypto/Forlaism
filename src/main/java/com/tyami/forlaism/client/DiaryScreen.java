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
 * 少年の日記帳を表示するGUI。
 *
 * - 羊皮紙風の背景（コードで描画）
 * - 手書き風の茶色いインク
 * - 見開き1ページ = 左→右へ文章が流れる
 * - 文字はページ内で自動折り返し・自動ページ送り
 */
@OnlyIn(Dist.CLIENT)
public class DiaryScreen extends Screen {

    /** 日記の元テキスト。長文をまとめて書いてOK。 */
    private static final String RAW_TEXT =
            "Hodie mihi primum diarium datum est. " +
            "Pater mihi dixit me iam satis magnum esse ut res meas ipse scriberem. " +
            "Laetus eram, quod semper historias antiquas audire amo. " +
            "Postea avus mihi fabulam de antiquis deis narravit. " +
            "Dixit olim homines de Halo magno et de Domino, " +
            "qui supra mundum stare dicebatur, locutos esse. " +
            "Nescio utrum vera sint, sed mihi pulchra videbantur. " +
            "Deinde solus cum amicis ad montem post domum nostram ambulavi. " +
            "Folia vento movebantur et caelum erat clarum. " +
            "Hodie nihil mirabile accidit, sed volo omnia haec in hoc libro servare. " +
            "Fortasse aliquando, cum maior ero, " +
            "haec verba iterum legam et recordabor huius diei.";

    /** ページごとの見出し（見開きの左上に表示）。 */
    private static final List<String> HEADERS = List.of(
            "Dies I",
            "Dies II",
            "Dies III",
            "Dies IV",
            "Dies V"
    );

    // =========================================================
    // レイアウト定数
    // =========================================================

    /** 本全体のサイズ。 */
    private static final int BOOK_W = 240;
    private static final int BOOK_H = 180;

    /** ページの余白。 */
    private static final int PAGE_MARGIN = 6;
    private static final int SPINE_W = 8;

    /** 本文の左右パディング。 */
    private static final int TEXT_PADDING_X = 14;
    private static final int TEXT_PADDING_Y_TOP = 36;
    private static final int TEXT_PADDING_Y_BOTTOM = 22;

    /** 行間の追加スペース。 */
    private static final int LINE_SPACING = 2;

    // =========================================================
    // カラーパレット
    // =========================================================

    private static final int PARCHMENT       = 0xFFEFE0BC;
    private static final int PARCHMENT_EDGE  = 0xFFC9B58A;
    private static final int PARCHMENT_OUTLINE = 0xFF8B6F3E;
    private static final int INK             = 0xFF4A3520;
    private static final int INK_FAINT       = 0xFF7A6248;
    private static final int RIBBON          = 0xFF8B2020;

    // =========================================================
    // 状態
    // =========================================================

    /** 見開き（spread）単位で管理する。1 spread = 左ページ + 右ページ。 */
    private int currentSpread = 0;

    /** 全spread数（フォント計測後に確定）。 */
    private int totalSpreads = 1;

    /** 分割済みの本文（1ページあたりの行リスト）を保持。 */
    private final List<List<FormattedCharSequence>> pages = new ArrayList<>();

    /** ページめくりアニメーション。 */
    private float flipProgress = 0.0F;
    private int flipDirection = 0;

    // =========================================================
    // コンストラクタ
    // =========================================================

    public DiaryScreen() {
        super(Component.literal("日記"));
    }

    // =========================================================
    // init: フォントが使えるようになってからレイアウト計算
    // =========================================================

    @Override
    protected void init() {
        super.init();
        layoutPages();
    }

    /**
     * 本文を1ページずつに分割する。
     * 左ページに収まらなくなったら右ページへ、右ページも埋まったら次spreadへ。
     */
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

        // 全文を幅で折り返し
        List<FormattedCharSequence> allLines =
                this.font.split(Component.literal(RAW_TEXT), textMaxWidth);

        // 1ページずつ詰めていく
        List<FormattedCharSequence> currentPage = new ArrayList<>();
        int limit = firstPageMaxLines;

        for (FormattedCharSequence line : allLines) {
            if (currentPage.size() >= limit) {
                pages.add(new ArrayList<>(currentPage));
                currentPage.clear();
                // 2ページ目以降は見出しオフセットがないので通常の行数
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

        // 見開き数 = ページ数 ÷ 2（切り上げ）
        totalSpreads = Math.max(1, (pages.size() + 1) / 2);

        // 見出し数の方が多い場合はspread数を合わせる
        totalSpreads = Math.min(totalSpreads, Math.max(1, HEADERS.size()));
    }

    // =========================================================
    // render
    // =========================================================

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(graphics);

        // アニメ進行
        if (flipProgress > 0.0F) {
            flipProgress = Math.max(0.0F, flipProgress - 0.06F);
        }

        int x = (this.width - BOOK_W) / 2;
        int y = (this.height - BOOK_H) / 2;

        // 本の外枠
        drawBookFrame(graphics, x, y, BOOK_W, BOOK_H);

        // ページ寸法計算
        int pageWidth = (BOOK_W - SPINE_W - PAGE_MARGIN * 3) / 2;
        int pageHeight = BOOK_H - PAGE_MARGIN * 2;

        int leftPageX = x + PAGE_MARGIN;
        int leftPageY = y + PAGE_MARGIN;
        int rightPageX = leftPageX + pageWidth + PAGE_MARGIN + SPINE_W;
        int rightPageY = leftPageY;

        // 羊皮紙
        drawParchmentPage(graphics, leftPageX, leftPageY, pageWidth, pageHeight, false);
        drawParchmentPage(graphics, rightPageX, rightPageY, pageWidth, pageHeight, true);

        // 左ページの見出し
        String header = HEADERS.get(Math.min(currentSpread, HEADERS.size() - 1));
        int headerWidth = this.font.width(header);
        graphics.drawString(
                this.font,
                header,
                leftPageX + pageWidth / 2 - headerWidth / 2,
                leftPageY + 12,
                INK_FAINT,
                false
        );
        // 見出し下の飾り線
        graphics.fill(
                leftPageX + 16,
                leftPageY + 26,
                leftPageX + pageWidth - 16,
                leftPageY + 27,
                INK_FAINT
        );

        // 左ページ本文
        int leftIndex = currentSpread * 2;
        if (leftIndex < pages.size()) {
            drawPageText(
                    graphics,
                    pages.get(leftIndex),
                    leftPageX + TEXT_PADDING_X,
                    leftPageY + TEXT_PADDING_Y_TOP,
                    false // 見出しなし（もう上で描いた）
            );
        }

        // 右ページ本文
        int rightIndex = currentSpread * 2 + 1;
        if (rightIndex < pages.size()) {
            drawPageText(
                    graphics,
                    pages.get(rightIndex),
                    rightPageX + TEXT_PADDING_X,
                    rightPageY + TEXT_PADDING_Y_TOP - 20, // 右ページは見出しがない分上に詰める
                    true
            );
        } else {
            // 右ページが空なら押し花・しおりで飾る
            drawRightPageDecoration(graphics, rightPageX, rightPageY, pageWidth, pageHeight);
        }

        // ページ番号（見開きの右下に1つ）
        String pageNum = (currentSpread + 1) + " / " + totalSpreads;
        int pnWidth = this.font.width(pageNum);
        graphics.drawString(
                this.font,
                pageNum,
                rightPageX + pageWidth - pnWidth - 10,
                rightPageY + pageHeight - 14,
                INK_FAINT,
                false
        );

        // めくりアニメ
        if (flipProgress > 0.0F) {
            drawFlipOverlay(graphics, x, y, BOOK_W, BOOK_H);
        }

        // ナビゲーション
        drawNavigationHints(graphics, x, y, BOOK_W, BOOK_H);

        super.render(graphics, mouseX, mouseY, partialTick);
    }

    // =========================================================
    // 本文描画
    // =========================================================

    private void drawPageText(GuiGraphics graphics,
                              List<FormattedCharSequence> lines,
                              int textX, int textY,
                              boolean isRightPage) {

        int pageWidth = (BOOK_W - SPINE_W - PAGE_MARGIN * 3) / 2;
        int textMaxWidth = pageWidth - TEXT_PADDING_X * 2;
        int pageHeight = BOOK_H - PAGE_MARGIN * 2;
        int textMaxHeight = pageHeight - TEXT_PADDING_Y_TOP - TEXT_PADDING_Y_BOTTOM;
        int lineHeight = this.font.lineHeight + LINE_SPACING;

        // 右ページは見出しがない分、利用可能な高さが少し広い
        int availableHeight = isRightPage
                ? textMaxHeight + 20
                : textMaxHeight;

        int maxLines = Math.max(1, availableHeight / lineHeight);

        int lineY = textY;
        int drawn = 0;
        for (FormattedCharSequence line : lines) {
            if (drawn >= maxLines) break; // ★はみ出し防止
            graphics.drawString(this.font, line, textX, lineY, INK, false);
            lineY += lineHeight;
            drawn++;
        }
    }

    // =========================================================
    // 本の外枠
    // =========================================================

    private void drawBookFrame(GuiGraphics graphics, int x, int y, int w, int h) {
        // 影
        graphics.fill(x - 3, y - 3, x + w + 3, y + h + 3, 0x66000000);
        // 革表紙
        graphics.fill(x, y, x + w, y + h, 0xFF3A2418);
        graphics.fillGradient(x + 2, y + 2, x + w - 2, y + h - 2,
                0xFF5C3A22, 0xFF2E1B10);
    }

    // =========================================================
    // 羊皮紙ページ
    // =========================================================

    private void drawParchmentPage(GuiGraphics graphics, int x, int y, int w, int h, boolean isRight) {
        // 縁取り
        graphics.fill(x - 1, y - 1, x + w + 1, y + h + 1, PARCHMENT_OUTLINE);
        // ベース
        graphics.fill(x, y, x + w, y + h, PARCHMENT);
        // 上下の陰影
        graphics.fillGradient(x, y, x + w, y + 12, PARCHMENT_EDGE, PARCHMENT);
        graphics.fillGradient(x, y + h - 12, x + w, y + h, PARCHMENT, PARCHMENT_EDGE);

        // 背表紙側の影
        int shadowWidth = 5;
        if (isRight) {
            graphics.fillGradient(x, y, x + shadowWidth, y + h, 0x44000000, 0x00000000);
        } else {
            graphics.fillGradient(x + w - shadowWidth, y, x + w, y + h, 0x00000000, 0x44000000);
        }
    }

    // =========================================================
    // 右ページの飾り
    // =========================================================

    private void drawRightPageDecoration(GuiGraphics graphics, int x, int y, int w, int h) {
        int cx = x + w / 2;
        int cy = y + h / 2 + 10;

        // インク染み
        graphics.fill(cx - 8, cy - 6, cx + 8, cy + 6, 0x22A07040);
        graphics.fill(cx - 6, cy - 8, cx + 6, cy + 8, 0x22A07040);
        graphics.fill(cx - 4, cy - 4, cx + 4, cy + 4, 0x33A07040);

        // 押し花
        int fx = x + w / 2 - 3;
        int fy = y + 32;
        graphics.fill(fx - 3, fy, fx + 3, fy + 1, 0xFFAA5577);
        graphics.fill(fx, fy - 3, fx + 1, fy + 4, 0xFFAA5577);
        graphics.fill(fx - 1, fy - 1, fx + 2, fy + 2, 0xFFFFDD66);

        // 縦罫
        graphics.fill(x + 6, y + 20, x + 7, y + h - 20, 0x33A07040);

        // しおり
        graphics.fill(x + w / 2 - 1, y - 2, x + w / 2 + 1, y + 26, RIBBON);
        graphics.fill(x + w / 2 - 3, y + 24, x + w / 2 + 3, y + 28, RIBBON);
    }

    // =========================================================
    // めくりアニメ
    // =========================================================

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

    // =========================================================
    // ナビゲーション
    // =========================================================

    private void drawNavigationHints(GuiGraphics graphics, int x, int y, int w, int h) {
        if (currentSpread > 0) {
            graphics.drawString(this.font, "◀", x - 14, y + h / 2 - 4, 0xCCCCCC, false);
        }
        if (currentSpread < totalSpreads - 1) {
            graphics.drawString(this.font, "▶", x + w + 6, y + h / 2 - 4, 0xCCCCCC, false);
        } else {
            String hint = "◀ クリックで閉じる ▶";
            int hw = this.font.width(hint);
            graphics.drawString(
                    this.font, hint,
                    x + w / 2 - hw / 2,
                    y + h + 10,
                    0xAAAAAA,
                    false
            );
        }
    }

    // =========================================================
    // 入力
    // =========================================================

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0) {
            // 左クリック: 次の見開きへ
            if (currentSpread < totalSpreads - 1) {
                currentSpread++;
                flipProgress = 1.0F;
                flipDirection = 1;
            } else {
                this.onClose();
            }
            return true;
        } else if (button == 1) {
            // 右クリック: 前の見開きへ
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