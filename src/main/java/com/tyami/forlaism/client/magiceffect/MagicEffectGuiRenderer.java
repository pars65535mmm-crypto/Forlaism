package com.tyami.forlaism.client.magiceffect;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.ShaderInstance;
import org.joml.Matrix4f;

/**
 * GUI（インベントリ、コンテナ、チェスト、クリエイティブ等）における魔法エフェクト描画を担当するクラス。
 */
public final class MagicEffectGuiRenderer {

    private static final long START_TIME_NANOS = System.nanoTime();

    private MagicEffectGuiRenderer() {
    }

    /**
     * 現在のアニメーション経過時間（秒）を取得する。
     */
    public static float getAnimationTime() {
        return (System.nanoTime() - START_TIME_NANOS) / 1_000_000_000.0f;
    }

    /**
     * GUI上のアイテム位置 (x, y) に魔法エフェクトを描画する。
     * アイテムの16x16領域を基準に、周囲へ美しくはみ出すサイズでエフェクトを展開する。
     *
     * @param graphics GuiGraphics
     * @param x アイテムスロットのX座標
     * @param y アイテムスロットのY座標
     * @param style 適用するスタイル
     */
    public static void render(GuiGraphics graphics, int x, int y, MagicEffectStyle style) {
        if (style == null || !style.isEnabled()) {
            return;
        }

        ShaderInstance shader = MagicEffectShader.getShader();
        if (shader == null) {
            return;
        }

        float time = getAnimationTime();

        // アイテム中心座標 (cx, cy)
        float cx = x + 8.0f;
        float cy = y + 8.0f;

        // エフェクトの展開半径（アイテム16x16より外側へしっかりはみ出す半径: 約22px、全体44x44px）
        float radius = 22.0f * style.getScale();
        float x0 = cx - radius;
        float x1 = cx + radius;
        float y0 = cy - radius;
        float y1 = cy + radius;

        // GUIの深度バッファ値
        float z = 150.0f;

        // 描画ステートの変更前にグラフィックスバッファをフラッシュ
        graphics.flush();

        // 1. 描画ステートのセットアップ（純粋加算合成で鮮烈に発光）
        RenderSystem.enableBlend();
        RenderSystem.blendFunc(GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ONE);
        RenderSystem.depthMask(false);
        RenderSystem.enableDepthTest();

        // 2. シェーダーとUniformのセット
        RenderSystem.setShader(() -> shader);
        MagicEffectShader.applyUniforms(time, style);

        // 3. クアッドの描画（POSITION_COLOR_TEX）
        Matrix4f pose = graphics.pose().last().pose();

        Tesselator tesselator = Tesselator.getInstance();
        BufferBuilder builder = tesselator.getBuilder();
        builder.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR_TEX);

        // 頂点構築（アルファ減衰を防ぎ純粋な加算光を出力するため白を渡す）
        builder.vertex(pose, x0, y1, z).color(1.0f, 1.0f, 1.0f, 1.0f).uv(0.0f, 1.0f).endVertex();
        builder.vertex(pose, x1, y1, z).color(1.0f, 1.0f, 1.0f, 1.0f).uv(1.0f, 1.0f).endVertex();
        builder.vertex(pose, x1, y0, z).color(1.0f, 1.0f, 1.0f, 1.0f).uv(1.0f, 0.0f).endVertex();
        builder.vertex(pose, x0, y0, z).color(1.0f, 1.0f, 1.0f, 1.0f).uv(0.0f, 0.0f).endVertex();

        BufferUploader.drawWithShader(builder.end());

        // 4. 描画ステートを完全に復元（GUI汚染を防止）
        RenderSystem.depthMask(true);
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableBlend();
        RenderSystem.setShader(GameRenderer::getPositionColorTexShader);
    }
}
