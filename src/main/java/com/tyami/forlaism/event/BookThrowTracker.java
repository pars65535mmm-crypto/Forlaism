package com.tyami.forlaism.event;

import com.tyami.forlaism.registry.Items;

import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import net.minecraftforge.event.entity.item.ItemTossEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * 生贄の書を投げた時に、投げたプレイヤーのUUIDを記録する。
 */
@Mod.EventBusSubscriber(modid = "forlaism")
public final class BookThrowTracker {

    private BookThrowTracker() {
    }

    @SubscribeEvent
    public static void onItemToss(ItemTossEvent event) {

        ItemEntity itemEntity = event.getEntity();
        ItemStack stack = itemEntity.getItem();

        if (!stack.is(Items.BOOK_OF_SACRIFICE.get())) {
            return;
        }

        Player player = event.getPlayer();

        // ItemStack のNBTに Thrower を記録
        stack.getOrCreateTag().putUUID("Thrower", player.getUUID());
    }
}