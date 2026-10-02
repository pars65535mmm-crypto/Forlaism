package com.tyami.forlaism.client.model;

import com.tyami.forlaism.Forlaism;
import com.tyami.forlaism.client.animation.EndWardenAnimation;
import com.tyami.forlaism.entity.EndWardenAttackPhase;
import com.tyami.forlaism.entity.EndWardenEntity;

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

/**
 * EndWarden のモデル。
 *
 * Blockbench から出力された end_warden.java を
 * HierarchicalModel<EndWardenEntity> に組み替えたもの。
 *
 * アニメーションは EndWardenAnimation 側で定義。
 */
public class EndWardenModel<T extends Entity> extends HierarchicalModel<T> {

    /** レイヤー定義。Forlaism.ClientModEvents で registerLayerDefinition する。 */
    public static final ModelLayerLocation LAYER_LOCATION =
            new ModelLayerLocation(
                    new ResourceLocation(Forlaism.MOD_ID, "end_warden"),
                    "main"
            );

    private final ModelPart root;
    private final ModelPart head;
    private final ModelPart torso;
    private final ModelPart leftArm;
    private final ModelPart rightArm;
    private final ModelPart leftLeg;
    private final ModelPart rightLeg;
    private final ModelPart hidarihane;
    private final ModelPart migihhane;
    private final ModelPart kurodama;

    public EndWardenModel(ModelPart root) {
        this.root = root;
        this.head = root.getChild("head");
        this.torso = root.getChild("torso");
        this.leftArm = root.getChild("left_arm");
        this.rightArm = root.getChild("right_arm");
        this.leftLeg = root.getChild("left_leg");
        this.rightLeg = root.getChild("right_leg");
        ModelPart hane = root.getChild("hane");
        this.hidarihane = hane.getChild("hidarihane");
        this.migihhane = hane.getChild("migihhane");
        this.kurodama = root.getChild("kurodama");
    }

    // =========================================================
    // LayerDefinition
    // =========================================================

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();

        PartDefinition head = root.addOrReplaceChild(
                "head",
                CubeListBuilder.create().texOffs(0, 32)
                        .addBox(-8.0F, -16.0F, -5.0F, 16.0F, 16.0F, 10.0F, new CubeDeformation(0.0F)),
                PartPose.offset(0.0F, -10.0F, -1.0F)
        );

        PartDefinition leftTendril = root.addOrReplaceChild(
                "left_tendril",
                CubeListBuilder.create().texOffs(91, 19)
                        .addBox(-3.0F, -10.0F, -2.0F, 3.0F, 9.0F, 3.0F, new CubeDeformation(0.0F)),
                PartPose.offset(8.0F, -22.0F, 0.0F)
        );
        leftTendril.addOrReplaceChild(
                "left_tendril_r1",
                CubeListBuilder.create().texOffs(58, 0)
                        .addBox(8.0F, -59.0F, 0.0F, 16.0F, 16.0F, 0.0F, new CubeDeformation(0.0F)),
                PartPose.offsetAndRotation(-9.0F, 45.0F, 0.0F, 0.0F, 0.0F, 0.0436F)
        );

        PartDefinition rightTendril = root.addOrReplaceChild(
                "right_tendril",
                CubeListBuilder.create().texOffs(88, 18)
                        .addBox(0.0F, -10.0F, -2.0F, 3.0F, 9.0F, 3.0F, new CubeDeformation(0.0F)),
                PartPose.offset(-8.0F, -22.0F, 0.0F)
        );
        rightTendril.addOrReplaceChild(
                "right_tendril_r1",
                CubeListBuilder.create().texOffs(58, 3)
                        .addBox(-24.0F, -59.0F, 0.0F, 16.0F, 16.0F, 0.0F, new CubeDeformation(0.0F)),
                PartPose.offsetAndRotation(-27.0F, 47.1F, 0.0F, -3.1397F, 0.0436F, -3.0979F)
        );

        root.addOrReplaceChild(
                "torso",
                CubeListBuilder.create().texOffs(0, 0)
                        .addBox(-9.0F, -13.0F, -4.0F, 18.0F, 21.0F, 11.0F, new CubeDeformation(0.0F)),
                PartPose.offset(0.0F, 3.0F, -1.0F)
        );

        root.addOrReplaceChild(
                "left_ribcage",
                CubeListBuilder.create().texOffs(10, 11).mirror()
                        .addBox(-7.0F, -11.0F, -0.1F, 9.0F, 21.0F, 0.0F, new CubeDeformation(0.0F)).mirror(false),
                PartPose.offset(7.0F, 1.0F, -5.0F)
        );

        root.addOrReplaceChild(
                "right_ribcage",
                CubeListBuilder.create().texOffs(12, 10)
                        .addBox(-2.0F, -11.0F, -0.1F, 9.0F, 21.0F, 0.0F, new CubeDeformation(0.0F)),
                PartPose.offset(-7.0F, 1.0F, -5.0F)
        );

        root.addOrReplaceChild(
                "left_arm",
                CubeListBuilder.create().texOffs(44, 51)
                        .addBox(-4.0F, 0.0F, -4.0F, 8.0F, 28.0F, 8.0F, new CubeDeformation(0.0F)),
                PartPose.offset(13.0F, -10.0F, 0.0F)
        );

        root.addOrReplaceChild(
                "right_arm",
                CubeListBuilder.create().texOffs(0, 58)
                        .addBox(-4.0F, 0.0F, -4.0F, 8.0F, 28.0F, 8.0F, new CubeDeformation(0.0F)),
                PartPose.offset(-13.0F, -10.0F, 0.0F)
        );

        root.addOrReplaceChild(
                "left_leg",
                CubeListBuilder.create().texOffs(75, 77)
                        .addBox(-2.9F, 0.0F, -3.0F, 6.0F, 13.0F, 6.0F, new CubeDeformation(0.0F)),
                PartPose.offset(6.0F, 11.0F, -1.0F)
        );

        root.addOrReplaceChild(
                "right_leg",
                CubeListBuilder.create().texOffs(75, 48)
                        .addBox(-3.1F, 0.0F, -3.0F, 6.0F, 13.0F, 6.0F, new CubeDeformation(0.0F)),
                PartPose.offset(-6.0F, 11.0F, -1.0F)
        );

        PartDefinition hane = root.addOrReplaceChild(
                "hane",
                CubeListBuilder.create(),
                PartPose.offset(0.0F, 24.0F, 0.0F)
        );

        PartDefinition hidarihane = hane.addOrReplaceChild(
                "hidarihane",
                CubeListBuilder.create().texOffs(31, 57)
                        .addBox(-1.0F, -3.9F, -1.0F, 2.0F, 6.0F, 3.0F, new CubeDeformation(0.0F)),
                PartPose.offset(-7.0F, -23.0F, 8.0F)
        );
        hidarihane.addOrReplaceChild(
                "cube_r1",
                CubeListBuilder.create().texOffs(15, 57)
                        .addBox(-17.0F, -6.0F, -1.0F, 17.0F, 14.0F, 3.0F, new CubeDeformation(0.0F)),
                PartPose.offsetAndRotation(-18.8F, -11.5F, 0.0F, 0.0F, 0.0F, 0.0262F)
        );
        hidarihane.addOrReplaceChild(
                "cube_r2",
                CubeListBuilder.create().texOffs(14, 56)
                        .addBox(-17.0F, -10.0F, -2.0F, 26.0F, 18.0F, 4.0F, new CubeDeformation(0.0F)),
                PartPose.offsetAndRotation(-8.6F, -2.4F, 0.0F, 0.0F, 0.0F, 0.3927F)
        );

        PartDefinition migihhane = hane.addOrReplaceChild(
                "migihhane",
                CubeListBuilder.create().texOffs(31, 57)
                        .addBox(-3.0F, -4.0F, -1.0F, 2.0F, 6.0F, 3.0F, new CubeDeformation(0.0F)),
                PartPose.offset(6.0F, -23.0F, 8.0F)
        );
        migihhane.addOrReplaceChild(
                "cube_r3",
                CubeListBuilder.create().texOffs(15, 57)
                        .addBox(-17.0F, -6.0F, -1.0F, 17.0F, 14.0F, 3.0F, new CubeDeformation(0.0F)),
                PartPose.offsetAndRotation(36.2F, -10.2F, 0.0F, 0.0F, 0.0F, 0.0262F)
        );
        migihhane.addOrReplaceChild(
                "cube_r4",
                CubeListBuilder.create().texOffs(13, 55)
                        .addBox(-17.0F, -10.0F, -3.0F, 26.0F, 18.0F, 5.0F, new CubeDeformation(0.0F)),
                PartPose.offsetAndRotation(14.5F, -4.6F, 0.0F, 0.0F, 0.0F, -0.3054F)
        );

        root.addOrReplaceChild(
                "kurodama",
                CubeListBuilder.create().texOffs(0, 61)
                        .addBox(-5.1F, -4.1F, -6.4F, 8.0F, 8.0F, 8.0F, new CubeDeformation(0.0F)),
                PartPose.offsetAndRotation(0.0F, -1.0F, 9.0F, 0.0436F, 0.0F, 0.0F)
        );

        return LayerDefinition.create(mesh, 128, 128);
    }

    // =========================================================
    // root
    // =========================================================

    @Override
    public ModelPart root() {
        return this.root;
    }

    // =========================================================
    // アニメーション
    // =========================================================

    @Override
    public void setupAnim(
            T entity,
            float limbSwing,
            float limbSwingAmount,
            float ageInTicks,
            float netHeadYaw,
            float headPitch
    ) {
        // 全パーツをリセット
        this.root.getAllParts().forEach(ModelPart::resetPose);

        // エンティティが EndWardenEntity の場合のみアニメ再生
        if (!(entity instanceof EndWardenEntity warden)) {
            return;
        }

        EndWardenAttackPhase phase = warden.getPhase();

        // フェーズに応じたアニメを再生
        switch (phase) {
            case SONIC_BOOM ->
                    this.animate(warden.sonicBoomAnimationState, EndWardenAnimation.SONIC_BOOM, ageInTicks);
            case MELEE ->
                    this.animate(warden.attackAnimationState, EndWardenAnimation.ATTACK, ageInTicks);
            case WALK, DIVE ->
                    this.animate(warden.walkAnimationState, EndWardenAnimation.WALK, ageInTicks);
            default ->
                    this.animate(warden.idleAnimationState, EndWardenAnimation.IDLE, ageInTicks);
        }

        // 頭の向き（歩行時の首振り）
        this.head.yRot = netHeadYaw * ((float) Math.PI / 180.0F);
        this.head.xRot = headPitch * ((float) Math.PI / 180.0F);
    }

    // =========================================================
    // 描画
    // =========================================================

    @Override
    public void renderToBuffer(
            com.mojang.blaze3d.vertex.PoseStack poseStack,
            com.mojang.blaze3d.vertex.VertexConsumer vertexConsumer,
            int packedLight,
            int packedOverlay,
            float red,
            float green,
            float blue,
            float alpha
    ) {
        this.root.render(poseStack, vertexConsumer, packedLight, packedOverlay, red, green, blue, alpha);
    }
}