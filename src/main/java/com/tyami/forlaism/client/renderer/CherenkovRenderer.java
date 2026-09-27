package com.tyami.forlaism.client.renderer;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.tyami.forlaism.client.magiceffect.CherenkovShader;

import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.world.phys.Vec3;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import org.joml.Matrix4f;

/**
 * チェレンコフ光演出のレンダラ。
 *
 * 加工中心の真上に巨大な光輪（板ポリ）を表示し、
 * シェーダーで回転する発光リングを描画する。
 */
@Mod.EventBusSubscriber(
        modid = "forlaism",
        bus = Mod.EventBusSubscriber.Bus.FORGE,
        value = Dist.CLIENT
)
public final class CherenkovRenderer {

    /** リングの半径（ブロック単位）。 */
    private static final float RADIUS = 8.0f;

    private CherenkovRenderer() {
    }

    /** クライアント側の毎tick更新。 */
    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        CherenkovState.tick();
    }

    /** ワールド描画。 */
    @SubscribeEvent
    public static void onRenderLevel(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_PARTICLES) return;

        var active = CherenkovState.getActive();
        if (active.isEmpty()) return;

        ShaderInstance shader = CherenkovShader.getShader();
        if (shader == null) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return;

        Camera camera = event.getCamera();
        Vec3 camPos = camera.getPosition();
        float partialTick = event.getPartialTick();
        PoseStack pose = event.getPoseStack();

        // =========================================================
        // レンダリング準備
        // =========================================================
        pose.pushPose();

        RenderSystem.enableBlend();
        RenderSystem.blendFunc(
                com.mojang.blaze3d.platform.GlStateManager.SourceFactor.SRC_ALPHA,
                com.mojang.blaze3d.platform.GlStateManager.DestFactor.ONE
        );
        RenderSystem.disableDepthTest();
        RenderSystem.depthMask(false);
        RenderSystem.disableCull();

        RenderSystem.setShader(() -> shader);

        Tesselator tesselator = Tesselator.getInstance();
        BufferBuilder buffer = tesselator.getBuilder();

        for (CherenkovState.Instance inst : active) {

            float time = inst.tick + partialTick;
            float alpha = inst.fadeAlpha();

            // シェーダーの Uniform を毎回セット
            CherenkovShader.applyUniforms(time, alpha);

            // =====================================================
            // 中心のワールド座標（板ポリの中心）
            // =====================================================
            double wx = inst.center.getX() + 0.5;
            double wy = inst.center.getY() + 3.5;  // 縦穴の真ん中
            double wz = inst.center.getZ() + 0.5;

            pose.pushPose();
            pose.translate(wx - camPos.x, wy - camPos.y, wz - camPos.z);

            // カメラの方を向く（ビルボード）
            var camRotation = camera.rotation();
            pose.mulPose(camRotation);

            Matrix4f matrix = pose.last().pose();

            // =====================================================
            // 3枚の板ポリ（XY / XZ / YZ）を配置して
            // どの角度からも光輪が見えるようにする
            // =====================================================

            // XY平面
            buffer.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR_TEX);
            drawQuad(buffer, matrix, RADIUS);
            BufferUploader.drawWithShader(buffer.end());

            // 90度回転（XZ平面）… でも既にビルボードしてるので、
            // 追加の回転は pose で行う
            pose.pushPose();
            pose.mulPose(com.mojang.math.Axis.XP.rotationDegrees(90.0f));
            buffer.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR_TEX);
            drawQuad(buffer, pose.last().pose(), RADIUS);
            BufferUploader.drawWithShader(buffer.end());
            pose.popPose();

            pose.pushPose();
            pose.mulPose(com.mojang.math.Axis.YP.rotationDegrees(90.0f));
            buffer.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR_TEX);
            drawQuad(buffer, pose.last().pose(), RADIUS);
            BufferUploader.drawWithShader(buffer.end());
            pose.popPose();

            pose.popPose();
        }

        // =========================================================
        // レンダリング後始末
        // =========================================================
        RenderSystem.depthMask(true);
        RenderSystem.enableDepthTest();
        RenderSystem.enableCull();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableBlend();

        pose.popPose();
    }

    private static void drawQuad(BufferBuilder buffer, Matrix4f matrix, float radius) {
        // POSITION_COLOR_TEX
        buffer.vertex(matrix, -radius, -radius, 0).color(1f, 1f, 1f, 1f).uv(0f, 1f).endVertex();
        buffer.vertex(matrix,  radius, -radius, 0).color(1f, 1f, 1f, 1f).uv(1f, 1f).endVertex();
        buffer.vertex(matrix,  radius,  radius, 0).color(1f, 1f, 1f, 1f).uv(1f, 0f).endVertex();
        buffer.vertex(matrix, -radius,  radius, 0).color(1f, 1f, 1f, 1f).uv(0f, 0f).endVertex();
    }
}