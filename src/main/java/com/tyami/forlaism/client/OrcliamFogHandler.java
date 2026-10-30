package com.tyami.forlaism.client;

import com.mojang.blaze3d.shaders.FogShape;
import com.tyami.forlaism.registry.ModEffects;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.event.ViewportEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * orcliam (空回り) エフェクトによる現実のような濃霧の描画ハンドラ。
 *
 * マイクラのパーティクルではなく、ゲームのシェーダー・ビューポートレベルで
 * 現実世界の立ち込める深い霧（10ブロック先完全遮蔽）を再現する。
 */
@OnlyIn(Dist.CLIENT)
@Mod.EventBusSubscriber(
        modid = "forlaism",
        value = Dist.CLIENT,
        bus = Mod.EventBusSubscriber.Bus.FORGE
)
public final class OrcliamFogHandler {

    private OrcliamFogHandler() {
    }

    private static boolean hasOrcliamEffect() {
        Minecraft mc = Minecraft.getInstance();
        Player player = mc.player;
        if (player == null) return false;
        return player.hasEffect(ModEffects.ORCLIAM.get());
    }

    /**
     * 霧の色彩設定: 現実の濃霧・朝靄のような冷たく自然な乳灰白色。
     */
    @SubscribeEvent
    public static void onComputeFogColor(ViewportEvent.ComputeFogColor event) {
        if (!hasOrcliamEffect()) return;

        // 現実の濃霧のカラー (淡い冷白色)
        event.setRed(0.82F);
        event.setGreen(0.85F);
        event.setBlue(0.88F);
    }

    /**
     * 霧の深度・形状設定: 0ブロック（至近距離）から開始し、10ブロック先で完全に見えなくなる。
     */
    @SubscribeEvent
    public static void onRenderFog(ViewportEvent.RenderFog event) {
        if (!hasOrcliamEffect()) return;

        // 目の前から徐々に濃くなり、10ブロックで完全に見えなくなる
        event.setNearPlaneDistance(0.0F);
        event.setFarPlaneDistance(10.0F);
        event.setFogShape(FogShape.SPHERE);
        event.setCanceled(true);
    }
}
