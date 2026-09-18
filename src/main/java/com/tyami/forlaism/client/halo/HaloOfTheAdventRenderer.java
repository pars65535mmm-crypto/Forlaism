package com.tyami.forlaism.client.halo;

import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.client.Minecraft;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

import top.theillusivec4.curios.api.SlotContext;
import top.theillusivec4.curios.api.client.ICurioRenderer;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class HaloOfTheAdventRenderer implements ICurioRenderer {

private static final ResourceLocation TEXTURE =
        new ResourceLocation(
                "forlaism",
                "textures/entity/halo_of_the_advent.png"
        );
/*
 * ==============================
 * 追従設定
 * ==============================
 */
// Haloが追従する速さ
private static final double FOLLOW_SPEED = 0.04D;
// プレイヤーから絶対に離れない最大距離
private static final double MAX_DISTANCE = 0.1D;
// 粒子を出す間隔
private static final int PARTICLE_INTERVAL = 2;
/*
 * エンティティごとのHalo状態
 */
private static final Map<UUID, HaloState> STATES = new HashMap<>();
private final HaloOfTheAdventModel<LivingEntity> model;
public HaloOfTheAdventRenderer(
        HaloOfTheAdventModel<LivingEntity> model) {
    this.model = model;
}
/*
 * Haloの追従状態
 */
private static class HaloState {
    // 前フレームのプレイヤー位置
    Vec3 previousEntityPosition;
    // プレイヤーの移動によって生じたHaloの遅れ
    Vec3 lagOffset = Vec3.ZERO;
    // 粒子カウンター
    int particleTimer = 0;
    HaloState(Vec3 initialPosition) {
        this.previousEntityPosition = initialPosition;
    }
}
@Override
public <T extends LivingEntity, M extends EntityModel<T>> void render(
        ItemStack stack,
        SlotContext slotContext,
        PoseStack poseStack,
        RenderLayerParent<T, M> renderLayerParent,
        MultiBufferSource renderTypeBuffer,
        int light,
        float limbSwing,
        float limbSwingAmount,
        float partialTicks,
        float ageInTicks,
        float netHeadYaw,
        float headPitch) {
    LivingEntity entity = slotContext.entity();
    if (entity == null) {
        return;
    }
    UUID uuid = entity.getUUID();
    HaloState state = STATES.computeIfAbsent(
            uuid,
            id -> new HaloState(entity.position())
    );
    /*
     * ========================================
     * 1. プレイヤーの移動量を取得
     * ========================================
     */
    Vec3 currentEntityPosition = entity.position();
    Vec3 movement =
            currentEntityPosition.subtract(
                    state.previousEntityPosition
            );
    state.previousEntityPosition = currentEntityPosition;
    /*
     * ========================================
     * 2. 移動した分だけHaloを置いていく
     * ========================================
     *
     * プレイヤーが右へ1m移動
     *
     * プレイヤー →
     *
     * Haloはその場に残ろうとする
     *        ◎
     *
     * その後、徐々に追いつく
     */
    state.lagOffset =
            state.lagOffset.subtract(movement);
    /*
     * ========================================
     * 3. Haloをプレイヤーへ戻す
     * ========================================
     */
    state.lagOffset =
            state.lagOffset.scale(
                    1.0D - FOLLOW_SPEED
            );
    /*
     * ========================================
     * 4. 最大1m制限
     * ========================================
     */
    double distance = state.lagOffset.length();
    if (distance > MAX_DISTANCE) {
        state.lagOffset =
                state.lagOffset
                        .normalize()
                        .scale(MAX_DISTANCE);
    }
    /*
     * ========================================
     * 5. 描画開始
     * ========================================
     */
    poseStack.pushPose();
    /*
     * プレイヤーの胴体に追従
     */
    if (renderLayerParent.getModel()
            instanceof HumanoidModel<?> humanoidModel) {
        humanoidModel.body.translateAndRotate(poseStack);
    }
    /*
     * 元々のHalo位置
     */
    poseStack.translate(
            0.0D,
            -1.5D,
            1.2D
    );
    /*
     * ========================================
     * 6. 遅延追従
     * ========================================
     *
     * movementをそのまま入れるのではなく、
     * lagOffsetを現在のモデル位置へ加える。
     */
    poseStack.translate(
            state.lagOffset.x,
            state.lagOffset.y,
            state.lagOffset.z
    );
    /*
     * ========================================
     * 7. 描画
     * ========================================
     */
    var vertexConsumer =
        renderTypeBuffer.getBuffer(
                RenderType.entityTranslucentEmissive(TEXTURE)
        );

    model.setupAnim(
        entity,
        limbSwing,
        limbSwingAmount,
        ageInTicks,
        netHeadYaw,
        headPitch
    );
    model.renderToBuffer(
            poseStack,
            vertexConsumer,
            light,
            OverlayTexture.NO_OVERLAY,
            1.0F,
            1.0F,
            1.0F,
            1.0F
    );
    poseStack.popPose();
    /*
     * ========================================
     * 8. 粒子
     * ========================================
     */
    spawnTrailParticles(entity, state, partialTicks);
}
/*
 * ============================================
 * Haloの軌跡粒子
 * ============================================
 */
private static void spawnTrailParticles(
        LivingEntity entity,
        HaloState state,
        float partialTicks) {
    Minecraft minecraft = Minecraft.getInstance();
    if (minecraft.level == null) {
        return;
    }
    /*
     * 描画1回ごとに呼ばれるので、
     * 毎回粒子を出さない。
     */
    state.particleTimer++;
    if (state.particleTimer < PARTICLE_INTERVAL) {
        return;
    }
    state.particleTimer = 0;
    /*
     * Haloのおおよそのワールド位置。
     *
     * プレイヤーの胴体～頭付近。
     */
    Vec3 basePosition =
            entity.position()
                    .add(
                            0.0D,
                            entity.getBbHeight() * 0.72D,
                            0.0D
                    );
    /*
     * 遅れている分を粒子位置にも反映。
     */
    Vec3 particlePosition =
            basePosition.add(state.lagOffset);
    /*
     * ランダムな微細な揺らぎ
     */
    double offsetX =
            (entity.getRandom().nextDouble() - 0.5D) * 0.25D;
    double offsetY =
            (entity.getRandom().nextDouble() - 0.5D) * 0.25D;
    double offsetZ =
            (entity.getRandom().nextDouble() - 0.5D) * 0.25D;
    particlePosition =
            particlePosition.add(
                    offsetX,
                    offsetY,
                    offsetZ
            );
    /*
     * エンチャント風の光粒子。
     *
     * まずは標準粒子を使う。
     * 後で専用粒子に変更可能。
     */
    minecraft.level.addParticle(
            ParticleTypes.END_ROD,
            particlePosition.x,
            particlePosition.y,
            particlePosition.z,
            0.0D,
            0.015D,
            0.0D
    );
    /*
     * たまに追加の粒子。
     */
    if (entity.getRandom().nextFloat() < 0.35F) {
        minecraft.level.addParticle(
                ParticleTypes.ENCHANT,
                particlePosition.x,
                particlePosition.y,
                particlePosition.z,
                0.0D,
                0.02D,
                0.0D
        );
    }
}

}