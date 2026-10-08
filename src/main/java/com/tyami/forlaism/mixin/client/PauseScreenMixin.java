package com.tyami.forlaism.mixin.client;

import com.tyami.forlaism.registry.Items;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.PauseScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.lang.reflect.Method;
import java.util.List;

/**
 * ポーズ画面（ESC）に「Save」ボタンを追加する。
 *
 * ウツワを所持している時のみ表示される。
 * 押すと、インベントリの「ウツワ」が「決意」に変わる。
 */
@Mixin(PauseScreen.class)
public abstract class PauseScreenMixin {

    private static Method ADD_WIDGET_METHOD;
    private static Method CHILDREN_METHOD;

    static {
        for (Method m : Screen.class.getDeclaredMethods()) {
            if (m.getName().equals("addRenderableWidget")
                    || m.getName().equals("m_142416_")) {
                m.setAccessible(true);
                ADD_WIDGET_METHOD = m;
                break;
            }
        }
        for (Method m : Screen.class.getDeclaredMethods()) {
            if (m.getName().equals("children")
                    || m.getName().equals("m_6702_")) {
                m.setAccessible(true);
                CHILDREN_METHOD = m;
                break;
            }
        }

        if (ADD_WIDGET_METHOD == null) {
            System.err.println("[Forlaism] addRenderableWidget not found!");
        }
        if (CHILDREN_METHOD == null) {
            System.err.println("[Forlaism] children not found!");
        }
    }

    @Inject(
            method = "init",
            at = @At("RETURN")
    )
    private void forlaism$addSaveButton(CallbackInfo ci) {

        PauseScreen self = (PauseScreen) (Object) this;

        if (ADD_WIDGET_METHOD == null || CHILDREN_METHOD == null) return;

        // ウツワを持ってる時だけボタンを出す
        if (!forlaism$hasUtsuwa()) return;

        try {
            List<?> children = (List<?>) CHILDREN_METHOD.invoke(self);

            Button quitButton = null;

            for (Object child : children) {

                if (!(child instanceof Button button)) continue;

                String text = button.getMessage().getString();

                boolean isQuitButton =
                        text.equals("セーブしてタイトルへ戻る")
                        || text.equals("Save and Quit to Title")
                        || text.equals("Save and Quit");

                if (isQuitButton) {
                    quitButton = button;
                    break;
                }
            }

            if (quitButton == null) return;

            int newX = quitButton.getX();
            int newY = quitButton.getY() + quitButton.getHeight() + 4;

            if (newY + quitButton.getHeight() > self.height - 4) {
                newY = quitButton.getY() - quitButton.getHeight() - 4;
            }

            // 個数表示付きラベル
            int count = forlaism$countUtsuwa();
            String label = "§dSave §7(ウツワ ×" + count + ")";

            Button saveButton = Button.builder(
                    Component.literal(label),
                    b -> forlaism$onSavePressed()
            ).bounds(newX, newY, quitButton.getWidth(), quitButton.getHeight()).build();

            ADD_WIDGET_METHOD.invoke(self, saveButton);

        } catch (Throwable t) {
            System.err.println("[Forlaism] Failed to add save button: " + t);
        }
    }

    /**
     * ウツワを1つでも持っているか。
     */
    private static boolean forlaism$hasUtsuwa() {
        return forlaism$countUtsuwa() > 0;
    }

    /**
     * 所持しているウツワの合計個数。
     */
    private static int forlaism$countUtsuwa() {

        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;

        if (player == null) return 0;

        var inventory = player.getInventory();

        int total = 0;

        for (int i = 0; i < inventory.getContainerSize(); i++) {

            ItemStack stack = inventory.getItem(i);

            if (stack.isEmpty()) continue;
            if (!stack.is(Items.UTSUWA.get())) continue;

            total += stack.getCount();
        }

        return total;
    }

    /**
     * Save ボタンが押された時の処理。
     */
    private static void forlaism$onSavePressed() {

        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;

        if (player == null) return;

        var inventory = player.getInventory();

        int converted = 0;

        for (int i = 0; i < inventory.getContainerSize(); i++) {

            ItemStack stack = inventory.getItem(i);

            if (stack.isEmpty()) continue;
            if (!stack.is(Items.UTSUWA.get())) continue;

            ItemStack determination = new ItemStack(Items.DETERMINATION.get());
            determination.setCount(stack.getCount());

            inventory.setItem(i, determination);

            converted += stack.getCount();
        }

        if (converted > 0) {

            player.displayClientMessage(
                    Component.literal("§4§l決意 §fが §c" + converted + " §f個、心に灯った…")
                            .withStyle(net.minecraft.ChatFormatting.DARK_RED),
                    true
            );

            if (mc.getSingleplayerServer() != null) {
                mc.getSingleplayerServer().saveEverything(false, false, false);
            }

        } else {
            player.displayClientMessage(
                    Component.literal("§7器が、無い。")
                            .withStyle(net.minecraft.ChatFormatting.GRAY),
                    true
            );
        }
    }
}