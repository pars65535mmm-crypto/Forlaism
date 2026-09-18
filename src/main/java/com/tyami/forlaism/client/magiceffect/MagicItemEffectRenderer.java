package com.tyami.forlaism.client.magiceffect;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * 幻想的な魔法アイテムエフェクトの描画システムにおけるメインエントリーポイント。
 * GUI、ItemEntity、手持ちアイテム、カスタムスクリーンなど、任意の場所から任意のItemStackに対して呼び出し可能。
 */
public final class MagicItemEffectRenderer {

    private MagicItemEffectRenderer() {
    }

    // ==========================================
    // GUI 向け描画 API
    // ==========================================

    /**
     * GUI上でItemStackに対応する魔法エフェクトを描画する。
     * ItemStackにエフェクトが設定されていない場合は何もしない。
     *
     * @param graphics GuiGraphics
     * @param stack 対象のItemStack
     * @param x スロットのX座標
     * @param y スロットのY座標
     */
    public static void renderGui(@NotNull GuiGraphics graphics, @NotNull ItemStack stack, int x, int y) {
        if (stack.isEmpty()) {
            return;
        }
        MagicEffectStyle style = MagicEffectManager.getStyle(stack);
        if (style != null) {
            renderGui(graphics, x, y, style);
        }
    }

    /**
     * GUI上で指定されたスタイルを用いて魔法エフェクトを描画する。
     *
     * @param graphics GuiGraphics
     * @param x スロットのX座標
     * @param y スロットのY座標
     * @param style 描画スタイル
     */
    public static void renderGui(@NotNull GuiGraphics graphics, int x, int y, @Nullable MagicEffectStyle style) {
        if (style == null || !style.isEnabled()) {
            return;
        }
        MagicEffectGuiRenderer.render(graphics, x, y, style);
    }

    /**
     * GUI上で指定されたスタイル（オーバーライド用）を用いて描画する。
     */
    public static void renderGui(@NotNull GuiGraphics graphics, @NotNull ItemStack stack, int x, int y, @Nullable MagicEffectStyle styleOverride) {
        MagicEffectStyle style = styleOverride != null ? styleOverride : MagicEffectManager.getStyle(stack);
        if (style != null) {
            renderGui(graphics, x, y, style);
        }
    }

    // ==========================================
    // ワールド / 3D 向け描画 API
    // ==========================================

    /**
     * ワールド内でItemStackに対応する魔法エフェクトを描画する。
     *
     * @param stack 対象のItemStack
     * @param poseStack PoseStack
     * @param buffer MultiBufferSource
     * @param billboard trueの場合カメラに向かって正対回転する
     */
    public static void renderWorld(@NotNull ItemStack stack, @NotNull PoseStack poseStack, @NotNull MultiBufferSource buffer, boolean billboard) {
        if (stack.isEmpty()) {
            return;
        }
        MagicEffectStyle style = MagicEffectManager.getStyle(stack);
        if (style != null) {
            renderWorld(poseStack, buffer, style, billboard);
        }
    }

    /**
     * ワールド内で指定されたスタイルを用いて魔法エフェクトを描画する。
     *
     * @param poseStack PoseStack
     * @param buffer MultiBufferSource
     * @param style 描画スタイル
     * @param billboard trueの場合カメラに向かって正対回転する
     */
    public static void renderWorld(@NotNull PoseStack poseStack, @NotNull MultiBufferSource buffer, @Nullable MagicEffectStyle style, boolean billboard) {
        if (style == null || !style.isEnabled()) {
            return;
        }
        MagicEffectWorldRenderer.render(poseStack, buffer, style, billboard);
    }

    /**
     * ワールド描画（スタイルオーバーライド対応版）
     */
    public static void renderWorld(@NotNull ItemStack stack, @NotNull PoseStack poseStack, @NotNull MultiBufferSource buffer, @Nullable MagicEffectStyle styleOverride, boolean billboard) {
        MagicEffectStyle style = styleOverride != null ? styleOverride : MagicEffectManager.getStyle(stack);
        if (style != null) {
            renderWorld(poseStack, buffer, style, billboard);
        }
    }

    // ==========================================
    // 汎用ショートハンド API
    // ==========================================

    /**
     * GUI描画用ショートハンド
     */
    public static void render(@NotNull ItemStack stack, @NotNull GuiGraphics graphics, int x, int y) {
        renderGui(graphics, stack, x, y);
    }

    /**
     * ワールド描画用ショートハンド（カメラビルボード有効）
     */
    public static void render(@NotNull ItemStack stack, @NotNull PoseStack poseStack, @NotNull MultiBufferSource buffer) {
        renderWorld(stack, poseStack, buffer, true);
    }

    /**
     * ワールド描画用ショートハンド（スタイル指定、カメラビルボード有効）
     */
    public static void render(@NotNull ItemStack stack, @NotNull PoseStack poseStack, @NotNull MultiBufferSource buffer, @Nullable MagicEffectStyle style) {
        renderWorld(stack, poseStack, buffer, style, true);
    }
}
