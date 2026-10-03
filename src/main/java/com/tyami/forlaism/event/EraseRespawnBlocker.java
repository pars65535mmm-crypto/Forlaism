package com.tyami.forlaism.event;

import com.tyami.forlaism.erase.EraseHelper;
import com.tyami.forlaism.erase.EraseRegistry;
import com.tyami.forlaism.annihilation.GMBAnnihilation;
import com.tyami.forlaism.annihilation.GMBEraseRegistry;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;

import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.UUID;

/**
 * 抹消済みEntityの再スポーンを阻止する。
 *
 * - EntityJoinLevelEvent: 追加時点で弾く
 * - LevelTickEvent: 定期的に監視して、復活していたら再抹消
 */
@Mod.EventBusSubscriber(modid = "forlaism")
public final class EraseRespawnBlocker {

    private EraseRespawnBlocker() {
    }

    // =========================================================
    // 追加阻止
    // =========================================================

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onEntityJoin(EntityJoinLevelEvent event) {

        Level level = event.getLevel();
        if (level.isClientSide) return;
        if (!(level instanceof ServerLevel serverLevel)) return;

        Entity entity = event.getEntity();
        UUID uuid = entity.getUUID();

        // 抹消済みなら追加させない
        if (EraseRegistry.isErased(serverLevel.getServer(), uuid)
                || GMBEraseRegistry.isErased(serverLevel.getServer(), uuid)) {
            event.setCanceled(true);

            // さらに念のため物理削除も仕込む
            // （canceledでも何かが追加される可能性があるため）
            serverLevel.getServer().execute(() -> {
                if (GMBEraseRegistry.isErased(serverLevel.getServer(), uuid)) {
                    GMBAnnihilation.forceRemove(serverLevel, entity);
                } else {
                    EraseHelper.forceRemove(serverLevel, entity);
                }
            });
        }
    }

    // =========================================================
    // 定期監視（復活検知）
    // =========================================================

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {

        if (event.phase != TickEvent.Phase.END) return;

        var server = event.getServer();
        if (server == null) return;

        // 20tick毎（1秒毎）にチェック
        if (server.getTickCount() % 20 != 0) return;

        for (ServerLevel level : server.getAllLevels()) {
            for (Entity entity : level.getAllEntities()) {
                UUID uuid = entity.getUUID();

                if (EraseRegistry.isErased(server, uuid)
                        || GMBEraseRegistry.isErased(server, uuid)) {
                    // 復活してる！ 即抹消
                    if (GMBEraseRegistry.isErased(server, uuid)) {
                        GMBAnnihilation.erase(level, entity, null, false);
                    } else {
                        EraseHelper.forceRemove(level, entity);
                    }
                }
            }
        }
    }
}
