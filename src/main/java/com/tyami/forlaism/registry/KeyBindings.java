package com.tyami.forlaism.registry;

import com.mojang.blaze3d.platform.InputConstants;

import net.minecraft.client.KeyMapping;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import org.lwjgl.glfw.GLFW;

/**
 * Forlaism のキーバインド登録。
 */
@Mod.EventBusSubscriber(
        modid = "forlaism",
        bus = Mod.EventBusSubscriber.Bus.MOD,
        value = Dist.CLIENT
)
public final class KeyBindings {

    /** グラップル発射キー（デフォルト C）。 */
    public static final KeyMapping GRAPPLE_FIRE = new KeyMapping(
            "key.forlaism.grapple_fire",
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_C,
            "key.categories.forlaism"
    );

    /** マスターピースクロック バインド画面キー（デフォルト C）。 */
    public static final KeyMapping MASTERPIECE_CLOCK_BIND = new KeyMapping(
            "key.forlaism.masterpiece_clock_bind",
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_C,
            "key.categories.forlaism"
    );

    private KeyBindings() {
    }

    @SubscribeEvent
    public static void registerKeys(RegisterKeyMappingsEvent event) {
        event.register(GRAPPLE_FIRE);
        event.register(MASTERPIECE_CLOCK_BIND);
    }
}