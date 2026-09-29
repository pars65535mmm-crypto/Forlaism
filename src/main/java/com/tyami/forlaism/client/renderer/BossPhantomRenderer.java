package com.tyami.forlaism.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.tyami.forlaism.Forlaism;
import com.tyami.forlaism.client.ClientBossCache;
import com.tyami.forlaism.network.BossSyncPacket;

import net.minecraft.client.Minecraft;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import org.joml.Quaternionf;

import java.util.HashMap;
import java.util.Map;

@Mod.EventBusSubscriber(modid = Forlaism.MOD_ID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class BossPhantomRenderer {

    private static final Map<String, ResourceLocation> SKIN_CACHE = new HashMap<>();
    private static PlayerModel<LivingEntity> playerModel;

    /** PlayerModel はブロック単位で組まれてるので、スケールは1.0でOK。 */
    private static final float MODEL_SCALE = 1.0F;

    private BossPhantomRenderer() {
    }

    private static ResourceLocation skinRL(String skinId) {
        return SKIN_CACHE.computeIfAbsent(skinId, id ->
                new ResourceLocation(Forlaism.MOD_ID, "textures/entity/" + id + ".png")
        );
    }

    private static PlayerModel<LivingEntity> getModel() {
        if (playerModel == null) {
            Minecraft mc = Minecraft.getInstance();
            playerModel = new PlayerModel<>(
                    mc.getEntityModels().bakeLayer(ModelLayers.PLAYER),
                    false
            );
        }
        return playerModel;
    }

    @SubscribeEvent
    public static void onRenderLevel(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_ENTITIES) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null) return;

        var entries = ClientBossCache.snapshot();
        if (entries.isEmpty()) return;

        var cam = event.getCamera();
        var camPos = cam.getPosition();

        PoseStack pose = event.getPoseStack();
        MultiBufferSource.BufferSource buffer = mc.renderBuffers().bufferSource();

        PlayerModel<LivingEntity> model = getModel();

        // 子供モデル化を防ぐ
        model.young = false;

        for (BossSyncPacket.Entry e : entries) {
            double dx = e.x - camPos.x;
            double dy = e.y - camPos.y;
            double dz = e.z - camPos.z;

            if (dx * dx + dy * dy + dz * dz > 128.0D * 128.0D) continue;

            pose.pushPose();

            // 1. ボスのワールド座標へ
            pose.translate(dx, dy, dz);

            // 2. yaw で回転
            pose.mulPose(new Quaternionf().rotateY((float) Math.toRadians(-e.yaw)));

            // 3. Y反転（GUI座標系 → ワールド座標系）
            //    PlayerModel は上方向が -Y で組まれてるので、反転が必要
            pose.scale(1.0F, -1.0F, 1.0F);

            // 4. モデルスケール（ブロック単位）
            pose.scale(MODEL_SCALE, MODEL_SCALE, MODEL_SCALE);

            // 5. 足元合わせ: PlayerModel の原点は「足元から 1.5ブロック上」なので、
            //    足元に来るように +1.5 持ち上げる（Y反転してるので、実質 -1.5 に相当）
            //    → 反転後は Y方向が逆なので、+1.5 で足元へ
            pose.translate(0.0D, 0.0D, 0.0D);

            model.setupAnim(mc.player, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F);

            ResourceLocation skin = skinRL(e.skinId);
            var vc = buffer.getBuffer(RenderType.entityTranslucent(skin));

            model.renderToBuffer(
                    pose,
                    vc,
                    LightTexture.FULL_BRIGHT,
                    OverlayTexture.NO_OVERLAY,
                    1.0F, 1.0F, 1.0F, 1.0F
            );

            pose.popPose();
        }

        buffer.endBatch();
    }
}