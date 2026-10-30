package com.tyami.forlaism.client.grimoire;

import com.tyami.forlaism.item.GrimoireItem;
import com.tyami.forlaism.lang.*;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import java.util.ArrayList;
import java.util.List;

/**
 * 魔道書の本文編集画面（複数行対応 + 署名前構文チェック）。
 */
@OnlyIn(Dist.CLIENT)
public class GrimoireEditScreen extends Screen {

    private final ItemStack stack;
    private final int slotIndex;

    private static final int BOOK_W = 320;
    private static final int BOOK_H = 240;

    private final List<String> pages = new ArrayList<>();
    private int currentPage = 0;

    private MultiLineEditor editor;
    private Button signButton;

    /** 構文チェックのエラーメッセージ（null ならエラーなし）。 */
    private String syntaxError = null;

    public GrimoireEditScreen(ItemStack stack, int slotIndex) {
        super(Component.literal("Grimoire Editor"));
        this.stack = stack;
        this.slotIndex = slotIndex;
        loadPages();
    }

    private void loadPages() {
        CompoundTag tag = stack.getTag();
        if (tag != null) {
            ListTag list = tag.getList(GrimoireItem.TAG_PAGES, 8);
            for (int i = 0; i < list.size(); i++) {
                pages.add(list.getString(i));
            }
        }
        if (pages.isEmpty()) {
            pages.add("");
        }
    }

    @Override
    protected void init() {
        super.init();

        int x = (this.width - BOOK_W) / 2;
        int y = (this.height - BOOK_H) / 2;

        int leftPageX = x + 14;
        int leftPageY = y + 34;

        // 多行エディタ
        this.editor = new MultiLineEditor(
                this.font,
                leftPageX, leftPageY,
                BOOK_W / 2 - 20, BOOK_H - 100,
                Component.literal("Spell text")
        );
        this.editor.setValue(pages.get(currentPage));
        this.addRenderableWidget(editor);

        // ページ切り替え
        int btnY = y + BOOK_H - 36;
        addRenderableWidget(Button.builder(
                Component.literal("◀"),
                b -> changePage(-1)
        ).bounds(x + 14, btnY, 30, 20).build());

        addRenderableWidget(Button.builder(
                Component.literal("▶"),
                b -> changePage(1)
        ).bounds(x + 14 + 34, btnY, 30, 20).build());

        // 構文チェックボタン
        addRenderableWidget(Button.builder(
                Component.literal("チェック"),
                b -> runSyntaxCheck()
        ).bounds(x + BOOK_W / 2 + 10, btnY, 70, 20).build());

        // 署名ボタン（構文エラーがあると押せない）
        this.signButton = Button.builder(
                Component.literal("署名する"),
                b -> openSignScreen()
        ).bounds(x + BOOK_W / 2 + 90, btnY, 80, 20).build();
        this.addRenderableWidget(signButton);


        // 初回チェック
        runSyntaxCheck();
    }

    private void changePage(int delta) {
        saveCurrentPage();
        int next = currentPage + delta;
        if (next < 0 || next >= pages.size()) return;
        currentPage = next;
        editor.setValue(pages.get(currentPage));
        runSyntaxCheck();
    }

    private void saveCurrentPage() {
        if (currentPage >= 0 && currentPage < pages.size()) {
            pages.set(currentPage, editor.getValue());
        }
    }

    /**
     * 構文チェック。
     *
     * 現在のページだけパースしてみる。
     * エラーがあれば syntaxError にメッセージを入れる。
     */
    private void runSyntaxCheck() {
        saveCurrentPage();

        String source = pages.get(currentPage);
        if (source == null || source.isBlank()) {
            syntaxError = null;
            if (signButton != null) signButton.active = true;
            return;
        }

        try {
            List<Token> tokens = new Lexer(source).tokenize();
            new Parser(tokens).parseProgram();
            syntaxError = null;
            if (signButton != null) signButton.active = true;
        } catch (LangException e) {
            syntaxError = Component.translatable(e.getMessageKey(), e.getArgs()).getString();
            if (signButton != null) signButton.active = false;
        } catch (Throwable t) {
            syntaxError = t.getMessage();
            if (signButton != null) signButton.active = false;
        }
    }

    private void openSignScreen() {
        saveCurrentPage();
        runSyntaxCheck();

        if (syntaxError != null) return;

        this.minecraft.setScreen(new GrimoireSignScreen(stack, pages));
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics);

        int x = (this.width - BOOK_W) / 2;
        int y = (this.height - BOOK_H) / 2;

        // 本の枠
        graphics.fill(x - 3, y - 3, x + BOOK_W + 3, y + BOOK_H + 3, 0x66000000);
        graphics.fill(x, y, x + BOOK_W, y + BOOK_H, 0xFF2E1B10);
        graphics.fillGradient(x + 2, y + 2, x + BOOK_W - 2, y + BOOK_H - 2,
                0xFFEFE0BC, 0xFFC9B58A);

        // タイトル
        graphics.drawCenteredString(
                this.font,
                "§d§l魔道書の編集",
                this.width / 2,
                y + 10,
                0xFFFFFFFF
        );

        // ページ番号
        graphics.drawString(
                this.font,
                "Page " + (currentPage + 1) + " / " + pages.size(),
                x + 14,
                y + 22,
                0xFF4A3520,
                false
        );

        // 区切り線
        graphics.fill(x + BOOK_W / 2 - 1, y + 24, x + BOOK_W / 2, y + BOOK_H - 40, 0xFF8B6F3E);

        // =========================================================
        // 右ページ: 構文チェック結果 + 例
        // =========================================================
        int rx = x + BOOK_W / 2 + 8;

        if (syntaxError == null) {
            graphics.drawString(
                    this.font,
                    "§a✔ 構文エラーなし",
                    rx, y + 34, 0xFF2E8B2E, false
            );
        } else {
            graphics.drawString(
                    this.font,
                    "§c✘ 構文エラー:",
                    rx, y + 34, 0xFFFF3333, false
            );

            // エラーメッセージを折り返して表示
            List<net.minecraft.util.FormattedCharSequence> wrapped =
                    this.font.split(Component.literal(syntaxError), BOOK_W / 2 - 24);
            int ey = y + 46;
            int maxLines = 6;
            for (int i = 0; i < Math.min(wrapped.size(), maxLines); i++) {
                graphics.drawString(this.font, wrapped.get(i), rx, ey, 0xFFFF5555, false);
                ey += 10;
            }
        }

        // 例
        graphics.drawString(
                this.font,
                "§8呪文の例:",
                rx,
                y + 110,
                0xFF7A6248,
                false
        );

        String[] examples = {
                "Ignite the nearest",
                "  zombie with fire.",
                "",
                "Loop for 100 ticks",
                "  Every 10 ticks,",
                "  heal the self with light."
        };

        int ey = y + 124;
        for (String ex : examples) {
            graphics.drawString(this.font, ex, rx, ey, 0xFF4A3520, false);
            ey += 10;
        }

        super.render(graphics, mouseX, mouseY, partialTick);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}