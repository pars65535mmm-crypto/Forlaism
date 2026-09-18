package com.tyami.forlaism.client.halo;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;

import net.minecraft.client.model.HierarchicalModel;
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

public class HaloOfTheAdventModel<T extends Entity>
        extends HierarchicalModel<T> {

    public static final ModelLayerLocation LAYER_LOCATION =
            new ModelLayerLocation(
                    new ResourceLocation(
                            "forlaism",
                            "halo_of_the_advent"
                    ),
                    "main"
            );

    private final ModelPart root;
    private final ModelPart group2;
    private final ModelPart group;
    private final ModelPart sikaku2;

    public HaloOfTheAdventModel(ModelPart root) {
        this.root = root;

        this.group2 = root.getChild("group2");
        this.group = root.getChild("group");
        this.sikaku2 = root.getChild("sikaku2");
    }

    @Override
    public ModelPart root() {
        return root;
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

        /*
         * ========================================
         * 毎フレーム初期姿勢へ戻す
         * ========================================
         */
        root.resetPose();

        /*
         * ========================================
         * 時間
         *
         * 20 tick = 1秒
         * 80 tick = 4秒
         * ========================================
         */
        float time = ageInTicks % 80.0F;

        /*
         * 0.0 ～ 1.0
         */
        float progress = time / 80.0F;

        /*
         * ========================================
         * group2
         *
         * 4秒で -90°
         * ========================================
         */
        group2.zRot =
                -progress * (float) (Math.PI / 2.0D);

        /*
         * ========================================
         * group
         *
         * 4秒で -360°
         * ========================================
         */
        group.zRot =
                -progress * (float) (Math.PI * 2.0D);

        /*
         * ========================================
         * sikaku2
         *
         * 4秒で +360°
         * ========================================
         */
        sikaku2.zRot =
                progress * (float) (Math.PI * 2.0D);
    }

    public static LayerDefinition createBodyLayer() {

        MeshDefinition meshdefinition =
                new MeshDefinition();

        PartDefinition partdefinition =
                meshdefinition.getRoot();

        /*
         * ==============================
         * group2
         * ==============================
         */
        PartDefinition group2 =
                partdefinition.addOrReplaceChild(
                        "group2",
                        CubeListBuilder.create()
                                .texOffs(0, 0)
                                .addBox(
                                        -15.0F,
                                        -14.25F,
                                        -0.25F,
                                        30.0F,
                                        30.0F,
                                        0.0F,
                                        new CubeDeformation(0.0F)
                                ),
                        PartPose.offset(
                                0.0F,
                                22.25F,
                                -7.75F
                        )
                );

        group2.addOrReplaceChild(
                "cube_r1",
                CubeListBuilder.create()
                        .texOffs(0, 0)
                        .addBox(
                                -16.0F,
                                -16.0F,
                                0.0F,
                                30.0F,
                                30.0F,
                                0.0F,
                                new CubeDeformation(0.0F)
                        ),
                PartPose.offsetAndRotation(
                        1.0F,
                        1.75F,
                        -1.25F,
                        0.0F,
                        0.0F,
                        -0.7854F
                )
        );

        /*
         * ==============================
         * group
         * ==============================
         */
        PartDefinition group =
                partdefinition.addOrReplaceChild(
                        "group",
                        CubeListBuilder.create(),
                        PartPose.offset(
                                0.25F,
                                22.25F,
                                -11.0F
                        )
                );

        group.addOrReplaceChild(
                "cube_r2",
                CubeListBuilder.create()
                        .texOffs(0, 30)
                        .mirror()
                        .addBox(
                                -15.5F,
                                -15.5F,
                                0.0F,
                                31.0F,
                                31.0F,
                                0.0F,
                                new CubeDeformation(0.0F)
                        )
                        .mirror(false),
                PartPose.offsetAndRotation(
                        -0.25F,
                        0.0F,
                        0.25F,
                        0.0F,
                        0.0F,
                        -0.7854F
                )
        );

        /*
         * ==============================
         * sikaku2
         * ==============================
         */
        PartDefinition sikaku2 =
                partdefinition.addOrReplaceChild(
                        "sikaku2",
                        CubeListBuilder.create(),
                        PartPose.offset(
                                0.0F,
                                22.5F,
                                -10.25F
                        )
                );

        sikaku2.addOrReplaceChild(
                "cube_r3",
                CubeListBuilder.create()
                        .texOffs(0, 0)
                        .addBox(
                                -14.5F,
                                -14.5F,
                                0.0F,
                                29.0F,
                                29.0F,
                                0.0F,
                                new CubeDeformation(0.0F)
                        ),
                PartPose.offsetAndRotation(
                        0.0F,
                        -0.25F,
                        0.25F,
                        0.0F,
                        0.0F,
                        -0.7854F
                )
        );

        return LayerDefinition.create(
                meshdefinition,
                64,
                64
        );
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

        root.render(
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