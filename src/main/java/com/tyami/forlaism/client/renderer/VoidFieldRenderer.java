package com.tyami.forlaism.client.renderer;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;

import com.tyami.forlaism.world.VoidFieldManager;

import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.world.phys.Vec3;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import org.joml.Matrix4f;

/**
 * 虚空フィールドの球体描画。
 */
@Mod.EventBusSubscriber(
        modid = "forlaism",
        value = Dist.CLIENT,
        bus = Mod.EventBusSubscriber.Bus.FORGE
)
public final class VoidFieldRenderer {

    private VoidFieldRenderer() {
    }

    @SubscribeEvent
    public static void render(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_PARTICLES) return;

        // クライアント側の虚空寿命を進める
        VoidFieldManager.clientTick();

        var fields = VoidFieldManager.snapshot();
        if (fields.isEmpty()) return;

        ShaderInstance shader = RailgunShaderRegistry.getVoidFieldShader();
        if (shader == null) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return;

        Camera cam = event.getCamera();
        Vec3 camPos = cam.getPosition();
        float partial = event.getPartialTick();
        float gameTime = mc.level.getGameTime() + partial;

        PoseStack pose = event.getPoseStack();

        pose.pushPose();

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableDepthTest();
        RenderSystem.disableCull();
        RenderSystem.depthMask(false);

        RenderSystem.setShader(() -> shader);

        for (var f : fields) {

            // フェード
            float life = f.remainingTicks() / (float) f.durationTicks();
            float alpha = Math.min(1.0F, life * 2.5F);

            // 見た目の半径（実ダメージ範囲は 45m だが、見た目は少し小さめ）
            float visualRadius = 10.0F;

            // uniform
            RailgunShaderRegistry.applyVoidUniforms(
                    gameTime * 0.1F,  // シェーダー内時間（ゆっくり）
                    alpha,
                    visualRadius,
                    0.4F, 0.8F, 1.0F
            );

            pose.pushPose();
            pose.translate(
                    f.x() - camPos.x,
                    f.y() - camPos.y,
                    f.z() - camPos.z
            );

            Matrix4f m = pose.last().pose();

            // 1個ずつ begin/end
            BufferBuilder b = Tesselator.getInstance().getBuilder();
            drawSphere(b, m, visualRadius);

            pose.popPose();
        }

        RenderSystem.depthMask(true);
        RenderSystem.enableCull();
        RenderSystem.enableDepthTest();
        RenderSystem.disableBlend();

        pose.popPose();
    }

    // =========================================================
    // 球体メッシュ
    // =========================================================

    private static final int LAT = 48;
    private static final int LON = 92;

    private static void drawSphere(BufferBuilder buf, Matrix4f m, float radius) {
        buf.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR_TEX);

        for (int i = 0; i < LAT; i++) {
            float v0 = i / (float) LAT;
            float v1 = (i + 1) / (float) LAT;
            float phi0 = (float) (v0 * Math.PI);
            float phi1 = (float) (v1 * Math.PI);

            for (int j = 0; j < LON; j++) {
                float u0 = j / (float) LON;
                float u1 = (j + 1) / (float) LON;
                float theta0 = (float) (u0 * Math.PI * 2);
                float theta1 = (float) (u1 * Math.PI * 2);

                float x00 = (float) (Math.sin(phi0) * Math.cos(theta0)) * radius;
                float y00 = (float) (Math.cos(phi0)) * radius;
                float z00 = (float) (Math.sin(phi0) * Math.sin(theta0)) * radius;

                float x10 = (float) (Math.sin(phi1) * Math.cos(theta0)) * radius;
                float y10 = (float) (Math.cos(phi1)) * radius;
                float z10 = (float) (Math.sin(phi1) * Math.sin(theta0)) * radius;

                float x11 = (float) (Math.sin(phi1) * Math.cos(theta1)) * radius;
                float y11 = (float) (Math.cos(phi1)) * radius;
                float z11 = (float) (Math.sin(phi1) * Math.sin(theta1)) * radius;

                float x01 = (float) (Math.sin(phi0) * Math.cos(theta1)) * radius;
                float y01 = (float) (Math.cos(phi0)) * radius;
                float z01 = (float) (Math.sin(phi0) * Math.sin(theta1)) * radius;

                vertex(buf, m, x00, y00, z00, u0, v0);
                vertex(buf, m, x10, y10, z10, u0, v1);
                vertex(buf, m, x11, y11, z11, u1, v1);
                vertex(buf, m, x01, y01, z01, u1, v0);
            }
        }

        BufferUploader.drawWithShader(buf.end());
    }

    private static void vertex(BufferBuilder buf, Matrix4f m,
                                float x, float y, float z,
                                float u, float v) {
        buf.vertex(m, x, y, z)
                .color(1.0F, 1.0F, 1.0F, 1.0F)
                .uv(u, v)
                .endVertex();
    }
}