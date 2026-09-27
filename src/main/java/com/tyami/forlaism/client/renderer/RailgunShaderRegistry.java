package com.tyami.forlaism.client.renderer;

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
 * レールガン用シェーダーの登録・Uniform設定。
 */
@Mod.EventBusSubscriber(
        modid = "forlaism",
        value = Dist.CLIENT,
        bus = Mod.EventBusSubscriber.Bus.MOD
)
public final class RailgunShaderRegistry {

    private static ShaderInstance voidFieldShader;
    private static ShaderInstance beamShader;

    private RailgunShaderRegistry() {
    }

    @SubscribeEvent
    public static void onRegisterShaders(RegisterShadersEvent event) {
        try {
            event.registerShader(
                    new ShaderInstance(
                            event.getResourceProvider(),
                            new ResourceLocation("forlaism", "void_field"),
                            DefaultVertexFormat.POSITION_COLOR_TEX
                    ),
                    s -> voidFieldShader = s
            );
        } catch (IOException e) {
            System.err.println("[Forlaism] Failed to load void_field shader: " + e.getMessage());
        }

        try {
            event.registerShader(
                    new ShaderInstance(
                            event.getResourceProvider(),
                            new ResourceLocation("forlaism", "railgun_beam"),
                            DefaultVertexFormat.POSITION_COLOR_TEX
                    ),
                    s -> beamShader = s
            );
        } catch (IOException e) {
            System.err.println("[Forlaism] Failed to load railgun_beam shader: " + e.getMessage());
        }
    }

    @Nullable
    public static ShaderInstance getVoidFieldShader() {
        return voidFieldShader;
    }

    @Nullable
    public static ShaderInstance getBeamShader() {
        return beamShader;
    }

    // =========================================================
    // Void uniform
    // =========================================================

    public static void applyVoidUniforms(float time, float alpha, float radius,
                                         float r, float g, float b) {
        if (voidFieldShader == null) return;

        set(voidFieldShader, "EffectTime", time);
        set(voidFieldShader, "EffectAlpha", alpha);
        set(voidFieldShader, "EffectRadius", radius);

        AbstractUniform c = voidFieldShader.safeGetUniform("EffectColor");
        if (c != null) c.set(r, g, b, 1.0f);
    }

    // =========================================================
    // Beam uniform
    // =========================================================

    public static void applyBeamUniforms(float time, float alpha, float thickness,
                                         float r, float g, float b) {
        if (beamShader == null) return;

        set(beamShader, "EffectTime", time);
        set(beamShader, "EffectAlpha", alpha);
        set(beamShader, "EffectThickness", thickness);

        AbstractUniform c = beamShader.safeGetUniform("EffectColor");
        if (c != null) c.set(r, g, b, 1.0f);
    }

    private static void set(ShaderInstance shader, String name, float v) {
        AbstractUniform u = shader.safeGetUniform(name);
        if (u != null) u.set(v);
    }
}