package com.tyami.forlaism.client.model;

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

/**
 * 投げナイフのモデル。
 *
 * Blockbench 5.2.1 出力を HierarchicalModel<Entity> に組み替え。
 * パッケージとレイヤー名だけ調整。
 */
public class IinekoreKnifeModel<T extends Entity> extends HierarchicalModel<T> {

    public static final ModelLayerLocation LAYER_LOCATION =
            new ModelLayerLocation(
                    new ResourceLocation("forlaism", "iinekore_knife"),
                    "main"
            );

    private final ModelPart root;
    private final ModelPart bbMain;

    public IinekoreKnifeModel(ModelPart root) {
        this.root = root;
        this.bbMain = root.getChild("bb_main");
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition meshdefinition = new MeshDefinition();
        PartDefinition partdefinition = meshdefinition.getRoot();

        PartDefinition bbMain = partdefinition.addOrReplaceChild(
                "bb_main",
                CubeListBuilder.create(),
                PartPose.offset(0.0F, 24.0F, 0.0F)
        );

        bbMain.addOrReplaceChild(
                "cube_r1",
                CubeListBuilder.create()
                        .texOffs(-12, -12)
                        .addBox(-1.0F, -7.0F, -8.0F, 0.0F, 14.0F, 14.0F,
                                new CubeDeformation(0.0F)),
                PartPose.offsetAndRotation(
                        0.5F, 0.0F, 0.0F,
                        -0.7854F, 0.0F, 0.0F
                )
        );

        return LayerDefinition.create(meshdefinition, 16, 16);
    }

    @Override
    public ModelPart root() {
        return root;
    }

    @Override
    public void setupAnim(T entity, float limbSwing, float limbSwingAmount,
                          float ageInTicks, float netHeadYaw, float headPitch) {
        // 回転は Renderer 側で制御するので何もしない
    }

    @Override
    public void renderToBuffer(PoseStack poseStack, VertexConsumer vertexConsumer,
                               int packedLight, int packedOverlay,
                               float red, float green, float blue, float alpha) {
        bbMain.render(poseStack, vertexConsumer, packedLight, packedOverlay,
                red, green, blue, alpha);
    }
}