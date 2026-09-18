package com.tyami.forlaism.client.halo;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;

import net.minecraft.client.animation.KeyframeAnimations;
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

import org.joml.Vector3f;

public class HaloOfTheFirmament<T extends Entity> extends HierarchicalModel<T> {

    /*
     * KeyframeAnimations.animate() に渡す一時Vector。
     *
     * HierarchicalModel.ANIMATION_VECTOR_CACHE は private なので、
     * ここでは自前の Vector3f を使用する。
     */
    private static final Vector3f ANIMATION_VECTOR = new Vector3f();

    public static final ModelLayerLocation LAYER_LOCATION =
            new ModelLayerLocation(
                    new ResourceLocation(
                            "forlaism",
                            "halo_of_the_firmament"
                    ),
                    "main"
            );

    private final ModelPart root;
    private final ModelPart siroiyatu;
    private final ModelPart bone;
    private final ModelPart gaienn;

    public HaloOfTheFirmament(ModelPart root) {
        this.root = root;

        this.siroiyatu = root.getChild("siroiyatu");
        this.bone = root.getChild("bone");
        this.gaienn = root.getChild("gaienn");
    }

    public static LayerDefinition createBodyLayer() {

        MeshDefinition meshdefinition = new MeshDefinition();
        PartDefinition partdefinition = meshdefinition.getRoot();

        /*
         * siroiyatu
         */
        PartDefinition siroiyatu =
                partdefinition.addOrReplaceChild(
                        "siroiyatu",
                        CubeListBuilder.create(),
                        PartPose.offset(
                                0.0F,
                                24.0F,
                                0.0F
                        )
                );

        siroiyatu.addOrReplaceChild(
                "cube_r1",
                CubeListBuilder.create()
                        .texOffs(0, 37)
                        .addBox(
                                -8.0F,
                                -9.0F,
                                -1.0F,
                                17.0F,
                                17.0F,
                                0.0F,
                                new CubeDeformation(0.0F)
                        ),
                PartPose.offsetAndRotation(
                        -0.5F,
                        1.0F,
                        -0.5F,
                        -1.5708F,
                        0.0F,
                        0.0F
                )
        );

        /*
         * bone
         */
        PartDefinition bone =
                partdefinition.addOrReplaceChild(
                        "bone",
                        CubeListBuilder.create(),
                        PartPose.offset(
                                0.0F,
                                24.0F,
                                0.0F
                        )
                );

        bone.addOrReplaceChild(
                "cube_r2",
                CubeListBuilder.create()
                        .texOffs(34, 37)
                        .addBox(
                                -8.5F,
                                -8.5F,
                                -2.0F,
                                17.0F,
                                17.0F,
                                0.0F,
                                new CubeDeformation(0.0F)
                        ),
                PartPose.offsetAndRotation(
                        0.0F,
                        0.75F,
                        0.0F,
                        1.5708F,
                        0.0F,
                        -3.1416F
                )
        );

        /*
         * gaienn
         */
        PartDefinition gaienn =
                partdefinition.addOrReplaceChild(
                        "gaienn",
                        CubeListBuilder.create()

                                /*
                                 * 大きな薄い円盤
                                 */
                                .texOffs(0, 0)
                                .addBox(
                                        -18.5F,
                                        0.0F,
                                        -18.5F,
                                        37.0F,
                                        0.0F,
                                        37.0F,
                                        new CubeDeformation(0.0F)
                                )

                                /*
                                 * 南側
                                 */
                                .texOffs(-6, -3)
                                .addBox(
                                        -2.5F,
                                        -2.5F,
                                        13.5F,
                                        5.0F,
                                        5.0F,
                                        5.0F,
                                        new CubeDeformation(0.0F)
                                )

                                /*
                                 * 西側
                                 */
                                .texOffs(-6, -3)
                                .addBox(
                                        -18.5F,
                                        -2.5F,
                                        -2.5F,
                                        5.0F,
                                        5.0F,
                                        5.0F,
                                        new CubeDeformation(0.0F)
                                )

                                /*
                                 * 北側
                                 */
                                .texOffs(-6, -3)
                                .addBox(
                                        -2.5F,
                                        -2.5F,
                                        -18.5F,
                                        5.0F,
                                        5.0F,
                                        5.0F,
                                        new CubeDeformation(0.0F)
                                )

                                /*
                                 * 東側
                                 */
                                .texOffs(-6, -3)
                                .addBox(
                                        13.5F,
                                        -2.5F,
                                        -2.5F,
                                        5.0F,
                                        5.0F,
                                        5.0F,
                                        new CubeDeformation(0.0F)
                                ),

                        PartPose.offset(
                                0.0F,
                                24.75F,
                                0.0F
                        )
                );

        return LayerDefinition.create(
                meshdefinition,
                256,
                256
        );
    }

    /*
     * HierarchicalModel が要求するルート。
     */
    @Override
    public ModelPart root() {
        return root;
    }

    /*
     * アニメーション処理。
     */
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
         * 前フレームの変形をリセット。
         */
        root.getAllParts().forEach(ModelPart::resetPose);

        /*
         * Blockbenchから生成したアニメーションを再生。
         *
         * ageInTicks はtick単位なので、
         * 50ms/tickとしてミリ秒へ変換。
         */
        KeyframeAnimations.animate(
                this,
                HaloOfTheFirmamentAnimation.animation,
                (long) (ageInTicks * 50.0F),
                1.0F,
                ANIMATION_VECTOR
        );
    }

    /*
     * モデル描画。
     */
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

        siroiyatu.render(
                poseStack,
                vertexConsumer,
                packedLight,
                packedOverlay,
                red,
                green,
                blue,
                alpha
        );

        bone.render(
                poseStack,
                vertexConsumer,
                packedLight,
                packedOverlay,
                red,
                green,
                blue,
                alpha
        );

        gaienn.render(
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