package com.tyami.forlaism.event;

import com.tyami.forlaism.world.DreamCollapseData;
import com.tyami.forlaism.world.DreamDimension;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Dream崩壊後、Dreamディメンションへの入場を阻止する。
 */
@Mod.EventBusSubscriber(modid = "forlaism")
public final class DreamCollapseBlocker {

    private DreamCollapseBlocker() {
    }

    @SubscribeEvent
    public static void onDimensionChange(PlayerEvent.PlayerChangedDimensionEvent event) {

        if (!(event.getEntity() instanceof ServerPlayer player)) return;

        // Dreamへの入場だけをチェック
        if (!event.getTo().equals(DreamDimension.DREAM_LEVEL)) return;

        // 崩壊済み？
        if (!DreamCollapseData.get(player.server).isCollapsed()) return;

        // 阻止
        event.setCanceled(true);

        player.displayClientMessage(
                Component.literal("§4§lその夢は、既に終わった。")
                        .withStyle(net.minecraft.ChatFormatting.DARK_RED),
                true
        );

        // 元の場所へ引き戻す
        if (event.getFrom() != null) {
            var fromLevel = player.server.getLevel(event.getFrom());
            if (fromLevel != null) {
                player.teleportTo(
                        fromLevel,
                        player.getX(),
                        player.getY(),
                        player.getZ(),
                        player.getYRot(),
                        player.getXRot()
                );
            }
        }
    }

    /**
     * 他MOD/コマンドで無理やり入るのも阻止する。
     */
    @SubscribeEvent
    public static void onPlayerTick(net.minecraftforge.event.TickEvent.PlayerTickEvent event) {

        if (event.phase != net.minecraftforge.event.TickEvent.Phase.END) return;
        if (!(event.player instanceof ServerPlayer player)) return;

        // Dreamにいるか？
        if (!player.level().dimension().equals(DreamDimension.DREAM_LEVEL)) return;

        // 崩壊済み？
        if (!DreamCollapseData.get(player.server).isCollapsed()) return;

        // 追い出す
        var overworld = player.server.overworld();
        var spawnPos = overworld.getSharedSpawnPos();

        player.teleportTo(
                overworld,
                spawnPos.getX() + 0.5,
                spawnPos.getY() + 100.0,
                spawnPos.getZ() + 0.5,
                player.getYRot(),
                player.getXRot()
        );

        player.displayClientMessage(
                Component.literal("§4§lその夢は、既に終わった。")
                        .withStyle(net.minecraft.ChatFormatting.DARK_RED),
                true
        );
    }
}