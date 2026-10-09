package com.tyami.forlaism.client;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

/**
 * マスターピースクロックのバインド画面。
 *
 * 現状は「開くだけ」。今後ここに機能を追加する。
 *
 * 拡張方法:
 *   - ボタンを追加したい → addRenderableWidget() を init() 内で
 *   - サーバー処理が必要 → パケットを新規作成して送信
 *   - 表示要素を増やす → render() 内で描画
 */
@OnlyIn(Dist.CLIENT)
public class MasterpieceClockBindScreen extends Screen {

    private static final int WIDTH = 240;
    private static final int HEIGHT = 180;

    public MasterpieceClockBindScreen() {
        super(Component.literal("Masterpiece Clock"));
    }

    @Override
    protected void init() {
        super.init();
        // ここにボタンなどを追加予定
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(graphics);

        int x = (this.width - WIDTH) / 2;
        int y = (this.height - HEIGHT) / 2;

        // 背景パネル（銀色）
        graphics.fill(x - 2, y - 2, x + WIDTH + 2, y + HEIGHT + 2, 0xFF888888);
        graphics.fill(x, y, x + WIDTH, y + HEIGHT, 0xFF101018);
        graphics.fillGradient(x + 2, y + 2, x + WIDTH - 2, y + HEIGHT - 2,
                0xFF202030, 0xFF101018);

        // タイトル
        graphics.drawCenteredString(
                this.font,
                "§f§lMasterpiece Clock",
                this.width / 2,
                y + 12,
                0xFFFFFFFF
        );

        // 中央メッセージ
        graphics.drawCenteredString(
                this.font,
                "§7─ 刻は、汝の掌中に ─",
                this.width / 2,
                y + HEIGHT / 2 - 10,
                0xFF888888
        );

        graphics.drawCenteredString(
                this.font,
                "§8(機能は後日追加予定)",
                this.width / 2,
                y + HEIGHT / 2 + 10,
                0xFF555555
        );

        super.render(graphics, mouseX, mouseY, partialTick);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}