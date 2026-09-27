package com.tyami.forlaism.client.renderer;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;

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

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * レールガンのレーザービーム描画（シェーダー版）。
 */
@Mod.EventBusSubscriber(
        modid = "forlaism",
        value = Dist.CLIENT,
        bus = Mod.EventBusSubscriber.Bus.FORGE
)
public final class RailgunLaserRenderer {

    private static final int DURATION = 12;

    private static final List<Laser> ACTIVE = new ArrayList<>();

    private RailgunLaserRenderer() {
    }

    public static void spawn(Vec3 start, Vec3 end) {
        ACTIVE.add(new Laser(start, end, DURATION));
    }

    @SubscribeEvent
    public static void tick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        Iterator<Laser> it = ACTIVE.iterator();
        while (it.hasNext()) {
            Laser l = it.next();
            l.remaining--;
            if (l.remaining <= 0) it.remove();
        }
    }

    @SubscribeEvent
    public static void render(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_PARTICLES) return;
        if (ACTIVE.isEmpty()) return;

        ShaderInstance shader = RailgunShaderRegistry.getBeamShader();
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

        for (Laser l : ACTIVE) {

            float alpha = l.remaining / (float) DURATION;

            RailgunShaderRegistry.applyBeamUniforms(
                    gameTime * 0.1F,
                    alpha,
                    0.3F,
                    0.5F, 0.85F, 1.0F
            );

            Vec3 a = l.start.subtract(camPos);
            Vec3 b = l.end.subtract(camPos);

            pose.pushPose();
            Matrix4f m = pose.last().pose();

            // カメラ向きの円柱ビルボード
            Vec3 dir = b.subtract(a).normalize();
            Vec3 camDir = camPos.subtract(l.start).normalize();
            Vec3 up = dir.cross(camDir).normalize();
            if (up.lengthSqr() < 1e-6) up = new Vec3(0, 1, 0);
            Vec3 right = up.cross(dir).normalize();

            float thickness = 0.6F;

            BufferBuilder buf = Tesselator.getInstance().getBuilder();
            buf.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR_TEX);

            int sides = 8;
            for (int i = 0; i < sides; i++) {
                double a1 = i * Math.PI * 2 / sides;
                double a2 = (i + 1) * Math.PI * 2 / sides;

                Vec3 o1 = right.scale(Math.cos(a1) * thickness).add(up.scale(Math.sin(a1) * thickness));
                Vec3 o2 = right.scale(Math.cos(a2) * thickness).add(up.scale(Math.sin(a2) * thickness));

                // 進行方向 u: 0→1
                vertex(buf, m, a.add(o1), 0.0F, (float) (i / (double) sides));
                vertex(buf, m, b.add(o1), 1.0F, (float) (i / (double) sides));
                vertex(buf, m, b.add(o2), 1.0F, (float) ((i + 1) / (double) sides));
                vertex(buf, m, a.add(o2), 0.0F, (float) ((i + 1) / (double) sides));
            }

            BufferUploader.drawWithShader(buf.end());
            pose.popPose();
        }

        RenderSystem.depthMask(true);
        RenderSystem.enableCull();
        RenderSystem.enableDepthTest();
        RenderSystem.disableBlend();

        pose.popPose();
    }

    private static void vertex(BufferBuilder buf, Matrix4f m,
                                Vec3 p, float u, float v) {
        buf.vertex(m, (float) p.x, (float) p.y, (float) p.z)
                .color(1.0F, 1.0F, 1.0F, 1.0F)
                .uv(u, v)
                .endVertex();
    }

    private static final class Laser {
        final Vec3 start;
        final Vec3 end;
        int remaining;

        Laser(Vec3 start, Vec3 end, int duration) {
            this.start = start;
            this.end = end;
            this.remaining = duration;
        }
    }
}