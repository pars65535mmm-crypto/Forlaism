package com.tyami.forlaism.client;

import com.tyami.forlaism.item.PrismLightItem;
import com.tyami.forlaism.network.FactotumPacketHandler;
import com.tyami.forlaism.network.PrismLockPacket;

import net.minecraft.client.Minecraft;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * プリズムライト構え中にShiftが押されたらロックパケット送信。
 */
@Mod.EventBusSubscriber(
        modid = "forlaism",
        value = Dist.CLIENT,
        bus = Mod.EventBusSubscriber.Bus.FORGE
)
public final class PrismLightKeyHandler {

    private static boolean lastShiftState = false;

    private PrismLightKeyHandler() {
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;

        // プリズムライトを構えているか
        boolean usingPrism = mc.player.isUsingItem()
                && mc.player.getUseItem().getItem() instanceof PrismLightItem;

        if (!usingPrism) {
            lastShiftState = false;
            return;
        }

        boolean shiftNow = mc.player.isShiftKeyDown();

        // Shift押した瞬間を検出
        if (shiftNow && !lastShiftState) {
            FactotumPacketHandler.CHANNEL.sendToServer(new PrismLockPacket());
        }
        lastShiftState = shiftNow;
    }
}