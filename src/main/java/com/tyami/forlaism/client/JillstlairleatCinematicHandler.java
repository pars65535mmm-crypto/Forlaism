package com.tyami.forlaism.client;

import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.DeathScreen;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.event.RenderGuiOverlayEvent;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.client.event.ViewportEvent;
import net.minecraftforge.client.gui.overlay.VanillaGuiOverlay;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * ジルスレイトの死亡暗転および自壊シネマティック演出ハンドラ。
 */
@OnlyIn(Dist.CLIENT)
@Mod.EventBusSubscriber(
        modid = "forlaism",
        value = Dist.CLIENT,
        bus = Mod.EventBusSubscriber.Bus.FORGE
)
public final class JillstlairleatCinematicHandler {

    /** 死亡シーケンスの経過tick (0 ~ 500: 20秒暗転 + 5秒待機) */
    private static int deathTicks = -1;

    /** 自壊シーケンスの経過tick (0 ~ 40) */
    private static int selfDestructTicks = -1;
    private static CameraType originalCameraType = CameraType.FIRST_PERSON;

    private JillstlairleatCinematicHandler() {
    }

    public static void startDeathSequence() {
        deathTicks = 0;
    }

    public static void startSelfDestructSequence() {
        selfDestructTicks = 0;
        Minecraft mc = Minecraft.getInstance();
        if (mc.options != null) {
            originalCameraType = mc.options.getCameraType();
            // 三人称後方視点に切り替え
            mc.options.setCameraType(CameraType.THIRD_PERSON_BACK);
        }
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        Minecraft mc = Minecraft.getInstance();
        Player player = mc.player;

        // 死亡シーケンス進行
        if (deathTicks >= 0) {
            deathTicks++;
            if (deathTicks >= 500) {
                // 25秒経過: 終了
                deathTicks = -1;
            }
        }

        // 自壊シーケンス進行
        if (selfDestructTicks >= 0) {
            selfDestructTicks++;

            if (player != null && mc.level != null) {
                // 15tick以降はプレイヤーが消え、青い塵（パーティクル）が舞う
                if (selfDestructTicks >= 15) {
                    for (int i = 0; i < 4; i++) {
                        mc.level.addParticle(
                                ParticleTypes.SOUL_FIRE_FLAME,
                                player.getX() + (mc.level.random.nextDouble() - 0.5) * 1.5,
                                player.getY() + mc.level.random.nextDouble() * 1.8,
                                player.getZ() + (mc.level.random.nextDouble() - 0.5) * 1.5,
                                0, 0.05, 0
                        );
                    }
                }
            }

            if (selfDestructTicks >= 40) {
                // 演出終了
                selfDestructTicks = -1;
                if (mc.options != null) {
                    mc.options.setCameraType(originalCameraType);
                }
            }
        }
    }

    /**
     * 死亡待機中はデス画面（リスポーンボタン）を開かせない。
     */
    @SubscribeEvent
    public static void onScreenOpen(ScreenEvent.Opening event) {
        if (deathTicks >= 0 && event.getScreen() instanceof DeathScreen) {
            event.setCanceled(true);
        }
    }

    /**
     * 自壊シーケンス中のカメラ操作: 遠くから見つめるアングルにズームアウト。
     */
    @SubscribeEvent
    public static void onComputeCameraAngles(ViewportEvent.ComputeCameraAngles event) {
        if (selfDestructTicks >= 0) {
            // 見下ろしアングル（ピッチを下向きに固定、少し回転）
            float progress = (float) selfDestructTicks / 40.0F;
            event.setPitch(25.0F + progress * 10.0F);
            event.setYaw(event.getYaw() + progress * 15.0F);
        }
    }

    /**
     * 死亡シーケンス中の画面暗転描画（20秒かけて徐々に暗くなり、5秒間完全に真っ暗）。
     */
    @SubscribeEvent
    public static void onRenderOverlay(RenderGuiOverlayEvent.Post event) {
        if (deathTicks < 0) return;
        if (!event.getOverlay().id().equals(VanillaGuiOverlay.HOTBAR.id())) return;

        GuiGraphics graphics = event.getGuiGraphics();
        int w = event.getWindow().getGuiScaledWidth();
        int h = event.getWindow().getGuiScaledHeight();

        float alpha;
        if (deathTicks < 400) {
            // 20秒（400tick）かけて 0.0 -> 1.0 へフェード
            alpha = (float) deathTicks / 400.0F;
        } else {
            // 完全に真っ暗
            alpha = 1.0F;
        }

        alpha = Math.max(0.0F, Math.min(1.0F, alpha));
        int color = ((int) (alpha * 255.0F) << 24); // 黒色塗りつぶし

        graphics.fill(0, 0, w, h, color);
    }
}
