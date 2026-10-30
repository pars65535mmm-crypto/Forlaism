package com.tyami.forlaism.client;

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
 * オーロラ用シェーダーの登録。
 */
@Mod.EventBusSubscriber(
        modid = "forlaism",
        value = Dist.CLIENT,
        bus = Mod.EventBusSubscriber.Bus.MOD
)
public final class AuroraShader {

    private static ShaderInstance shader;

    private AuroraShader() {
    }

    @SubscribeEvent
    public static void onRegisterShaders(RegisterShadersEvent event) {
        try {
            event.registerShader(
                    new ShaderInstance(
                            event.getResourceProvider(),
                            new ResourceLocation("forlaism", "aurora"),
                            DefaultVertexFormat.POSITION_COLOR_TEX
                    ),
                    s -> shader = s
            );
        } catch (IOException e) {
            System.err.println("[Forlaism] Failed to load aurora shader: " + e.getMessage());
        }
    }

    @Nullable
    public static ShaderInstance getShader() {
        return shader;
    }

    public static void applyUniforms(float time, float alpha) {
        if (shader == null) return;
        AbstractUniform t = shader.safeGetUniform("EffectTime");
        if (t != null) t.set(time);
        AbstractUniform a = shader.safeGetUniform("EffectAlpha");
        if (a != null) a.set(alpha);
    }
}