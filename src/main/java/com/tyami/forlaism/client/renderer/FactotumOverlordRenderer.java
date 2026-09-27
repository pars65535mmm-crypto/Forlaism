package com.tyami.forlaism.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.tyami.forlaism.entity.FactotumOverlordEntity;
import com.tyami.forlaism.registry.Blocks;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.client.model.data.ModelData;

/**
 * Factotum Overlord のレンダラー。
 *
 * モデルを使わず、MadoromuBlock を正六面体状に複数配置して描画する。
 */
public class FactotumOverlordRenderer extends EntityRenderer<FactotumOverlordEntity> {

    private static final BlockState BLOCK_STATE =
            Blocks.MADOROMU_BLOCK.get().defaultBlockState();

    public FactotumOverlordRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.shadowRadius = 1.5F;
    }

    @Override
    public ResourceLocation getTextureLocation(FactotumOverlordEntity entity) {
        return InventoryMenu.BLOCK_ATLAS;
    }

    @Override
    public void render(
            FactotumOverlordEntity entity,
            float entityYaw,
            float partialTick,
            PoseStack poseStack,
            MultiBufferSource buffer,
            int packedLight
    ) {
        super.render(entity, entityYaw, partialTick, poseStack, buffer, packedLight);

        poseStack.pushPose();

        // フワフワ浮遊
        float hover = (float) Math.sin((entity.tickCount + partialTick) * 0.08F) * 0.3F;
        poseStack.translate(0.0D, 1.5D + hover, 0.0D);

        // 回転（起動中は速く）
        float spin = entity.isActivated()
                ? (entity.tickCount + partialTick) * 4.0F
                : (entity.tickCount + partialTick) * 0.5F;
        poseStack.mulPose(Axis.YP.rotationDegrees(spin));

        // =========================================================
        // 正六面体状に MadoromuBlock を配置
        // =========================================================
        // 3×3×3 = 27個（ただし中空）
        renderHollowCube(poseStack, buffer, packedLight, 1.5F);

        poseStack.popPose();
    }

    /**
     * 中空の立方体状にブロックを描画。
     *
     * size は一辺の半分（= 半径）。
     */
    private void renderHollowCube(
            PoseStack poseStack,
            MultiBufferSource buffer,
            int packedLight,
            float size
    ) {
        BlockRenderDispatcher dispatcher =
                Minecraft.getInstance().getBlockRenderer();

        // 各面に 3×3 のブロックを配置
        // 位置は -size, 0, +size の3段階
        float[] offsets = {-size, 0.0F, size};

        // 外殻のみ（中空）
        for (float x : offsets) {
            for (float y : offsets) {
                for (float z : offsets) {

                    // 完全に内部（0,0,0）はスキップ
                    if (x == 0.0F && y == 0.0F && z == 0.0F) continue;

                    // 面の内部（軸1つだけ0の位置は中空なので除外）
                    // → 全部の外殻だけ描画するため、既に上で全パターン取得済み

                    poseStack.pushPose();
                    poseStack.translate(x - 0.5F, y - 0.5F, z - 0.5F);

                    dispatcher.renderSingleBlock(
                            BLOCK_STATE,
                            poseStack,
                            buffer,
                            packedLight,
                            OverlayTexture.NO_OVERLAY,
                            ModelData.EMPTY,
                            null
                    );

                    poseStack.popPose();
                }
            }
        }
    }
}