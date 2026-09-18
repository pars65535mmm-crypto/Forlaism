package com.tyami.forlaism.client.magiceffect;

import com.mojang.blaze3d.shaders.AbstractUniform;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.renderer.RenderStateShard;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterShadersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;

/**
 * 魔法エフェクト用のカスタムShaderInstanceの登録・管理およびRenderTypeを提供するクラス。
 */
@Mod.EventBusSubscriber(modid = "forlaism", value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
public final class MagicEffectShader extends RenderType {

    private static ShaderInstance magicShader;
    private static RenderType worldRenderType;

    // ダミーコンストラクタ（RenderTypeのprotected定数にアクセスするための継承）
    private MagicEffectShader(String name, VertexFormat format, VertexFormat.Mode mode, int bufferSize, boolean affectsCrumbling, boolean sortOnUpload, Runnable setupState, Runnable clearState) {
        super(name, format, mode, bufferSize, affectsCrumbling, sortOnUpload, setupState, clearState);
    }

    @SubscribeEvent
    public static void onRegisterShaders(RegisterShadersEvent event) {
        try {
            event.registerShader(
                    new ShaderInstance(
                            event.getResourceProvider(),
                            new ResourceLocation("forlaism", "magic_item_effect"),
                            DefaultVertexFormat.POSITION_COLOR_TEX
                    ),
                    shader -> {
                        magicShader = shader;
                    }
            );
        } catch (IOException e) {
            System.err.println("[Forlaism] Failed to load magic_item_effect shader: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @Nullable
    public static ShaderInstance getShader() {
        return magicShader;
    }

    /**
     * シェーダーのUniformを設定する。
     */
    public static void applyUniforms(float gameTimeSeconds, MagicEffectStyle style) {
        if (magicShader == null) {
            return;
        }

        // EffectTime (バニラのGAME_TIME上書きを回避する独自時間Uniform)
        AbstractUniform uEffectTime = magicShader.safeGetUniform("EffectTime");
        if (uEffectTime != null) {
            uEffectTime.set(gameTimeSeconds);
        }

        // EffectColor (r, g, b, a)
        AbstractUniform uColor = magicShader.safeGetUniform("EffectColor");
        if (uColor != null) {
            float[] c = style.getColorComponents();
            uColor.set(c[0], c[1], c[2], c[3]);
        }

        // EffectParams (intensity, scale, speed, ringCount)
        AbstractUniform uParams = magicShader.safeGetUniform("EffectParams");
        if (uParams != null) {
            uParams.set(
                    style.getIntensity(),
                    style.getScale(),
                    style.getSpeed(),
                    (float) style.getRingCount()
            );
        }

        // EffectExtra (particleCount, flaresEnabled, coreGlowEnabled, 0.0)
        AbstractUniform uExtra = magicShader.safeGetUniform("EffectExtra");
        if (uExtra != null) {
            uExtra.set(
                    (float) style.getParticleCount(),
                    style.hasFlares() ? 1.0f : 0.0f,
                    style.hasCoreGlow() ? 1.0f : 0.0f,
                    0.0f
            );
        }
    }

    /**
     * ワールド描画用の加算半透明RenderTypeを取得する。
     */
    public static RenderType getWorldRenderType() {
        if (worldRenderType == null) {
            worldRenderType = create(
                    "forlaism_magic_item_effect",
                    DefaultVertexFormat.POSITION_COLOR_TEX,
                    VertexFormat.Mode.QUADS,
                    256,
                    false,
                    true,
                    CompositeState.builder()
                            .setShaderState(new RenderStateShard.ShaderStateShard(() -> magicShader))
                            .setTransparencyState(ADDITIVE_TRANSPARENCY)
                            .setDepthTestState(LEQUAL_DEPTH_TEST)
                            .setCullState(NO_CULL)
                            .setWriteMaskState(COLOR_WRITE) // 深度バッファへの書き込みをOFFにして加算合成
                            .createCompositeState(false)
            );
        }
        return worldRenderType;
    }
}
