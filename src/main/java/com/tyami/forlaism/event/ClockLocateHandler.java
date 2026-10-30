package com.tyami.forlaism.event;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.levelgen.structure.Structure;

import java.util.Optional;

/**
 * locate 処理。
 */
public final class ClockLocateHandler {

    private ClockLocateHandler() {
    }

    // =========================================================
    // 構造物検索
    // =========================================================

    public static void locateStructure(ServerPlayer player, String structureId) {
        try {
            ServerLevel sl = player.serverLevel();
            ResourceLocation id = ResourceLocation.tryParse(structureId);

            if (id == null) {
                player.displayClientMessage(
                        Component.literal("§c不正なID: " + structureId), true);
                return;
            }

            var registry = sl.registryAccess().registryOrThrow(Registries.STRUCTURE);
            Structure structure = registry.get(id);

            if (structure == null) {
                player.displayClientMessage(
                        Component.literal("§c構造物が見つからない: " + structureId), true);
                return;
            }

            // 1.20.1: getHolder(ResourceKey) ではなく getResourceKey + getHolder
            var keyOpt = registry.getResourceKey(structure);
            if (keyOpt.isEmpty()) {
                player.displayClientMessage(
                        Component.literal("§c構造物キーが取得できない"), true);
                return;
            }

            Optional<Holder.Reference<Structure>> holderOpt = registry.getHolder(keyOpt.get());
            if (holderOpt.isEmpty()) {
                player.displayClientMessage(
                        Component.literal("§c構造物ホルダーが取得できない"), true);
                return;
            }

            // findNearestMapStructure は HolderSet<Structure> を要求
            HolderSet<Structure> holderSet = HolderSet.direct(holderOpt.get());

            var result = sl.getChunkSource().getGenerator().findNearestMapStructure(
                    sl, holderSet, player.blockPosition(), 100, false
            );

            if (result == null) {
                player.displayClientMessage(
                        Component.literal("§c構造物が見つかりませんでした（範囲外）"), true);
                return;
            }

            BlockPos pos = result.getFirst();
            int dist = (int) Math.sqrt(player.blockPosition().distSqr(pos));

            player.displayClientMessage(
                    Component.literal("§a[Locate] §f" + structureId
                            + ": §e" + pos.getX() + ", " + pos.getY() + ", " + pos.getZ()
                            + " §7(距離: " + dist + "m)"), false);

        } catch (Throwable t) {
            player.displayClientMessage(
                    Component.literal("§c検索失敗: " + t.getMessage()), true);
        }
    }

        // =========================================================
    // バイオーム検索
    // =========================================================

    public static void locateBiome(ServerPlayer player, String biomeId) {
        try {
            ServerLevel sl = player.serverLevel();
            ResourceLocation id = ResourceLocation.tryParse(biomeId);

            if (id == null) {
                player.displayClientMessage(
                        Component.literal("§c不正なID: " + biomeId), true);
                return;
            }

            var registry = sl.registryAccess().registryOrThrow(Registries.BIOME);
            Biome biome = registry.get(id);

            if (biome == null) {
                player.displayClientMessage(
                        Component.literal("§cバイオームが見つからない: " + biomeId), true);
                return;
            }

            var keyOpt = registry.getResourceKey(biome);
            if (keyOpt.isEmpty()) {
                player.displayClientMessage(
                        Component.literal("§cバイオームキーが取得できない"), true);
                return;
            }

            Optional<Holder.Reference<Biome>> holderOpt = registry.getHolder(keyOpt.get());
            if (holderOpt.isEmpty()) {
                player.displayClientMessage(
                        Component.literal("§cバイオームホルダーが取得できない"), true);
                return;
            }

            // ★ 1.20.1 は findClosestBiome3d を使う
            var result = sl.findClosestBiome3d(
                    holder -> holder == holderOpt.get(),  // 対象のバイオームだけに絞る
                    player.blockPosition(),
                    6400,  // 探索半径
                    32,    // 水平ステップ
                    64     // 垂直ステップ
            );

            if (result == null) {
                player.displayClientMessage(
                        Component.literal("§cバイオームが見つかりませんでした（範囲外）"), true);
                return;
            }

            BlockPos pos = result.getFirst();
            int dist = (int) Math.sqrt(player.blockPosition().distSqr(pos));

            player.displayClientMessage(
                    Component.literal("§a[Locate] §f" + biomeId
                            + ": §e" + pos.getX() + ", " + pos.getY() + ", " + pos.getZ()
                            + " §7(距離: " + dist + "m)"), false);

        } catch (Throwable t) {
            player.displayClientMessage(
                    Component.literal("§c検索失敗: " + t.getMessage()), true);
        }
    }
}