package com.tyami.forlaism.client;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.tyami.forlaism.world.BloodMoonManager;

import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import org.joml.Matrix4f;

/**
 * ブラッドムーン中の赤い月と発光。
 *
 * 空描画後に「赤い月」を上書きで描画する。
 */
@Mod.EventBusSubscriber(
        modid = "forlaism",
        value = Dist.CLIENT,
        bus = Mod.EventBusSubscriber.Bus.FORGE
)
public final class BloodMoonSkyRenderer {

    private BloodMoonSkyRenderer() {
    }

    private static boolean active() {
        Minecraft mc = Minecraft.getInstance();
        ClientLevel level = mc.level;
        if (level == null) return false;
        if (!BloodMoonManager.isClientActive()) return false;
        long time = level.getDayTime() % 24000L;
        return time >= 13000L && time < 23000L;
    }

    @SubscribeEvent
    public static void onRenderSky(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_SKY) return;
        if (!active()) return;

        Minecraft mc = Minecraft.getInstance();
        ClientLevel level = mc.level;
        if (level == null) return;

        // 月の方角を計算（バニラ準拠）
        long dayTime = level.getDayTime();
        float celestialAngle = level.getTimeOfDay(event.getPartialTick());
        float moonAngle = celestialAngle - 0.25F;
        double moonRot = Mth.frac(moonAngle);

        // 月の仰角・方位
        double yaw = Math.PI * 2 * moonRot; // 縦回転
        double pitch = 0; // 真横方向

        // 仮: カメラ位置基準で「月の方角」に赤い円を描く
        Camera cam = event.getCamera();
        Vec3 camPos = cam.getPosition();
        PoseStack pose = event.getPoseStack();

        pose.pushPose();

        // 月の方角をワールド空間に投影
        double dist = 100.0D;
        double mx = Math.cos(moonRot * Math.PI * 2 - Math.PI / 2) * dist;
        double my = Math.sin(moonRot * Math.PI * 2 - Math.PI / 2) * dist;
        double mz = -0.5D * dist;

        pose.translate(mx, my, mz);

        // カメラ向きに合わせる
        var camRot = cam.rotation();
        pose.mulPose(camRot);

        RenderSystem.enableBlend();
        RenderSystem.blendFunc(
                com.mojang.blaze3d.platform.GlStateManager.SourceFactor.SRC_ALPHA,
                com.mojang.blaze3d.platform.GlStateManager.DestFactor.ONE
        );
        RenderSystem.depthMask(false);
        RenderSystem.disableCull();
        RenderSystem.setShader(GameRenderer::getPositionColorShader);

        Matrix4f mat = pose.last().pose();

        // =========================================================
        // 1. 赤い月本体
        // =========================================================
        Tesselator tess = Tesselator.getInstance();
        BufferBuilder bb = tess.getBuilder();
        bb.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);

        float size = 30.0F;
        // 中心赤・外縁赤黒
        addMoonDisc(bb, mat, size, 0.85F, 0.10F, 0.05F, 1.0F);

        BufferUploader.drawWithShader(bb.end());

        // =========================================================
        // 2. 発光オーラ（周囲）
        // =========================================================
        bb.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
        addMoonDisc(bb, mat, size * 2.2F, 1.0F, 0.2F, 0.1F, 0.35F);
        BufferUploader.drawWithShader(bb.end());

        RenderSystem.depthMask(true);
        RenderSystem.enableCull();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableBlend();

        pose.popPose();
    }

    /**
     * 円形ディスクを描画。
     */
    private static void addMoonDisc(BufferBuilder bb, Matrix4f m,
                                    float radius,
                                    float r, float g, float b, float a) {
        int segments = 48;
        for (int i = 0; i < segments; i++) {
            double a1 = (i / (double) segments) * Math.PI * 2;
            double a2 = ((i + 1) / (double) segments) * Math.PI * 2;

            float x1 = (float) Math.cos(a1) * radius;
            float y1 = (float) Math.sin(a1) * radius;
            float x2 = (float) Math.cos(a2) * radius;
            float y2 = (float) Math.sin(a2) * radius;

            // 中心
            bb.vertex(m, 0, 0, 0).color(r, g, b, a).endVertex();
            bb.vertex(m, x1, y1, 0).color(r * 0.5F, g * 0.5F, b * 0.5F, 0F).endVertex();
            bb.vertex(m, x2, y2, 0).color(r * 0.5F, g * 0.5F, b * 0.5F, 0F).endVertex();
            bb.vertex(m, 0, 0, 0).color(r, g, b, a).endVertex();
        }
    }
}