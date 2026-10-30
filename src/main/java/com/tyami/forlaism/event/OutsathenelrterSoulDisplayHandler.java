package com.tyami.forlaism.event;

import com.tyami.forlaism.Forlaism;
import com.tyami.forlaism.item.OutsathenelrterItem;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * アウトサジネーターをメインハンド or オフハンドに持ってる間、
 * アクションバーに「魂数 : N」を表示する。
 */
@Mod.EventBusSubscriber(modid = Forlaism.MOD_ID)
public final class OutsathenelrterSoulDisplayHandler {

    private OutsathenelrterSoulDisplayHandler() {
    }

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        if (!(event.player instanceof ServerPlayer player)) return;

        ItemStack stack = ItemStack.EMPTY;

        // メインハンド優先
        if (player.getMainHandItem().getItem() instanceof OutsathenelrterItem) {
            stack = player.getMainHandItem();
        }
        // オフハンド
        else if (player.getOffhandItem().getItem() instanceof OutsathenelrterItem) {
            stack = player.getOffhandItem();
        }

        // 持ってないなら何もしない
        if (stack.isEmpty()) return;

        int soul = OutsathenelrterItem.getSoul(stack);

        player.displayClientMessage(
                Component.literal("魂数 : " + soul),
                true
        );
    }
}