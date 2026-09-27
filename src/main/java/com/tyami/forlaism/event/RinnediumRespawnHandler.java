package com.tyami.forlaism.event;

import com.tyami.forlaism.registry.Items;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;

import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * 核の自爆で予約されたリンネディウムインゴットを、
 * リスポーン時にリスポーン地点へドロップする。
 */
@Mod.EventBusSubscriber(modid = "forlaism")
public final class RinnediumRespawnHandler {

    private RinnediumRespawnHandler() {
    }

    @SubscribeEvent
    public static void onRespawn(PlayerEvent.PlayerRespawnEvent event) {

        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }

        int count = PendingRinnediumData.consume(player);
        if (count <= 0) {
            return;
        }

        // =========================================================
        // リスポーン地点の取得
        // =========================================================
        BlockPos respawnPos = player.getRespawnPosition();
        ServerLevel respawnLevel = player.server.getLevel(player.getRespawnDimension());

        if (respawnPos == null || respawnLevel == null) {
            // リスポーン地点が未設定 → ワールドスポーン
            respawnLevel = player.server.overworld();
            respawnPos = respawnLevel.getSharedSpawnPos();
        }

        // =========================================================
        // アイテムをドロップ
        // =========================================================
        int remaining = count;
        while (remaining > 0) {
            int size = Math.min(remaining, 64);

            ItemStack drop = new ItemStack(Items.RINNEDIUM_INGOT.get(), size);

            ItemEntity itemEntity = new ItemEntity(
                    respawnLevel,
                    respawnPos.getX() + 0.5,
                    respawnPos.getY() + 0.5,
                    respawnPos.getZ() + 0.5,
                    drop
            );
            itemEntity.setNoPickUpDelay();
            itemEntity.setGlowingTag(true);

            respawnLevel.addFreshEntity(itemEntity);

            remaining -= size;
        }

        player.displayClientMessage(
                net.minecraft.network.chat.Component.literal(
                        "§5§lリンネディウムインゴット §d×" + count + " §fがリスポーン地点に顕現した…"
                ),
                true
        );
    }
}