package com.tyami.forlaism.client.gmb;

import com.mojang.blaze3d.shaders.AbstractUniform;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterShadersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;

/**
 * GMB 専用シェーダー。
 *
 * 既存の magic_item_effect / cherenkov とは完全に独立。
 * 虹色グリッチを表現する。
 */
@Mod.EventBusSubscriber(
        modid = "forlaism",
        value = Dist.CLIENT,
        bus = Mod.EventBusSubscriber.Bus.MOD
)
public final class GMBShader {

    private static ShaderInstance shader;

    private GMBShader() {
    }

    @SubscribeEvent
    public static void onRegisterShaders(RegisterShadersEvent event) {
        try {
            event.registerShader(
                    new ShaderInstance(
                            event.getResourceProvider(),
                            new ResourceLocation("forlaism", "gmb_glitch"),
                            DefaultVertexFormat.POSITION_COLOR_TEX
                    ),
                    s -> shader = s
            );
        } catch (IOException e) {
            System.err.println("[Forlaism] Failed to load gmb_glitch shader: " + e.getMessage());
        }
    }

    @Nullable
    public static ShaderInstance getShader() {
        return shader;
    }

    /**
     * GMB シェーダー用の Uniform を設定する。
     */
    public static void applyUniforms(float time, float alpha) {
        if (shader == null) return;

        set(shader, "EffectTime", time);
        set(shader, "EffectAlpha", alpha);
    }

    private static void set(ShaderInstance shader, String name, float v) {
        AbstractUniform u = shader.safeGetUniform(name);
        if (u != null) u.set(v);
    }
}