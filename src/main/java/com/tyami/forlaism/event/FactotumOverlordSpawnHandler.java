package com.tyami.forlaism.event;

import com.tyami.forlaism.registry.ModEntityTypes;
import com.tyami.forlaism.world.DreamDimension;
import com.tyami.forlaism.entity.FactotumOverlordEntity;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.phys.AABB;

import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * プレイヤーが 1000, -60, 1000 の近くに来たら
 * Factotum Overlord を1体だけ召喚する。
 */
@Mod.EventBusSubscriber(modid = "forlaism")
public final class FactotumOverlordSpawnHandler {

    private static final int SPAWN_X = 1000;
    private static final int SPAWN_Y = -60;
    private static final int SPAWN_Z = 1000;

    private static final double TRIGGER_RADIUS = 32.0D; // プレイヤーがこれより近づいたら召喚
    private static final double CHECK_RADIUS = 128.0D;  // ボスがいるか探す範囲

    // 【重要】無限召喚ストッパー: 1回召喚チェックをしたら、次のチェックまで200Hz(10秒)待つ
    private static int checkCooldown = 0;

    private FactotumOverlordSpawnHandler() {
    }

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;

        // クールダウンのカウントダウン（毎Tick減らす）
        if (checkCooldown > 0) {
            checkCooldown--;
            return;
        }

        var server = event.getServer();
        ServerLevel dream = server.getLevel(DreamDimension.DREAM_LEVEL);
        if (dream == null) return;

        BlockPos spawnPos = new BlockPos(SPAWN_X, SPAWN_Y, SPAWN_Z);
        AABB triggerArea = new AABB(spawnPos).inflate(TRIGGER_RADIUS);

        // 1. 指定の範囲内に「プレイヤー」がいるかチェック
        // (プレイヤーが近くにいないなら、これ以降の重い処理や召喚処理は一切しない)
        boolean isPlayerNearby = !dream.getPlayers(player -> player.getBoundingBox().intersects(triggerArea)).isEmpty();
        if (!isPlayerNearby) {
            return;
        }

        // 2. プレイヤーが近くにいた場合のみ、既にボスがいるかチェック
        var existing = dream.getEntitiesOfClass(
                FactotumOverlordEntity.class,
                new AABB(spawnPos).inflate(CHECK_RADIUS),
                entity -> entity.isAlive() && !entity.isRemoved()
        );

        // 既にボスが生きていれば何もしない
        if (!existing.isEmpty()) {
            // ボスが生きている間も、頻繁にgetEntitiesOfClassが走らないよう10秒の猶予を与える
            checkCooldown = 200; 
            return;
        }

        // 3. ボスがいないので召喚
        var overlord = ModEntityTypes.FACTOTUM_OVERLORD.get().create(dream);
        if (overlord != null) {
            overlord.moveTo(
                    SPAWN_X + 0.5,
                    SPAWN_Y,
                    SPAWN_Z + 0.5,
                    0.0F,
                    0.0F
            );
            overlord.finalizeSpawn(
                    dream,
                    dream.getCurrentDifficultyAt(spawnPos),
                    MobSpawnType.STRUCTURE,
                    null,
                    null
            );

            dream.addFreshEntity(overlord);
            
            // 召喚に成功したら、連動して即座に2匹目が湧かないよう次のチェックを10秒止める
            checkCooldown = 200;
        }
    }
}
