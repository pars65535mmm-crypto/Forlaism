package com.tyami.forlaism.client.grimoire;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarratedElementType;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;

/**
 * 自前の複数行テキストエディタ。
 *
 * 機能:
 *   - 文字入力（charTyped）
 *   - Backspace / Delete
 *   - Enter（改行）
 *   - ←→↑↓（カーソル移動）
 *   - Home / End
 *   - Shift + 矢印（選択）
 *   - Ctrl+A（全選択）
 *   - Ctrl+C / Ctrl+V / Ctrl+X
 *   - 折り返し描画（横スクロールしない）
 *
 * 折り返し:
 *   width を超える文字は自動で次の行に折り返す。
 *   ただし内部的には「論理行」は \n だけで区切られ、
 *   「表示行」は折り返しで増える。
 *
 *   カーソル位置は「論理インデックス」で管理し、
 *   描画時だけ「表示行・表示列」に変換する。
 */
public class MultiLineEditor extends AbstractWidget {

    private final Font font;
    private final StringBuilder text = new StringBuilder();
    private int cursor = 0;

    /** 選択開始位置（Shift+矢印 / Ctrl+A 用）。-1 なら選択なし。 */
    private int selectionAnchor = -1;

    private int scrollLine = 0;
    private final int lineHeight;
    private final int padding = 4;

    public MultiLineEditor(Font font, int x, int y, int width, int height, Component message) {
        super(x, y, width, height, message);
        this.font = font;
        this.lineHeight = font.lineHeight + 2;
    }

    // =========================================================
    // テキスト操作
    // =========================================================

    public String getValue() {
        return text.toString();
    }

    public void setValue(String value) {
        text.setLength(0);
        text.append(value == null ? "" : value);
        cursor = text.length();
        selectionAnchor = -1;
        scrollLine = 0;
    }

    private void insert(String s) {
        if (hasSelection()) {
            deleteSelection();
        }
        text.insert(cursor, s);
        cursor += s.length();
    }

    private void backspace() {
        if (hasSelection()) {
            deleteSelection();
            return;
        }
        if (cursor <= 0) return;
        text.deleteCharAt(cursor - 1);
        cursor--;
    }

    private void delete() {
        if (hasSelection()) {
            deleteSelection();
            return;
        }
        if (cursor >= text.length()) return;
        text.deleteCharAt(cursor);
    }

    private void deleteSelection() {
        if (!hasSelection()) return;
        int start = Math.min(cursor, selectionAnchor);
        int end = Math.max(cursor, selectionAnchor);
        text.delete(start, end);
        cursor = start;
        selectionAnchor = -1;
    }

    private boolean hasSelection() {
        return selectionAnchor >= 0 && selectionAnchor != cursor;
    }

    private void selectAll() {
        selectionAnchor = 0;
        cursor = text.length();
    }

    private String getSelectedText() {
        if (!hasSelection()) return "";
        int start = Math.min(cursor, selectionAnchor);
        int end = Math.max(cursor, selectionAnchor);
        return text.substring(start, end);
    }

    // =========================================================
    // 論理行・表示行
    // =========================================================

    private int lineStart(int pos) {
        int i = pos;
        while (i > 0 && text.charAt(i - 1) != '\n') i--;
        return i;
    }

    private int lineEnd(int pos) {
        int i = pos;
        while (i < text.length() && text.charAt(i) != '\n') i++;
        return i;
    }

    private int currentLine(int pos) {
        int line = 0;
        for (int i = 0; i < pos && i < text.length(); i++) {
            if (text.charAt(i) == '\n') line++;
        }
        return line;
    }

    /**
     * 論理位置 → (表示行, 表示列)。
     */
    private int[] toDisplay(int pos) {
        int displayLine = 0;
        int col = 0;
        int maxWidth = width - padding * 2;

        for (int i = 0; i < pos && i < text.length(); i++) {
            char c = text.charAt(i);

            if (c == '\n') {
                displayLine++;
                col = 0;
                continue;
            }

            String ch = String.valueOf(c);
            int charWidth = font.width(ch);

            // 折り返し判定
            if (col + charWidth > maxWidth) {
                displayLine++;
                col = 0;
            }
            col += charWidth;
        }
        return new int[]{displayLine, col};
    }

    /**
     * 論理位置をカーソル移動に使う（上下左右）。
     */
    private void moveUp() {
        // 表示行で1行上へ
        int[] disp = toDisplay(cursor);
        int targetDisplayLine = disp[0] - 1;
        if (targetDisplayLine < 0) return;
        cursor = findPosAtDisplay(targetDisplayLine, disp[1]);
    }

    private void moveDown() {
        int[] disp = toDisplay(cursor);
        int targetDisplayLine = disp[0] + 1;
        cursor = findPosAtDisplay(targetDisplayLine, disp[1]);
    }

    /**
     * 指定した表示行・表示列に最も近い論理位置を探す。
     */
    private int findPosAtDisplay(int targetLine, int targetCol) {
        int displayLine = 0;
        int col = 0;
        int maxWidth = width - padding * 2;
        int bestPos = 0;
        int bestDiff = Integer.MAX_VALUE;

        for (int i = 0; i <= text.length(); i++) {
            if (displayLine == targetLine) {
                int diff = Math.abs(col - targetCol);
                if (diff < bestDiff) {
                    bestDiff = diff;
                    bestPos = i;
                }
            }

            if (i == text.length()) break;

            char c = text.charAt(i);
            if (c == '\n') {
                displayLine++;
                col = 0;
            } else {
                int charWidth = font.width(String.valueOf(c));
                if (col + charWidth > maxWidth) {
                    displayLine++;
                    col = 0;
                }
                col += charWidth;
            }
        }
        return bestPos;
    }

    // =========================================================
    // 入力
    // =========================================================

    @Override
    public boolean charTyped(char c, int modifiers) {
        if (!this.isFocused()) return false;
        if (c == '\n' || c == '\r' || c < 32) return false;

        insert(String.valueOf(c));
        return true;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (!this.isFocused()) return false;

        boolean ctrl = (modifiers & GLFW.GLFW_MOD_CONTROL) != 0;
        boolean shift = (modifiers & GLFW.GLFW_MOD_SHIFT) != 0;

        // 選択開始（Shiftが押された瞬間、選択がなければカーソル位置をアンカーに）
        if (shift && selectionAnchor < 0) {
            selectionAnchor = cursor;
        }

        // Ctrl+A
        if (ctrl && keyCode == GLFW.GLFW_KEY_A) {
            selectAll();
            return true;
        }

        // Ctrl+C
        if (ctrl && keyCode == GLFW.GLFW_KEY_C) {
            String selected = getSelectedText();
            if (!selected.isEmpty()) {
                Minecraft.getInstance().keyboardHandler.setClipboard(selected);
            }
            return true;
        }

        // Ctrl+V
        if (ctrl && keyCode == GLFW.GLFW_KEY_V) {
            String clip = Minecraft.getInstance().keyboardHandler.getClipboard();
            if (clip != null && !clip.isEmpty()) {
                insert(clip);
            }
            return true;
        }

        // Ctrl+X
        if (ctrl && keyCode == GLFW.GLFW_KEY_X) {
            String selected = getSelectedText();
            if (!selected.isEmpty()) {
                Minecraft.getInstance().keyboardHandler.setClipboard(selected);
                deleteSelection();
            }
            return true;
        }

        switch (keyCode) {
            case GLFW.GLFW_KEY_BACKSPACE -> {
                backspace();
                return true;
            }
            case GLFW.GLFW_KEY_DELETE -> {
                delete();
                return true;
            }
            case GLFW.GLFW_KEY_ENTER, GLFW.GLFW_KEY_KP_ENTER -> {
                insert("\n");
                return true;
            }
            case GLFW.GLFW_KEY_LEFT -> {
                if (cursor > 0) {
                    cursor--;
                    if (!shift) selectionAnchor = -1;
                }
                return true;
            }
            case GLFW.GLFW_KEY_RIGHT -> {
                if (cursor < text.length()) {
                    cursor++;
                    if (!shift) selectionAnchor = -1;
                }
                return true;
            }
            case GLFW.GLFW_KEY_UP -> {
                moveUp();
                if (!shift) selectionAnchor = -1;
                return true;
            }
            case GLFW.GLFW_KEY_DOWN -> {
                moveDown();
                if (!shift) selectionAnchor = -1;
                return true;
            }
            case GLFW.GLFW_KEY_HOME -> {
                cursor = lineStart(cursor);
                if (!shift) selectionAnchor = -1;
                return true;
            }
            case GLFW.GLFW_KEY_END -> {
                cursor = lineEnd(cursor);
                if (!shift) selectionAnchor = -1;
                return true;
            }
        }

        // Shiftを離したら選択解除（キーイベントでは検知しにくいので、ここでは保留）
        return false;
    }

    @Override
    public boolean keyReleased(int keyCode, int scanCode, int modifiers) {
        if (keyCode == GLFW.GLFW_KEY_LEFT_SHIFT || keyCode == GLFW.GLFW_KEY_RIGHT_SHIFT) {
            // 選択を確定（アンカーはリセットしない）
            return true;
        }
        return false;
    }

    // =========================================================
    // 描画
    // =========================================================

    @Override
    protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {

        // 背景
        int bg = this.isFocused() ? 0xFFFFF8E8 : 0xFFE8DCC0;
        int border = this.isFocused() ? 0xFF8B6F3E : 0xFF7A6248;

        graphics.fill(getX() - 1, getY() - 1, getX() + width + 1, getY() + height + 1, border);
        graphics.fill(getX(), getY(), getX() + width, getY() + height, bg);

        int maxWidth = width - padding * 2;
        int maxVisibleLines = (height - padding * 2) / lineHeight;

        // =========================================================
        // 論理テキスト → 表示行リストに変換
        // =========================================================
        List<DisplayLine> lines = buildDisplayLines(maxWidth);

        // カーソルの表示位置
        int[] cursorDisp = toDisplay(cursor);
        int cursorLine = cursorDisp[0];

        // スクロール追従
        if (cursorLine < scrollLine) {
            scrollLine = cursorLine;
        } else if (cursorLine >= scrollLine + maxVisibleLines) {
            scrollLine = cursorLine - maxVisibleLines + 1;
        }

        // =========================================================
        // 描画
        // =========================================================
        int y = getY() + padding;
        for (int i = scrollLine; i < Math.min(lines.size(), scrollLine + maxVisibleLines); i++) {
            DisplayLine dl = lines.get(i);

            // カーソル行ハイライト
            if (i == cursorLine && this.isFocused()) {
                graphics.fill(
                        getX() + padding,
                        y - 1,
                        getX() + width - padding,
                        y + lineHeight - 1,
                        0x33FFAA00
                );
            }

            // 選択ハイライト
            if (hasSelection()) {
                int selStart = Math.min(cursor, selectionAnchor);
                int selEnd = Math.max(cursor, selectionAnchor);

                if (dl.startPos < selEnd && dl.endPos > selStart) {
                    int selLocalStart = Math.max(selStart, dl.startPos) - dl.startPos;
                    int selLocalEnd = Math.min(selEnd, dl.endPos) - dl.startPos;

                    int x1 = getX() + padding + font.width(dl.text.substring(0, selLocalStart));
                    int x2 = getX() + padding + font.width(dl.text.substring(0, selLocalEnd));

                    graphics.fill(x1, y - 1, x2, y + lineHeight - 1, 0x5566AAFF);
                }
            }

            graphics.drawString(font, dl.text, getX() + padding, y, 0xFF4A3520, false);
            y += lineHeight;
        }

        // カーソル点滅
        if (this.isFocused() && (System.currentTimeMillis() / 500) % 2 == 0) {
            int cx = getX() + padding + cursorDisp[1];
            int cy = getY() + padding + (cursorLine - scrollLine) * lineHeight;
            graphics.fill(cx, cy, cx + 1, cy + font.lineHeight, 0xFF4A3520);
        }

        // 行番号
        if (this.isFocused()) {
            String info = (cursorLine + 1) + " / " + lines.size();
            int w = font.width(info);
            graphics.drawString(
                    font, info,
                    getX() + width - w - padding,
                    getY() + height - font.lineHeight - padding,
                    0xFF7A6248, false
            );
        }
    }

    /**
     * 論理テキストを折り返し付きの表示行リストに変換する。
     */
    private List<DisplayLine> buildDisplayLines(int maxWidth) {
        List<DisplayLine> result = new ArrayList<>();

        int lineStart = 0;
        int i = 0;

        while (i <= text.length()) {
            // 論理行の終わりを見つける
            int logicalEnd = i;
            while (logicalEnd < text.length() && text.charAt(logicalEnd) != '\n') {
                logicalEnd++;
            }

            // 論理行を折り返す
            int segmentStart = lineStart;
            int col = 0;
            int pos = lineStart;

            while (pos <= logicalEnd) {
                if (pos == logicalEnd) {
                    // 行末まで来た
                    result.add(new DisplayLine(
                            text.substring(segmentStart, pos),
                            segmentStart, pos
                    ));
                    break;
                }

                String ch = String.valueOf(text.charAt(pos));
                int w = font.width(ch);

                if (col + w > maxWidth) {
                    // 折り返し
                    result.add(new DisplayLine(
                            text.substring(segmentStart, pos),
                            segmentStart, pos
                    ));
                    segmentStart = pos;
                    col = 0;
                }
                col += w;
                pos++;
            }

            // 次の論理行へ
            i = logicalEnd + 1;
            lineStart = i;
            if (i > text.length()) break;
        }

        if (result.isEmpty()) {
            result.add(new DisplayLine("", 0, 0));
        }
        return result;
    }

    private record DisplayLine(String text, int startPos, int endPos) {
    }

    // =========================================================
    // AbstractWidget の必須メソッド
    // =========================================================

    @Override
    protected void updateWidgetNarration(NarrationElementOutput output) {
        output.add(NarratedElementType.TITLE, this.getMessage());
    }
}