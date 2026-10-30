package com.tyami.forlaism.event;

import com.tyami.forlaism.client.AuroraShader;
import com.tyami.forlaism.world.FakedreamDimension;

import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.player.Player;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.client.event.ViewportEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import org.joml.Matrix4f;

/**
 * FakeDream の塔（x=1300, z=230）を登った時の景色変化。
 *
 * Y=500 〜 560 : 霧
 * Y=560 〜 620 : 夕焼け → 夜 → 星空+オーロラ
 */
@Mod.EventBusSubscriber(
        modid = "forlaism",
        value = Dist.CLIENT,
        bus = Mod.EventBusSubscriber.Bus.FORGE
)
public final class TowerSkyHandler {

    public static final double TOWER_X = 1300.0;
    public static final double TOWER_Z = 230.0;
    public static final double TOWER_RADIUS = 11.0;

    public static final double Y_FOG_START = 500.0;
    public static final double Y_FOG_END = 560.0;
    public static final double Y_SUNSET_START = 560.0;
    public static final double Y_NIGHT_END = 620.0;

    private TowerSkyHandler() {
    }

    public static boolean isInsideTower(Player player) {
        if (player == null) return false;
        if (!player.level().dimension().equals(FakedreamDimension.FAKEDREAM_LEVEL)) return false;
        double dx = Math.abs(player.getX() - TOWER_X);
        double dz = Math.abs(player.getZ() - TOWER_Z);
        return dx <= TOWER_RADIUS && dz <= TOWER_RADIUS;
    }

    // =========================================================
    // 霧
    // =========================================================


    /**
 * 霧の強さを 0.0 〜 1.0 で返す。
 *
 * Y=500 で 0.0
 * Y=560 で 1.0（ピーク）
 * Y=620 で 0.0（霧が晴れる）
 */
private static float getFogIntensity(Player player) {
    double y = player.getY();

    if (y <= Y_FOG_START) return 0.0F;
    if (y >= Y_NIGHT_END) return 0.0F;

    // 500 〜 560 : 0.0 → 1.0
    if (y < Y_FOG_END) {
        float t = (float) ((y - Y_FOG_START) / (Y_FOG_END - Y_FOG_START));
        return Mth.clamp(t, 0.0F, 1.0F);
    }

    // 560 〜 620 : 1.0 → 0.0
    float t = (float) ((y - Y_FOG_END) / (Y_NIGHT_END - Y_FOG_END));
    return Mth.clamp(1.0F - t, 0.0F, 1.0F);
}



@SubscribeEvent
public static void onComputeFogColor(ViewportEvent.ComputeFogColor event) {
    Player player = Minecraft.getInstance().player;
    if (player == null) return;
    if (!isInsideTower(player)) return;

    float fogT = getFogIntensity(player);
    if (fogT <= 0.0F) return;

    event.setRed(Mth.lerp(fogT, event.getRed(), 0.95F));
    event.setGreen(Mth.lerp(fogT, event.getGreen(), 0.95F));
    event.setBlue(Mth.lerp(fogT, event.getBlue(), 1.00F));
}

@SubscribeEvent
public static void onRenderFog(ViewportEvent.RenderFog event) {
    Player player = Minecraft.getInstance().player;
    if (player == null) return;
    if (!isInsideTower(player)) return;

    float fogT = getFogIntensity(player);
    if (fogT <= 0.0F) return;

    float far = Mth.lerp(fogT, 128.0F, 12.0F);

    event.setNearPlaneDistance(0.0F);
    event.setFarPlaneDistance(far);
    event.setFogShape(com.mojang.blaze3d.shaders.FogShape.SPHERE);
    event.setCanceled(true);
}

    // =========================================================
    // 空・星空・オーロラ
    // =========================================================

    @SubscribeEvent
    public static void onRenderLevel(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_SKY) return;

        Player player = Minecraft.getInstance().player;
        if (!isInsideTower(player)) return;

        double y = player.getY();
        if (y < Y_SUNSET_START) return;

        float t = (float) ((y - Y_SUNSET_START) / (Y_NIGHT_END - Y_SUNSET_START));
        t = Mth.clamp(t, 0.0F, 1.0F);

        // 空の色
        float r, g, b;
        if (t < 0.5F) {
            float p = t / 0.5F;
            r = Mth.lerp(p, 1.00F, 0.30F);
            g = Mth.lerp(p, 0.55F, 0.15F);
            b = Mth.lerp(p, 0.20F, 0.40F);
        } else {
            float p = (t - 0.5F) / 0.5F;
            r = Mth.lerp(p, 0.30F, 0.02F);
            g = Mth.lerp(p, 0.15F, 0.03F);
            b = Mth.lerp(p, 0.40F, 0.15F);
        }

        com.mojang.blaze3d.systems.RenderSystem.disableDepthTest();
        com.mojang.blaze3d.systems.RenderSystem.depthMask(false);

        // =========================================================
        // 1. 空の背景
        // =========================================================
        var tesselator = com.mojang.blaze3d.vertex.Tesselator.getInstance();
        var buffer = tesselator.getBuilder();
        buffer.begin(
                com.mojang.blaze3d.vertex.VertexFormat.Mode.QUADS,
                com.mojang.blaze3d.vertex.DefaultVertexFormat.POSITION_COLOR
        );
        Matrix4f mat = event.getPoseStack().last().pose();
        float bgAlpha = t;
        buffer.vertex(mat, -2000F, -2000F, 0).color(r, g, b, bgAlpha).endVertex();
        buffer.vertex(mat,  2000F, -2000F, 0).color(r, g, b, bgAlpha).endVertex();
        buffer.vertex(mat,  2000F,  2000F, 0).color(r, g, b, bgAlpha).endVertex();
        buffer.vertex(mat, -2000F,  2000F, 0).color(r, g, b, bgAlpha).endVertex();
        com.mojang.blaze3d.vertex.BufferUploader.drawWithShader(buffer.end());

        // =========================================================
        // 2. 星（t > 0.5 で出てくる）
        // =========================================================
        float starAlpha = Mth.clamp((t - 0.5F) / 0.4F, 0.0F, 1.0F);
        if (starAlpha > 0.01F) {
            renderStars(event, starAlpha);
        }

        // =========================================================
        // 3. オーロラ（t > 0.7 で出てくる）
        // =========================================================
        float auroraAlpha = Mth.clamp((t - 0.7F) / 0.3F, 0.0F, 1.0F);
        if (auroraAlpha > 0.01F) {
            renderAurora(event, auroraAlpha);
        }

        com.mojang.blaze3d.systems.RenderSystem.depthMask(true);
        com.mojang.blaze3d.systems.RenderSystem.enableDepthTest();
    }

    // =========================================================
    // 星
    // =========================================================

    private static void renderStars(RenderLevelStageEvent event, float alpha) {
        // シード固定で星の位置を決める（フレームごとに変わらない）
        RandomSource rand = RandomSource.create(12345L);

        var tesselator = com.mojang.blaze3d.vertex.Tesselator.getInstance();
        var buffer = tesselator.getBuilder();
        buffer.begin(
                com.mojang.blaze3d.vertex.VertexFormat.Mode.QUADS,
                com.mojang.blaze3d.vertex.DefaultVertexFormat.POSITION_COLOR
        );
        Matrix4f mat = event.getPoseStack().last().pose();

        // 上方向の疑似星空（縦横 2000 の範囲に点を打つ）
        int starCount = 600;
        for (int i = 0; i < starCount; i++) {
            float x = (rand.nextFloat() - 0.5F) * 4000F;
            float y = (rand.nextFloat() - 0.5F) * 4000F;
            float size = 0.8F + rand.nextFloat() * 1.6F;

            // 明るさにばらつき
            float bright = 0.5F + rand.nextFloat() * 0.5F;
            float a = bright * alpha;

            // 4頂点の正方形
            buffer.vertex(mat, x - size, y - size, 0).color(1F, 1F, 1F, a).endVertex();
            buffer.vertex(mat, x + size, y - size, 0).color(1F, 1F, 1F, a).endVertex();
            buffer.vertex(mat, x + size, y + size, 0).color(1F, 1F, 1F, a).endVertex();
            buffer.vertex(mat, x - size, y + size, 0).color(1F, 1F, 1F, a).endVertex();
        }

        com.mojang.blaze3d.vertex.BufferUploader.drawWithShader(buffer.end());
    }

    // =========================================================
    // オーロラ
    // =========================================================

    private static void renderAurora(RenderLevelStageEvent event, float alpha) {
        var shader = AuroraShader.getShader();
        if (shader == null) return;

        float time = (System.nanoTime() % 1_000_000_000_000L) / 1_000_000_000.0F;

        com.mojang.blaze3d.systems.RenderSystem.setShader(() -> shader);
        AuroraShader.applyUniforms(time, alpha);

        var tesselator = com.mojang.blaze3d.vertex.Tesselator.getInstance();
        var buffer = tesselator.getBuilder();
        buffer.begin(
                com.mojang.blaze3d.vertex.VertexFormat.Mode.QUADS,
                com.mojang.blaze3d.vertex.DefaultVertexFormat.POSITION_COLOR_TEX
        );
        Matrix4f mat = event.getPoseStack().last().pose();

        // 画面上部 1/3 くらいに張る 1 枚の板ポリ
        float w = 4000F;
        float top = 1500F;
        float bottom = 200F;

        buffer.vertex(mat, -w, bottom, 0).color(1F, 1F, 1F, 1F).uv(0F, 0F).endVertex();
        buffer.vertex(mat,  w, bottom, 0).color(1F, 1F, 1F, 1F).uv(1F, 0F).endVertex();
        buffer.vertex(mat,  w, top,    0).color(1F, 1F, 1F, 1F).uv(1F, 1F).endVertex();
        buffer.vertex(mat, -w, top,    0).color(1F, 1F, 1F, 1F).uv(0F, 1F).endVertex();

        com.mojang.blaze3d.vertex.BufferUploader.drawWithShader(buffer.end());
    }
}