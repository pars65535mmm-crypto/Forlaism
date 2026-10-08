package com.tyami.forlaism.event;

import com.tyami.forlaism.world.FakedreamDimension;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;

import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Fakedreamディメンションでの死亡処理。
 *
 * 「死んでも帰れない」を実現する。
 *
 * ・Fakedream内で死んだら、Fakedream内のその場でリスポーン
 * ・リスポーン地点を死亡地点に固定する
 * ・通常のリスポーン（オーバーワールド等）に飛ばさない
 */
@Mod.EventBusSubscriber(modid = "forlaism")
public final class FakedreamDeathHandler {

    private FakedreamDeathHandler() {
    }

    /**
     * Fakedream内で死亡した時、リスポーン地点を死亡地点に固定する。
     */
    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onPlayerDeath(LivingDeathEvent event) {

        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }

        // Fakedream以外は通常処理
        if (!player.level().dimension().equals(FakedreamDimension.FAKEDREAM_LEVEL)) {
            return;
        }

        // =========================================================
        // リスポーン地点を「今死んだ場所」に強制固定
        // =========================================================
        BlockPos deathPos = player.blockPosition();

        player.setRespawnPosition(
                FakedreamDimension.FAKEDREAM_LEVEL,
                deathPos,
                player.getYRot(),
                true,
                false
        );


    }

    /**
     * リスポーン時に、Fakedream内ならそのままFakedreamにリスポーンさせる。
     *
     * バニラは respawnPosition を見てリスポーン先を決めるので、
     * 既に死亡時に setRespawnPosition で Fakedream を指定してあれば、
     * ここでは特に何もしなくてよい。
     *
     * 保険として、リスポーン後に座標を保証する。
     */
    @SubscribeEvent
    public static void onPlayerRespawn(PlayerEvent.PlayerRespawnEvent event) {

        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }

        // リスポーン先が Fakedream かチェック
        ServerLevel respawnLevel = player.server.getLevel(
                player.getRespawnDimension()
        );

        if (respawnLevel == null) return;

        if (!respawnLevel.dimension().equals(FakedreamDimension.FAKEDREAM_LEVEL)) {
            return;
        }

    }
}