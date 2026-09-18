package com.tyami.forlaism.item;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;

import java.util.List;

public final class SovereignLootExtractor {

    private SovereignLootExtractor() {
    }

    /**
     * 敵自身のLoot Tableを直接引いて
     * ドロップアイテムをその場に生成する。
     */
    public static void extract(
            ServerLevel level,
            LivingEntity target,
            Player player,
            ItemStack weapon
    ) {

        /*
         * 敵が持っているLoot Tableを取得
         */
        var lootTableId = target.getLootTable();

        if (lootTableId == null) {
            return;
        }

        LootTable lootTable =
                level.getServer()
                        .getLootData()
                        .getLootTable(lootTableId);

        if (lootTable == LootTable.EMPTY) {
            return;
        }

        /*
         * Loot Contextを構築
         *
         * THIS_ENTITY  = 倒された/殴られた敵
         * KILLER_ENTITY = プレイヤー
         * ORIGIN       = 敵の位置
         * TOOL         = 君主乃笏剣
         */
        LootParams params =
                new LootParams.Builder(level)

                        .withParameter(
                                LootContextParams.THIS_ENTITY,
                                target
                        )

                        .withParameter(
                                LootContextParams.KILLER_ENTITY,
                                player
                        )

                        .withParameter(
                                LootContextParams.ORIGIN,
                                target.position()
                        )

                        .withParameter(
                                LootContextParams.TOOL,
                                weapon
                        )

                        .withParameter(
                                LootContextParams.DAMAGE_SOURCE,
                                level.damageSources().playerAttack(player)
                        )

                        .withLuck(
                                player.getLuck()
                        )

                        .create(
                                LootContextParamSets.ENTITY
                        );

        /*
         * Loot Tableを直接ロール
         */
        List<ItemStack> drops =
                lootTable.getRandomItems(params);

        /*
         * アイテムを敵の位置へ吐き出す
         */
        for (ItemStack drop : drops) {

            if (drop.isEmpty()) {
                continue;
            }

            target.spawnAtLocation(
                    drop,
                    0.0F
            );
        }
    }
}