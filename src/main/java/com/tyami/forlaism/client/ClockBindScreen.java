package com.tyami.forlaism.client;

import com.tyami.forlaism.network.*;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import java.util.ArrayList;
import java.util.List;

@OnlyIn(Dist.CLIENT)
public class ClockBindScreen extends Screen {

    private static final int BOOK_W = 320;
    private static final int BOOK_H = 220;
    private static final int PAGE_MARGIN = 8;
    private static final int SPINE_W = 10;
    private static final int MAX_PAGES = 4;

    private static final int PARCHMENT         = 0xFF2A0E3A;
    private static final int PARCHMENT_EDGE    = 0xFF3D1A50;
    private static final int PARCHMENT_OUTLINE = 0xFF8A4DC8;
    private static final int INK               = 0xFFE8D0FF;
    private static final int INK_FAINT         = 0xFF9A7AA8;
    private static final int COVER_DARK        = 0xFF0A0510;
    private static final int COVER_LIGHT       = 0xFF1F0A2A;
    private static final int RIBBON            = 0xFFFFD700;
    private static final int CLOCK_GOLD        = 0xFFFFD700;
    private static final int CLOCK_HAND        = 0xFFFFFFFF;

    private int currentPage = 0;
    private int selectedEntityIndex = -1;
    private final List<Button> currentButtons = new ArrayList<>();
    private final List<EditBox> currentFields = new ArrayList<>();
    private long screenOpenNanos;

    // プレビュー
    private int previewWeather = 0;
    private int previewTime = 0;
    private int weatherBtnX, weatherBtnY, weatherBtnW, weatherBtnH;
    private int timeBtnX, timeBtnY, timeBtnW, timeBtnH;

    // TP座標入力
    private EditBox tpXField, tpYField, tpZField;

    // locate サブメニュー状態
    private int locateMode = 0;  // 0=構造物, 1=バイオーム
    private int locateScroll = 0;

    // onStateChanged の無限ループ防止用フラグ
    private boolean rebuilding = false;

    public ClockBindScreen() {
        super(Component.literal("Masterpiece Clock"));
    }

    @Override
    protected void init() {
        super.init();
        screenOpenNanos = System.nanoTime();

        previewWeather = switch (ClockStateCache.weatherLabel) {
            case "雨" -> 1;
            case "雷雨" -> 2;
            default -> 0;
        };
        previewTime = switch (ClockStateCache.timeLabel) {
            case "昼" -> 1;
            case "夕方" -> 2;
            case "夜" -> 3;
            default -> 0;
        };

        // ★ 修正: ダミーTOGGLE送信をやめて、状態要求のみ送る
        FactotumPacketHandler.CHANNEL.sendToServer(
                new ClockExtraActionPacket(ClockExtraActionPacket.Action.REQUEST_STATE)
        );

        rebuildButtons();
    }

    public void onStateChanged() {
        // 自分でrebuildしている最中は再入しない
        if (rebuilding) return;
        rebuildButtons();
    }

    private void rebuildButtons() {
        rebuilding = true;
        try {
            for (Button b : currentButtons) this.removeWidget(b);
            for (EditBox f : currentFields) this.removeWidget(f);
            currentButtons.clear();
            currentFields.clear();
            tpXField = null; tpYField = null; tpZField = null;

            int x = (this.width - BOOK_W) / 2;
            int y = (this.height - BOOK_H) / 2;
            int pageWidth = (BOOK_W - SPINE_W - PAGE_MARGIN * 3) / 2;
            int rightPageX = x + PAGE_MARGIN + pageWidth + PAGE_MARGIN + SPINE_W;
            int rightPageY = y + PAGE_MARGIN;

            switch (currentPage) {
                case 0 -> buildPage0(rightPageX, rightPageY, pageWidth);
                case 1 -> buildPage1(rightPageX, rightPageY, pageWidth);
                case 2 -> buildPage2(rightPageX, rightPageY, pageWidth);
                case 3 -> buildPage3(rightPageX, rightPageY, pageWidth);
            }
        } finally {
            rebuilding = false;
        }
    }

    // =========================================================
    // ページ0: 天候/時間/時間停止/重力
    // =========================================================

    private static final String[] WEATHER_LABELS = {"晴れ", "雨", "雷雨"};
    private static final String[] TIME_LABELS = {"朝", "昼", "夕方", "夜"};

    private void buildPage0(int baseX, int baseY, int pageWidth) {
        int cx = baseX + pageWidth / 2;
        int startY = baseY + 28;
        int gap = 28;

        weatherBtnX = cx - 70; weatherBtnY = startY; weatherBtnW = 140; weatherBtnH = 20;
        addButton(weatherBtnX, weatherBtnY, weatherBtnW, weatherBtnH,
                "§b天候: §f" + WEATHER_LABELS[previewWeather] + " §7(スクロール/クリック)",
                () -> sendAction(new ClockActionPacket(ClockActionPacket.Action.SET_WEATHER, previewWeather)));

        timeBtnX = cx - 70; timeBtnY = startY + gap; timeBtnW = 140; timeBtnH = 20;
        addButton(timeBtnX, timeBtnY, timeBtnW, timeBtnH,
                "§e時間: §f" + TIME_LABELS[previewTime] + " §7(スクロール/クリック)",
                () -> sendAction(new ClockActionPacket(ClockActionPacket.Action.SET_TIME, previewTime)));

        addButton(cx - 70, startY + gap * 2, 140, 20,
                ClockStateCache.timeFrozen ? "§c§l時間停止中 §7(解除)" : "§b時間停止 §7(発動)",
                () -> sendAction(new ClockActionPacket(ClockActionPacket.Action.TOGGLE_TIME_FREEZE)));

        addButton(cx - 70, startY + gap * 3, 140, 20,
                ClockStateCache.gravityInverted ? "§5§l重力反転中 §7(解除)" : "§5重力反転 §7(発動)",
                () -> sendAction(new ClockActionPacket(ClockActionPacket.Action.TOGGLE_GRAVITY)));
    }

    // =========================================================
    // ページ1: オートトーテム/オートエイム/TP/tick
    // =========================================================

    private void buildPage1(int baseX, int baseY, int pageWidth) {
        int cx = baseX + pageWidth / 2;
        int startY = baseY + 20;
        int gap = 26;

        addButton(cx - 80, startY, 160, 18,
                ClockStateCache.autoTotem ? "§dオートトーテム: §aON" : "§dオートトーテム: §cOFF",
                () -> sendExtra(ClockExtraActionPacket.Action.TOGGLE_AUTO_TOTEM));

        addButton(cx - 80, startY + gap, 160, 18,
                ClockStateCache.autoAim ? "§aオートエイム: §aON §7(弓を引くと補正)" : "§aオートエイム: §cOFF",
                () -> sendExtra(ClockExtraActionPacket.Action.TOGGLE_AUTO_AIM));

        // 視線先TP
        addButton(cx - 80, startY + gap * 2, 160, 18,
                "§b視線先TP", () -> sendExtra(ClockExtraActionPacket.Action.TELEPORT_LOOKED_AT));

        // 座標TP
        tpXField = new EditBox(this.font, cx - 80, startY + gap * 3, 50, 16, Component.literal("X"));
        tpXField.setValue(String.valueOf((int) net.minecraft.client.Minecraft.getInstance().player.getX()));
        this.addRenderableWidget(tpXField); currentFields.add(tpXField);

        tpYField = new EditBox(this.font, cx - 25, startY + gap * 3, 50, 16, Component.literal("Y"));
        tpYField.setValue(String.valueOf((int) net.minecraft.client.Minecraft.getInstance().player.getY()));
        this.addRenderableWidget(tpYField); currentFields.add(tpYField);

        tpZField = new EditBox(this.font, cx + 30, startY + gap * 3, 50, 16, Component.literal("Z"));
        tpZField.setValue(String.valueOf((int) net.minecraft.client.Minecraft.getInstance().player.getZ()));
        this.addRenderableWidget(tpZField); currentFields.add(tpZField);

        addButton(cx - 40, startY + gap * 4, 80, 16,
                "§b座標TP実行", () -> {
                    try {
                        double x = Double.parseDouble(tpXField.getValue());
                        double y = Double.parseDouble(tpYField.getValue());
                        double z = Double.parseDouble(tpZField.getValue());
                        FactotumPacketHandler.CHANNEL.sendToServer(
                                new ClockExtraActionPacket(
                                        ClockExtraActionPacket.Action.TELEPORT_TO_COORD, x, y, z, ""));
                    } catch (NumberFormatException ignored) {}
                });

        // tickサブメニュー
        addButton(cx - 80, startY + gap * 5, 160, 18,
                "§6tick操作 ▸", () -> { /* ページ1下部に統合 */ });
    }

    // =========================================================
    // ページ2: Entity保存管理
    // =========================================================

    private void buildPage2(int baseX, int baseY, int pageWidth) {
        int cx = baseX + pageWidth / 2;

        addButton(cx - 70, baseY + 10, 140, 20,
                "§d視線先のEntityを保存",
                () -> sendSave(ClockSaveEntityPacket.Type.SAVE_LOOKED_AT, null));

        var list = ClockStateCache.entityList;
        int listStartY = baseY + 40;
        int rowH = 18;

        for (int i = 0; i < Math.min(list.size(), 6); i++) {
            var e = list.get(i);
            final int finalI = i;
            String label = (i == selectedEntityIndex ? "§a▶ " : "§7・ ") + "§f" + e.displayName;
            addButton(baseX + 10, listStartY + i * rowH, pageWidth - 20, 16, label,
                    () -> { selectedEntityIndex = finalI; rebuildButtons(); });
        }

        if (selectedEntityIndex >= 0 && selectedEntityIndex < list.size()) {
            var e = list.get(selectedEntityIndex);
            addButton(cx - 70, baseY + 160, 68, 20, "§a復活",
                    () -> { sendSave(ClockSaveEntityPacket.Type.RESTORE, e.id); selectedEntityIndex = -1; });
            addButton(cx + 2, baseY + 160, 68, 20, "§c削除",
                    () -> { sendSave(ClockSaveEntityPacket.Type.DELETE, e.id); selectedEntityIndex = -1; });
        }

        addButton(cx - 70, baseY + 184, 140, 16, "§4全削除",
                () -> { sendSave(ClockSaveEntityPacket.Type.CLEAR_ALL, null); selectedEntityIndex = -1; });
    }

    // =========================================================
    // ページ3: locate
    // =========================================================

    private static final String[] STRUCTURE_IDS = {
            "minecraft:village_plains",
            "minecraft:village_desert",
            "minecraft:village_savanna",
            "minecraft:village_snowy",
            "minecraft:village_taiga",
            "minecraft:stronghold",
            "minecraft:ancient_city",
            "minecraft:woodland_mansion",
            "minecraft:monument",
            "minecraft:mansion",
            "minecraft:pillager_outpost",
            "minecraft:bastion_remnant",
            "minecraft:fortress",
            "minecraft:end_city",
            "minecraft:nether_fossil",
            "minecraft:ocean_ruin_cold",
            "minecraft:ocean_ruin_warm",
            "minecraft:shipwreck",
            "minecraft:buried_treasure",
            "minecraft:igloo",
            "minecraft:mineshaft",
            "minecraft:ruined_portal"
    };

    private static final String[] BIOME_IDS = {
            "minecraft:plains",
            "minecraft:desert",
            "minecraft:forest",
            "minecraft:taiga",
            "minecraft:snowy_plains",
            "minecraft:snowy_taiga",
            "minecraft:jungle",
            "minecraft:swamp",
            "minecraft:badlands",
            "minecraft:savanna",
            "minecraft:cherry_grove",
            "minecraft:meadow",
            "minecraft:grove",
            "minecraft:mushroom_fields",
            "minecraft:ocean",
            "minecraft:deep_ocean",
            "minecraft:warm_ocean",
            "minecraft:frozen_ocean",
            "minecraft:nether_wastes",
            "minecraft:soul_sand_valley",
            "minecraft:crimson_forest",
            "minecraft:warped_forest",
            "minecraft:basalt_deltas",
            "minecraft:the_end",
            "minecraft:end_highlands",
            "minecraft:end_midlands"
    };

    private void buildPage3(int baseX, int baseY, int pageWidth) {
        int cx = baseX + pageWidth / 2;

        // モード切替
        addButton(baseX + 5, baseY + 5, (pageWidth - 15) / 2, 16,
                (locateMode == 0 ? "§a▶ " : "§7") + "構造物", () -> { locateMode = 0; locateScroll = 0; rebuildButtons(); });
        addButton(baseX + 10 + (pageWidth - 15) / 2, baseY + 5, (pageWidth - 15) / 2, 16,
                (locateMode == 1 ? "§a▶ " : "§7") + "バイオーム", () -> { locateMode = 1; locateScroll = 0; rebuildButtons(); });

        String[] ids = locateMode == 0 ? STRUCTURE_IDS : BIOME_IDS;
        int listStartY = baseY + 28;
        int rowH = 16;
        int rows = 7;

        for (int i = 0; i < rows && (locateScroll + i) < ids.length; i++) {
            int idx = locateScroll + i;
            String id = ids[idx];
            String shortName = id.substring(id.indexOf(':') + 1);
            if (shortName.length() > 20) shortName = shortName.substring(0, 20) + "...";
            addButton(baseX + 5, listStartY + i * rowH, pageWidth - 10, 14,
                    "§7" + shortName, () -> {
                        var action = locateMode == 0
                                ? ClockExtraActionPacket.Action.LOCATE_STRUCTURE
                                : ClockExtraActionPacket.Action.LOCATE_BIOME;
                        FactotumPacketHandler.CHANNEL.sendToServer(
                                new ClockExtraActionPacket(action, 0, 0, 0, id));
                    });
        }

        // スクロールボタン
        if (locateScroll > 0) {
            addButton(baseX + 5, baseY + 155, 60, 14, "▲ 上", () -> { locateScroll -= rows; if (locateScroll < 0) locateScroll = 0; rebuildButtons(); });
        }
        if (locateScroll + rows < ids.length) {
            addButton(cx - 30, baseY + 155, 60, 14, "▼ 下", () -> { locateScroll += rows; rebuildButtons(); });
        }
    }

    // =========================================================
    // ヘルパ
    // =========================================================

    private void addButton(int x, int y, int w, int h, String label, Runnable onClick) {
        Button b = Button.builder(Component.literal(label), btn -> onClick.run())
                .bounds(x, y, w, h).build();
        this.addRenderableWidget(b);
        currentButtons.add(b);
    }

    private void sendAction(ClockActionPacket packet) {
        FactotumPacketHandler.CHANNEL.sendToServer(packet);
    }

    private void sendExtra(ClockExtraActionPacket.Action action) {
        FactotumPacketHandler.CHANNEL.sendToServer(new ClockExtraActionPacket(action));
    }

    private void sendSave(ClockSaveEntityPacket.Type type, java.util.UUID id) {
        FactotumPacketHandler.CHANNEL.sendToServer(new ClockSaveEntityPacket(type, id));
    }

    // =========================================================
    // スクロール
    // =========================================================

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        if (currentPage == 0) {
            int dir = delta > 0 ? 1 : -1;
            if (isInside(mouseX, mouseY, weatherBtnX, weatherBtnY, weatherBtnW, weatherBtnH)) {
                previewWeather = (previewWeather + dir + WEATHER_LABELS.length) % WEATHER_LABELS.length;
                rebuildButtons();
                return true;
            }
            if (isInside(mouseX, mouseY, timeBtnX, timeBtnY, timeBtnW, timeBtnH)) {
                previewTime = (previewTime + dir + TIME_LABELS.length) % TIME_LABELS.length;
                rebuildButtons();
                return true;
            }
        }
        return super.mouseScrolled(mouseX, mouseY, delta);
    }

    private static boolean isInside(double mx, double my, int x, int y, int w, int h) {
        return mx >= x && mx <= x + w && my >= y && my <= y + h;
    }

    // =========================================================
    // render
    // =========================================================

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(graphics);

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

        drawClockFace(graphics, leftPageX + pageWidth / 2, leftPageY + pageHeight / 2 - 10);

        String header = switch (currentPage) {
            case 0 -> "基本操作";
            case 1 -> "戦闘補助";
            case 2 -> "保存Entity";
            case 3 -> "Locate";
            default -> "";
        };

        int headerWidth = this.font.width(header);
        graphics.drawString(this.font, header,
                leftPageX + pageWidth / 2 - headerWidth / 2, leftPageY + 8, CLOCK_GOLD, false);
        graphics.fill(leftPageX + 16, leftPageY + 22, leftPageX + pageWidth - 16, leftPageY + 23, INK_FAINT);

        String pageNum = (currentPage + 1) + " / " + MAX_PAGES;
        int pnWidth = this.font.width(pageNum);
        graphics.drawString(this.font, pageNum,
                rightPageX + pageWidth - pnWidth - 10, rightPageY + pageHeight - 14, INK_FAINT, false);

        if (currentPage > 0) {
            graphics.drawString(this.font, "◀右クリック", x - 42, y + BOOK_H / 2, 0xCCCCCC, false);
        }
        if (currentPage < MAX_PAGES - 1) {
            graphics.drawString(this.font, "左クリック▶", x + BOOK_W + 6, y + BOOK_H / 2, 0xCCCCCC, false);
        }

        super.render(graphics, mouseX, mouseY, partialTick);
    }

    private void drawClockFace(GuiGraphics graphics, int cx, int cy) {
        int radius = 44;
        for (int r = radius; r > 0; r--) {
            int color = (r == radius) ? CLOCK_GOLD : PARCHMENT_EDGE;
            drawCircle(graphics, cx, cy, r, color);
        }
        drawCircleRing(graphics, cx, cy, radius, CLOCK_GOLD);
        drawCircleRing(graphics, cx, cy, radius - 2, CLOCK_GOLD);
        for (int i = 0; i < 12; i++) {
            double angle = (i / 12.0) * Math.PI * 2 - Math.PI / 2;
            int r1 = radius - 6;
            int r2 = (i % 3 == 0) ? radius - 14 : radius - 10;
            int x1 = cx + (int) (Math.cos(angle) * r1);
            int y1 = cy + (int) (Math.sin(angle) * r1);
            int x2 = cx + (int) (Math.cos(angle) * r2);
            int y2 = cy + (int) (Math.sin(angle) * r2);
            drawLine(graphics, x1, y1, x2, y2, CLOCK_GOLD);
        }
        double elapsed = (System.nanoTime() - screenOpenNanos) / 1_000_000_000.0;
        double hourAngle = (elapsed * Math.PI * 2 / 60.0) - Math.PI / 2;
        int hx = cx + (int) (Math.cos(hourAngle) * (radius * 0.5));
        int hy = cy + (int) (Math.sin(hourAngle) * (radius * 0.5));
        drawLine(graphics, cx, cy, hx, hy, CLOCK_HAND);
        double minAngle = (elapsed * Math.PI * 2 / 4.0) - Math.PI / 2;
        int mx = cx + (int) (Math.cos(minAngle) * (radius * 0.75));
        int my = cy + (int) (Math.sin(minAngle) * (radius * 0.75));
        drawLine(graphics, cx, cy, mx, my, CLOCK_HAND);
        graphics.fill(cx - 3, cy - 3, cx + 3, cy + 3, CLOCK_GOLD);
    }

    private void drawCircle(GuiGraphics g, int cx, int cy, int r, int color) {
        for (int dy = -r; dy <= r; dy++) {
            int dx = (int) Math.sqrt(r * r - dy * dy);
            g.fill(cx - dx, cy + dy, cx + dx, cy + dy + 1, color);
        }
    }

    private void drawCircleRing(GuiGraphics g, int cx, int cy, int r, int color) {
        int prevX = cx + r, prevY = cy;
        for (int i = 1; i <= 64; i++) {
            double angle = (i / 64.0) * Math.PI * 2;
            int x = cx + (int) (Math.cos(angle) * r);
            int y = cy + (int) (Math.sin(angle) * r);
            drawLine(g, prevX, prevY, x, y, color);
            prevX = x; prevY = y;
        }
    }

    private void drawLine(GuiGraphics g, int x1, int y1, int x2, int y2, int color) {
        int dx = Math.abs(x2 - x1);
        int dy = Math.abs(y2 - y1);
        int steps = Math.max(dx, dy);
        if (steps == 0) { g.fill(x1, y1, x1 + 1, y1 + 1, color); return; }
        for (int i = 0; i <= steps; i++) {
            int x = x1 + (x2 - x1) * i / steps;
            int y = y1 + (y2 - y1) * i / steps;
            g.fill(x, y, x + 1, y + 1, color);
        }
    }

    private void drawBookFrame(GuiGraphics g, int x, int y, int w, int h) {
        g.fill(x - 3, y - 3, x + w + 3, y + h + 3, 0x66000000);
        g.fill(x, y, x + w, y + h, 0xFF3A2418);
        g.fillGradient(x + 2, y + 2, x + w - 2, y + h - 2, COVER_LIGHT, COVER_DARK);
    }

    private void drawParchmentPage(GuiGraphics g, int x, int y, int w, int h, boolean isRight) {
        g.fill(x - 1, y - 1, x + w + 1, y + h + 1, PARCHMENT_OUTLINE);
        g.fill(x, y, x + w, y + h, PARCHMENT);
        g.fillGradient(x, y, x + w, y + 12, PARCHMENT_EDGE, PARCHMENT);
        g.fillGradient(x, y + h - 12, x + w, y + h, PARCHMENT, PARCHMENT_EDGE);
        int shadowWidth = 5;
        if (isRight) {
            g.fillGradient(x, y, x + shadowWidth, y + h, 0x44000000, 0x00000000);
        } else {
            g.fillGradient(x + w - shadowWidth, y, x + w, y + h, 0x00000000, 0x44000000);
        }
        if (isRight) {
            g.fill(x + w / 2 - 1, y - 2, x + w / 2 + 1, y + 26, RIBBON);
            g.fill(x + w / 2 - 3, y + 24, x + w / 2 + 3, y + 28, RIBBON);
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (super.mouseClicked(mouseX, mouseY, button)) return true;
        int x = (this.width - BOOK_W) / 2;
        int y = (this.height - BOOK_H) / 2;
        if (mouseX < x || mouseX > x + BOOK_W || mouseY < y || mouseY > y + BOOK_H) return false;
        if (button == 0) {
            if (currentPage < MAX_PAGES - 1) { currentPage++; rebuildButtons(); return true; }
        } else if (button == 1) {
            if (currentPage > 0) { currentPage--; rebuildButtons(); return true; }
        }
        return false;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}