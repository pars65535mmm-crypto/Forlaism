package com.tyami.forlaism.client;

import com.tyami.forlaism.network.FactotumPacketHandler;
import com.tyami.forlaism.network.GrappleFirePacket;
import com.tyami.forlaism.registry.KeyBindings;

import net.minecraft.client.Minecraft;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * グラップルキー入力をサーバーへ送る。
 */
@Mod.EventBusSubscriber(
        modid = "forlaism",
        value = Dist.CLIENT,
        bus = Mod.EventBusSubscriber.Bus.FORGE
)
public final class GrappleKeyHandler {

    private GrappleKeyHandler() {
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {

        if (event.phase != TickEvent.Phase.END) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;

        // Cキーが押された瞬間を検知
        while (KeyBindings.GRAPPLE_FIRE.consumeClick()) {
            FactotumPacketHandler.CHANNEL.sendToServer(new GrappleFirePacket());
        }
    }
}