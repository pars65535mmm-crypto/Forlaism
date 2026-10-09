package com.tyami.forlaism.client;

import com.tyami.forlaism.network.FactotumPacketHandler;
import com.tyami.forlaism.network.OpenClockBindPacket;
import com.tyami.forlaism.registry.Items;
import com.tyami.forlaism.registry.KeyBindings;

import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import top.theillusivec4.curios.api.CuriosApi;

/**
 * マスターピースクロック装備中、Cキーでバインド画面を開く。
 */
@Mod.EventBusSubscriber(
        modid = "forlaism",
        value = Dist.CLIENT,
        bus = Mod.EventBusSubscriber.Bus.FORGE
)
public final class MasterpieceClockKeyHandler {

    private MasterpieceClockKeyHandler() {
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;

        Minecraft mc = Minecraft.getInstance();
        Player player = mc.player;
        if (player == null) return;

        // キー押下チェック
        while (KeyBindings.MASTERPIECE_CLOCK_BIND.consumeClick()) {

            // Curios の clock スロットにクロックがあるか
            boolean hasClock = CuriosApi.getCuriosInventory(player)
                    .map(handler -> handler.findFirstCurio(
                            stack -> stack.getItem() instanceof com.tyami.forlaism.item.MasterpieceClockItem
                    ).isPresent())
                    .orElse(false);

            if (!hasClock) continue;

            // サーバーへ「バインド画面を開け」とリクエスト
            FactotumPacketHandler.CHANNEL.sendToServer(new OpenClockBindPacket());
        }
    }
}