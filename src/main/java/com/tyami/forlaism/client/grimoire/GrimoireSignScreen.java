package com.tyami.forlaism.client.grimoire;

import com.tyami.forlaism.item.GrimoireItem;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import java.util.List;

@OnlyIn(Dist.CLIENT)
public class GrimoireSignScreen extends Screen {

    private final ItemStack stack;
    private final List<String> pages;

    private EditBox titleField;
    private EditBox authorField;

    private static final int WIDTH = 240;
    private static final int HEIGHT = 140;

    public GrimoireSignScreen(ItemStack stack, List<String> pages) {
        super(Component.literal("Sign Grimoire"));
        this.stack = stack;
        this.pages = pages;
    }

    @Override
    protected void init() {
        super.init();

        int x = (this.width - WIDTH) / 2;
        int y = (this.height - HEIGHT) / 2;

        this.titleField = new EditBox(
                this.font,
                x + 20, y + 40, WIDTH - 40, 20,
                Component.literal("Title")
        );
        this.titleField.setMaxLength(32);
        this.titleField.setValue(GrimoireItem.getTitle(stack).equals("Untitled")
                ? "" : GrimoireItem.getTitle(stack));
        this.addRenderableWidget(titleField);

        this.authorField = new EditBox(
                this.font,
                x + 20, y + 80, WIDTH - 40, 20,
                Component.literal("Author")
        );
        this.authorField.setMaxLength(16);
        this.authorField.setValue(
                Minecraft.getInstance().player != null
                        ? Minecraft.getInstance().player.getGameProfile().getName()
                        : "Unknown"
        );
        this.addRenderableWidget(authorField);

        addRenderableWidget(Button.builder(
                Component.literal("署名する"),
                b -> sign()
        ).bounds(x + 20, y + HEIGHT - 30, 100, 20).build());

        addRenderableWidget(Button.builder(
                Component.literal("戻る"),
                b -> this.onClose()
        ).bounds(x + WIDTH - 120, y + HEIGHT - 30, 100, 20).build());
    }

    private void sign() {
        String title = titleField.getValue().trim();
        String author = authorField.getValue().trim();

        if (title.isEmpty()) {
            if (Minecraft.getInstance().player != null) {
                Minecraft.getInstance().player.displayClientMessage(
                        Component.literal("§cタイトルを入力してください"),
                        true
                );
            }
            return;
        }

        CompoundTag tag = stack.getOrCreateTag();

        ListTag pageList = new ListTag();
        for (String page : pages) {
            pageList.add(StringTag.valueOf(page));
        }
        tag.put(GrimoireItem.TAG_PAGES, pageList);
        tag.putString(GrimoireItem.TAG_TITLE, title);
        tag.putString(GrimoireItem.TAG_AUTHOR, author);
        tag.putBoolean(GrimoireItem.TAG_SIGNED, true);

        // =========================================================
        // クライアント側の所持アイテムを即座に同期
        // =========================================================
        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null) {
            mc.player.displayClientMessage(
            Component.literal("§d§l署名完了！ §7アイテムにsiftを押しながらカーソルを当てると待機0で発動準備が完了します"),
            false
    );
            var inv = mc.player.getInventory();

            // メインハンド（inv.selected は int フィールド）
            int sel = inv.selected;
            if (sel >= 0 && sel < inv.items.size()) {
                if (inv.items.get(sel) == stack) {
                    inv.items.set(sel, stack.copy());
                }
            }

            // オフハンド
            if (mc.player.getOffhandItem() == stack) {
                inv.offhand.set(0, stack.copy());
            }

            // インベントリ全体
            for (int i = 0; i < inv.items.size(); i++) {
                if (inv.items.get(i) == stack) {
                    inv.items.set(i, stack.copy());
                }
            }

            // Curiosスロットも念のため
            try {
                top.theillusivec4.curios.api.CuriosApi
                        .getCuriosInventory(mc.player)
                        .ifPresent(handler -> {
                            handler.getCurios().forEach((slotType, slotHandler) -> {
                                var stacks = slotHandler.getStacks();
                                for (int i = 0; i < stacks.getSlots(); i++) {
                                    if (stacks.getStackInSlot(i) == stack) {
                                        stacks.setStackInSlot(i, stack.copy());
                                    }
                                }
                            });
                        });
            } catch (Throwable ignored) {}
        }

        this.onClose();
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics);

        int x = (this.width - WIDTH) / 2;
        int y = (this.height - HEIGHT) / 2;

        graphics.fill(x - 3, y - 3, x + WIDTH + 3, y + HEIGHT + 3, 0x66000000);
        graphics.fill(x, y, x + WIDTH, y + HEIGHT, 0xFF2E1B10);
        graphics.fillGradient(x + 2, y + 2, x + WIDTH - 2, y + HEIGHT - 2,
                0xFFEFE0BC, 0xFFC9B58A);

        graphics.drawCenteredString(
                this.font,
                "§d§l魔道書に署名する",
                this.width / 2,
                y + 12,
                0xFFFFFFFF
        );

        graphics.drawString(this.font, "タイトル:", x + 20, y + 28, 0xFF4A3520, false);
        graphics.drawString(this.font, "著者:", x + 20, y + 68, 0xFF4A3520, false);

        graphics.drawCenteredString(
                this.font,
                "§c署名すると本文は書き換えられません",
                this.width / 2,
                y + HEIGHT - 50,
                0xFFFF5555
        );

        super.render(graphics, mouseX, mouseY, partialTick);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}