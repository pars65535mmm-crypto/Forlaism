package com.tyami.forlaism.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.tyami.forlaism.item.MuratsukumoDashTracker;
import com.tyami.forlaism.item.MuratsukumoItem;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderHandEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * ムラツクモを両手に装備したときの一人称視点の手描画。
 *
 * 二刀流を表現するため、メインとオフで逆の動きをする。
 *
 * - 構え: メインは右上、オフは左下
 * - 通常攻撃: 交差斬撃 + 返し
 * - 突進中: 前方突き出し + 逆回転
 */
@Mod.EventBusSubscriber(
        modid = "forlaism",
        value = Dist.CLIENT,
        bus = Mod.EventBusSubscriber.Bus.FORGE
)
public final class MuratsukumoRenderer {

    private MuratsukumoRenderer() {
    }

    @SubscribeEvent
    public static void onRenderHand(RenderHandEvent event) {

        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null) return;

        if (!MuratsukumoItem.isDualWielding(player)) {
            return;
        }

        event.setCanceled(true);

        if (event.getHand() != InteractionHand.MAIN_HAND) {
            return;
        }

        Minecraft mc = Minecraft.getInstance();
        ItemRenderer itemRenderer = mc.getItemRenderer();

        PoseStack pose = event.getPoseStack();
        MultiBufferSource buffer = event.getMultiBufferSource();
        int light = event.getPackedLight();
        float partialTick = event.getPartialTick();

        ItemStack muratsukumo = player.getMainHandItem();

        renderBlade(
                mc, player, itemRenderer, pose, buffer, light, partialTick,
                muratsukumo, HumanoidArm.RIGHT
        );

        renderBlade(
                mc, player, itemRenderer, pose, buffer, light, partialTick,
                muratsukumo, HumanoidArm.LEFT
        );
    }

    private static void renderBlade(
            Minecraft mc,
            LocalPlayer player,
            ItemRenderer itemRenderer,
            PoseStack pose,
            MultiBufferSource buffer,
            int light,
            float partialTick,
            ItemStack stack,
            HumanoidArm arm
    ) {

        boolean mainArmIsRight =
                mc.options.mainHand().get() == HumanoidArm.RIGHT;

        boolean isMainHand = (arm == HumanoidArm.RIGHT) == mainArmIsRight;

        // side: メイン=+1 / オフ=-1 (画面内の左右)
        float side = isMainHand
                ? (mainArmIsRight ? 1.0F : -1.0F)
                : (mainArmIsRight ? -1.0F : 1.0F);

        pose.pushPose();

        // =========================================================
        // 1. 基準位置
        // =========================================================
        float baseX = 0.62F * side;
        float baseY = -0.32F;
        float baseZ = -0.65F;

        pose.translate(baseX, baseY, baseZ);

        // =========================================================
        // 2. 状態取得
        // =========================================================
        int dashTicks = player.getPersistentData()
                .getInt(MuratsukumoDashTracker.TAG_DASH_TICKS);

        // 通常攻撃の振り (0.0 ～ 1.0 → 0.0)
        float attackSwing = player.getAttackAnim(partialTick);

        // =========================================================
        // 3. 突進中: 前方突き出し + 逆回転
        // =========================================================
        if (dashTicks > 0) {

            float time = player.tickCount + partialTick;

            // メインとオフで逆回転
            float dir = isMainHand ? 1.0F : -1.0F;
            float spin = (time * 90.0F * dir) % 360.0F;

            // 前方へ突き出す
            pose.translate(0.0F, 0.20F, -0.30F);

            // 回転（メインは順、オフは逆）
            pose.mulPose(Axis.XP.rotationDegrees(spin));

            // 内側に少し傾ける
            pose.mulPose(Axis.ZP.rotationDegrees(-15.0F * dir));

            // 拡大
            pose.scale(1.25F, 1.25F, 1.25F);

        } else {

            // =========================================================
            // 4. 通常時: 構え + 交差斬撃 + 返し
            // =========================================================

            // メインとオフで「振り始めの向き」を逆にする
            // メイン: 右上 → 左下
            // オフ  : 左上 → 右下

            // swing を 0→1→0 の三角波に
            // attackSwing 自体は 0→1→0 だが、
            // 「返し」も表現したいので、sin 波で 0→1→0→1... を作る
            float swing01 = attackSwing;

            // 0 → 1 の前半で振り下ろし、1 → 0 の後半で返し
            // 返しは同じ角度を使い回すので、両方で同じ計算式を使う
            float swing = swing01;

            // ---- 基準構え ----
            // メインは右上に構える（YP -60、ZP -25、XP 20）
            // オフは左上に構える（YP +60、ZP +25、XP 20）
            float idleYaw   = isMainHand ? -60.0F : 60.0F;
            float idleRoll  = isMainHand ? -25.0F : 25.0F;
            float idlePitch = 20.0F;

            // ---- 振り下ろし先 ----
            // メインは左下（YP +40、ZP +40、XP -50）
            // オフは右下（YP -40、ZP -40、XP -50）
            float endYaw   = isMainHand ? 40.0F : -40.0F;
            float endRoll  = isMainHand ? 40.0F : -40.0F;
            float endPitch = -90.0F;

            // ---- 補間 ----
            float yaw   = Mth.lerp(swing, idleYaw, endYaw);
            float roll  = Mth.lerp(swing, idleRoll, endRoll);
            float pitch = Mth.lerp(swing, idlePitch, endPitch);

            // 振り中は前方へ少し出す
            pose.translate(0.0F, swing * 0.10F, -swing * 0.25F);

            // 回転適用
            pose.mulPose(Axis.YP.rotationDegrees(yaw));
            pose.mulPose(Axis.ZP.rotationDegrees(roll));
            pose.mulPose(Axis.XP.rotationDegrees(pitch));
        }

        // 刀の向きを整える（元コードと同じ）
        pose.mulPose(Axis.XP.rotationDegrees(-90.0F));
        pose.mulPose(Axis.YP.rotationDegrees(180.0F));

        // =========================================================
        // 5. 描画
        // =========================================================
        itemRenderer.renderStatic(
                stack,
                ItemDisplayContext.FIRST_PERSON_RIGHT_HAND,
                light,
                OverlayTexture.NO_OVERLAY,
                pose,
                buffer,
                player.level(),
                player.getId()
        );

        pose.popPose();
    }
}