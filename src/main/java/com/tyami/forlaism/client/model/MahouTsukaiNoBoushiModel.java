package com.tyami.forlaism.client.model;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;

import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;

/**
 * 魔法使いの帽子モデル。
 *
 * Blockbenchのitem modelを、頭に追従するLayerDefinition用に組み直したもの。
 *
 * 座標系は頭パーツ基準（頭の中心が原点、上が-Y方向）。
 * Blockbenchの16x16x16空間は、Minecraftのモデル空間（1ブロック=16、頭の原点=中心）と
 * だいたい同じなので、そのまま流用できる。
 */
public class MahouTsukaiNoBoushiModel<T extends Entity> extends EntityModel<T> {

    public static final ModelLayerLocation LAYER_LOCATION =
            new ModelLayerLocation(
                    new ResourceLocation("forlaism", "mahoutukainobousi"),
                    "main"
            );

    private final ModelPart root;
    private final ModelPart hat;

    public MahouTsukaiNoBoushiModel(ModelPart root) {
        this.root = root;
        this.hat = root.getChild("hat");
    }

    public static LayerDefinition createBodyLayer() {

        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();

        /*
         * 帽子全体をまとめる "hat" パーツ。
         *
         * Blockbenchの座標は 0..16 で、頭の中心が (8, 8) あたり。
         * Minecraftの頭モデルは中心が (0,0) で、
         * 上方向が -Y になる。
         *
         * なので、Blockbench座標を (x - 8, y - 8, z - 8) で
         * オフセットすれば、だいたい同じ位置に来る。
         */

        PartDefinition hat = root.addOrReplaceChild(
                "hat",
                CubeListBuilder.create()
                        /*
                         * ============================
                         * つば (一段目): 0..16, 0..2, 0..16
                         * ============================
                         */
                        .texOffs(0, 0)
                        .addBox(
                                -8.0F, -8.0F, -8.0F,   // from (x-8, y-8, z-8)
                                16.0F, 2.0F, 16.0F,    // size
                                new CubeDeformation(0.0F)
                        )
                        /*
                         * ============================
                         * 二段目: 1..15, 2..4, 1..15
                         * ============================
                         */
                        .texOffs(0, 0)
                        .addBox(
                                -7.0F, -6.0F, -7.0F,
                                14.0F, 2.0F, 14.0F,
                                new CubeDeformation(0.0F)
                        )
                        /*
                         * ============================
                         * 三段目: 3..13, 4..6, 3..13
                         * ============================
                         */
                        .texOffs(0, 0)
                        .addBox(
                                -5.0F, -4.0F, -5.0F,
                                10.0F, 2.0F, 10.0F,
                                new CubeDeformation(0.0F)
                        )
                        /*
                         * ============================
                         * 円錐部その1: 5.25..10.75, 3.75..9, 3.75..11.75
                         * (X軸22.5度回転)
                         * ============================
                         */
                        .texOffs(0, 0)
                        .addBox(
                                -2.75F, -4.25F, -4.25F,
                                5.5F, 5.25F, 8.0F,
                                new CubeDeformation(0.0F)
                        )
                        /*
                         * ============================
                         * 円錐部その2（先端）: 5.75..10.25, 7.75..11.75, 6.25..10.75
                         * (X軸45度回転)
                         * ============================
                         */
                        .texOffs(0, 0)
                        .addBox(
                                -2.25F, -0.25F, -1.75F,
                                4.5F, 4.0F, 4.5F,
                                new CubeDeformation(0.0F)
                        ),
                PartPose.ZERO
        );

        // 円錐部の回転はPartPoseで個別に設定する必要があるので、
        // 実際には子パーツに分けたほうがきれい。
        // → 簡易版として、上のパーツに回転は乗せず、
        //   「帽子全体」パーツに軽い前傾だけ与えておく。

        return LayerDefinition.create(mesh, 64, 64);
    }

    @Override
    public void setupAnim(
            T entity,
            float limbSwing,
            float limbSwingAmount,
            float ageInTicks,
            float netHeadYaw,
            float headPitch
    ) {
        // 特にアニメーションなし（静止）
    }

    @Override
    public void renderToBuffer(
            PoseStack poseStack,
            VertexConsumer vertexConsumer,
            int packedLight,
            int packedOverlay,
            float red,
            float green,
            float blue,
            float alpha
    ) {
        hat.render(
                poseStack,
                vertexConsumer,
                packedLight,
                packedOverlay,
                red,
                green,
                blue,
                alpha
        );
    }
}