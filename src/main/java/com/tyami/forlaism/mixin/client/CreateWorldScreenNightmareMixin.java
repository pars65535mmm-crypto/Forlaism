package com.tyami.forlaism.mixin.client;

import com.tyami.forlaism.world.NightmareSavedData;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.worldselection.CreateWorldScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.lang.reflect.Method;
import java.util.List;

/**
 * ワールド作成画面に「ナイトメアモード」トグルボタンを追加する。
 *
 * 押すと ON/OFF が切り替わる。
 * ワールド作成後、最初のログイン時にフラグを適用する。
 */
@Mixin(CreateWorldScreen.class)
public abstract class CreateWorldScreenNightmareMixin {

    private static Method ADD_WIDGET_METHOD;
    private static Method CHILDREN_METHOD;

    static {
        for (Method m : Screen.class.getDeclaredMethods()) {
            String n = m.getName();
            if (n.equals("addRenderableWidget") || n.equals("m_142416_")) {
                m.setAccessible(true);
                ADD_WIDGET_METHOD = m;
                break;
            }
        }
        for (Method m : Screen.class.getDeclaredMethods()) {
            String n = m.getName();
            if (n.equals("children") || n.equals("m_6702_")) {
                m.setAccessible(true);
                CHILDREN_METHOD = m;
                break;
            }
        }
    }

    @Inject(method = "init", at = @At("RETURN"))
    private void forlaism$addNightmareToggle(CallbackInfo ci) {

        CreateWorldScreen self = (CreateWorldScreen) (Object) this;

        if (ADD_WIDGET_METHOD == null || CHILDREN_METHOD == null) return;

        try {
            List<?> children = (List<?>) CHILDREN_METHOD.invoke(self);

            Button createButton = null;
            for (Object child : children) {
                if (!(child instanceof Button btn)) continue;
                String text = btn.getMessage().getString();
                if (text.equals("Create New World")
                        || text.equals("新しいワールドをクリエイト")
                        || text.contains("Create")) {
                    createButton = btn;
                    break;
                }
            }

            if (createButton == null) return;

            int x = createButton.getX();
            int y = createButton.getY() + createButton.getHeight() + 4;

            Button toggle = Button.builder(
                    Component.literal("§5§lナイトメアモード: §aOFF"),
                    b -> toggleNightmare(b)
            ).bounds(x, y, createButton.getWidth(), createButton.getHeight()).build();

            ADD_WIDGET_METHOD.invoke(self, toggle);

        } catch (Throwable ignored) {
        }
    }

    private static void toggleNightmare(Button btn) {
        NightmareSavedData data = NightmareSavedData.get(
                net.minecraftforge.server.ServerLifecycleHooks.getCurrentServer()
        );
        boolean next = !data.isNightmare();
        data.setNightmare(next);

        btn.setMessage(Component.literal(
                "§5§lナイトメアモード: " + (next ? "§cON" : "§aOFF")
        ));
    }
}