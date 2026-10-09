package com.tyami.forlaism.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.tyami.forlaism.block.entity.AltarBlockEntity;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

/**
 * 儀式祭壇の BlockEntityRenderer。
 *
 * 9x9 = 81 個のアイテムを全部描画する。
 */
public class AltarBlockEntityRenderer implements BlockEntityRenderer<AltarBlockEntity> {

    private static final int RITUAL_DURATION = AltarBlockEntity.RITUAL_DURATION;

    public AltarBlockEntityRenderer(BlockEntityRendererProvider.Context ctx) {
    }

    @Override
    public void render(AltarBlockEntity be, float partialTick,
                       PoseStack pose, MultiBufferSource buffer,
                       int light, int overlay) {

        Minecraft mc = Minecraft.getInstance();
        ItemRenderer itemRenderer = mc.getItemRenderer();

        float time;
        if (be.getLevel() != null) {
            time = be.getLevel().getGameTime() + partialTick;
        } else {
            time = 0;
        }

        boolean crafting = be.isCrafting();
        float progress = be.getRitualProgress(partialTick);

        // =========================================================
        // 81 個の配置済みアイテムを描画
        // =========================================================
        if (crafting || !be.isEmpty()) {
            for (int i = 0; i < AltarBlockEntity.SLOTS; i++) {
                ItemStack stack = be.getItem(i);
                if (stack.isEmpty()) continue;

                float[] base = AltarBlockEntity.SLOT_OFFSETS[i];

                // Phase 0: 待機
                if (!crafting) {
                    renderFloating(
                            itemRenderer, stack, pose, buffer, be,
                            base[0], base[1], base[2],
                            time, i, 1.0F, overlay
                    );
                    continue;
                }

                // Phase 1: 螺旋収束
                if (progress < 0.5F) {
                    float p = progress / 0.5F;
                    float eased = p * p;

                    double ox = base[0];
                    double oz = base[2];
                    double origAngle = Math.atan2(oz, ox);
                    double origRadius = Math.sqrt(ox * ox + oz * oz);
                    double spin = p * Math.PI * 4.0;
                    double newRadius = origRadius * (1.0 - eased);
                    double newAngle = origAngle + spin;
                    double nx = Math.cos(newAngle) * newRadius;
                    double nz = Math.sin(newAngle) * newRadius;
                    double ny = base[1] + (1.8 - base[1]) * eased;

                    renderFloating(
                            itemRenderer, stack, pose, buffer, be,
                            (float) nx, (float) ny, (float) nz,
                            time, i, 1.0F, overlay
                    );
                }
                // Phase 2: ぎゅっ
                else if (progress < 0.75F) {
                    float p = (progress - 0.5F) / 0.25F;
                    float scale = Math.max(0.0F, 1.0F - p);

                    renderFloating(
                            itemRenderer, stack, pose, buffer, be,
                            0.0F, 1.8F, 0.0F,
                            time, i, scale, overlay
                    );
                }
                // Phase 3 以降: 消える
            }
        }

        // =========================================================
        // 完成品の描画
        // =========================================================
        if (be.hasOutput()) {
            ItemStack output = be.peekOutput();

            if (crafting && progress >= 0.75F) {
                float p = (progress - 0.75F) / 0.25F;
                float scale = p;
                float spin = -(1.0F - p) * 720.0F;

                renderOutput(
                        itemRenderer, output, pose, buffer, be,
                        time, scale, spin, overlay
                );
            } else if (!crafting) {
                renderOutput(
                        itemRenderer, output, pose, buffer, be,
                        time, 1.0F, 0.0F, overlay
                );
            }
        }
    }

    private void renderFloating(
            ItemRenderer itemRenderer, ItemStack stack,
            PoseStack pose, MultiBufferSource buffer,
            AltarBlockEntity be,
            float ox, float oy, float oz,
            float time, int seed,
            float scale,
            int overlay
    ) {
        if (scale <= 0.001F) return;

        pose.pushPose();

        pose.translate(0.5 + ox, oy, 0.5 + oz);

        double hover = Math.sin((time + seed * 7.0) * 0.08) * 0.04;
        pose.translate(0, hover, 0);

        float spin = (time + seed * 3.0F) * 1.2F;
        pose.mulPose(Axis.YP.rotationDegrees(spin));

        pose.scale(scale, scale, scale);
        pose.scale(0.35F, 0.35F, 0.35F);

        itemRenderer.renderStatic(
                stack,
                ItemDisplayContext.GROUND,
                0xF000F0,
                overlay,
                pose,
                buffer,
                be.getLevel(),
                seed
        );

        pose.popPose();
    }

    private void renderOutput(
            ItemRenderer itemRenderer, ItemStack stack,
            PoseStack pose, MultiBufferSource buffer,
            AltarBlockEntity be,
            float time,
            float scale,
            float extraSpin,
            int overlay
    ) {
        if (scale <= 0.001F) return;

        pose.pushPose();

        pose.translate(0.5, 1.8, 0.5);

        double hover = Math.sin(time * 0.15) * 0.08;
        pose.translate(0, hover, 0);

        float spin = time * 1.0F + extraSpin;
        pose.mulPose(Axis.YP.rotationDegrees(spin));

        pose.scale(scale, scale, scale);
        pose.scale(0.9F, 0.9F, 0.9F);

        itemRenderer.renderStatic(
                stack,
                ItemDisplayContext.GROUND,
                0xF000F0,
                overlay,
                pose,
                buffer,
                be.getLevel(),
                0
        );

        pose.popPose();
    }
}