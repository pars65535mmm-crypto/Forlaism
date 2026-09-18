package com.tyami.forlaism.client.renderer;

import com.mojang.authlib.GameProfile;
import com.mojang.authlib.minecraft.MinecraftProfileTexture;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.tyami.forlaism.entity.FactotumPhantomEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.DefaultPlayerSkin;
import net.minecraft.client.resources.SkinManager;
import net.minecraft.resources.ResourceLocation;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class FactotumPhantomRenderer extends LivingEntityRenderer<FactotumPhantomEntity, PlayerModel<FactotumPhantomEntity>> {

    private static final Map<UUID, ResourceLocation> SKIN_CACHE = new ConcurrentHashMap<>();

    public FactotumPhantomRenderer(EntityRendererProvider.Context context) {
        super(context, new PlayerModel<>(context.bakeLayer(ModelLayers.PLAYER), false), 0.0F);
    }

    @Override
    public ResourceLocation getTextureLocation(FactotumPhantomEntity entity) {
        return entity.getOwnerUUID().map(uuid -> {
            if (SKIN_CACHE.containsKey(uuid)) {
                return SKIN_CACHE.get(uuid);
            }

            Minecraft minecraft = Minecraft.getInstance();
            SkinManager skinManager = minecraft.getSkinManager();
            GameProfile profile = new GameProfile(uuid, null);

            skinManager.registerSkins(profile, (type, location, profileTexture) -> {
                if (type == MinecraftProfileTexture.Type.SKIN) {
                    SKIN_CACHE.put(uuid, location);
                }
            }, true);

            ResourceLocation fallback = DefaultPlayerSkin.getDefaultSkin(uuid);
            SKIN_CACHE.put(uuid, fallback);
            return fallback;
        }).orElse(DefaultPlayerSkin.getDefaultSkin());
    }

    @Override
    protected RenderType getRenderType(FactotumPhantomEntity entity, boolean bodyVisible, boolean translucent, boolean appearsGlowing) {
        return RenderType.entityTranslucent(getTextureLocation(entity));
    }

    @Override
    public void render(FactotumPhantomEntity entity, float entityYaw, float partialTicks,
                       PoseStack poseStack, MultiBufferSource bufferSource, int packedLight) {
        // 透明度30% (alpha = 0.3F) で描画
        poseStack.pushPose();

        this.model.attackTime = this.getAttackAnim(entity, partialTicks);
        this.model.riding = entity.isPassenger();
        this.model.young = entity.isBaby();

        float bodyRot = entity.yBodyRotO + (entity.yBodyRot - entity.yBodyRotO) * partialTicks;
        float headRot = entity.yHeadRotO + (entity.yHeadRot - entity.yHeadRotO) * partialTicks;
        float netHeadYaw = headRot - bodyRot;
        float headPitch = entity.xRotO + (entity.getXRot() - entity.xRotO) * partialTicks;

        this.setupRotations(entity, poseStack, entity.tickCount + partialTicks, bodyRot, partialTicks);
        this.scale(entity, poseStack, partialTicks);

        float walkAnimSpeed = entity.walkAnimation.speed(partialTicks);
        float walkAnimPos = entity.walkAnimation.position(partialTicks);

        this.model.setupAnim(entity, walkAnimPos, walkAnimSpeed, entity.tickCount + partialTicks, netHeadYaw, headPitch);

        RenderType renderType = RenderType.entityTranslucent(getTextureLocation(entity));
        VertexConsumer vertexConsumer = bufferSource.getBuffer(renderType);

        // 赤・オレンジ・白・水色のわずかなオーラ色（薄い水色白）にアルファ0.30F
        float red = 0.85F;
        float green = 0.95F;
        float blue = 1.0F;
        float alpha = 0.30F; // 目標30%の不透明度

        this.model.renderToBuffer(poseStack, vertexConsumer, 0xF000F0, OverlayTexture.NO_OVERLAY, red, green, blue, alpha);

        poseStack.popPose();
    }
}
