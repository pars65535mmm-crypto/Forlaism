package com.tyami.forlaism.client.magiceffect;

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
 * チェレンコフ光演出用シェーダー。
 */
@Mod.EventBusSubscriber(
        modid = "forlaism",
        value = Dist.CLIENT,
        bus = Mod.EventBusSubscriber.Bus.MOD
)
public final class CherenkovShader {

    private static ShaderInstance shader;

    private CherenkovShader() {
    }

    @SubscribeEvent
    public static void onRegisterShaders(RegisterShadersEvent event) {
        try {
            event.registerShader(
                    new ShaderInstance(
                            event.getResourceProvider(),
                            new ResourceLocation("forlaism", "cherenkov"),
                            DefaultVertexFormat.POSITION_COLOR_TEX
                    ),
                    s -> shader = s
            );
        } catch (IOException e) {
            System.err.println("[Forlaism] Failed to load cherenkov shader: " + e.getMessage());
        }
    }

    @Nullable
    public static ShaderInstance getShader() {
        return shader;
    }

    public static void applyUniforms(float time, float alpha) {
        if (shader == null) return;

        AbstractUniform uTime = shader.safeGetUniform("EffectTime");
        if (uTime != null) uTime.set(time);

        AbstractUniform uAlpha = shader.safeGetUniform("EffectAlpha");
        if (uAlpha != null) uAlpha.set(alpha);

        AbstractUniform uColor = shader.safeGetUniform("EffectColor");
        if (uColor != null) uColor.set(0.6f, 0.85f, 1.0f, 1.0f); // チェレンコフ青
    }
}