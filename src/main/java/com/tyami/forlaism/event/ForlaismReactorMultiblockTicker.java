package com.tyami.forlaism.event;

import com.tyami.forlaism.Forlaism;
import com.tyami.forlaism.block.entity.InserterBlockEntity;
import com.tyami.forlaism.world.ForlaismReactorMultiblockManager;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;

import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import com.tyami.forlaism.quantum.ReactorRecipe;
import com.tyami.forlaism.quantum.ReactorRecipes;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 輪廻再転式量子融合炉の毎tick処理。
 *
 * - 構造チェック（壊れたら自動解除 + reactorMode OFF）
 * - 中央空洞のアイテム取得
 * - 搬入機の FE 消費
 * - レシピ加工実行
 */
@Mod.EventBusSubscriber(modid = Forlaism.MOD_ID)
public class ForlaismReactorMultiblockTicker {

    /** 構造チェック間隔（tick）。 */
    private static final int CHECK_INTERVAL = 20;

    /** アンビエントパーティクル間隔（tick）。 */
    private static final int PARTICLE_INTERVAL = 5;

    @SubscribeEvent
    public static void onLevelTick(TickEvent.LevelTickEvent event) {

        if (event.phase != TickEvent.Phase.END) return;
        if (event.level.isClientSide) return;
        if (!(event.level instanceof ServerLevel serverLevel)) return;

        CherenkovEffect.tick(serverLevel);

        if (ForlaismReactorMultiblockManager.getActive().isEmpty()) return;

        // =========================================================
        // アンビエントパーティクル（短い間隔）
        // =========================================================
        if (serverLevel.getGameTime() % PARTICLE_INTERVAL == 0) {
            for (BlockPos origin : ForlaismReactorMultiblockManager.getActive()) {
                if (!serverLevel.isLoaded(origin)) continue;
                spawnAmbientParticles(serverLevel, origin);
            }
        }

        // =========================================================
        // 構造チェック + 加工処理（1秒ごと）
        // =========================================================
        if (serverLevel.getGameTime() % CHECK_INTERVAL != 0) return;

        Set<BlockPos> broken = new HashSet<>();

        for (BlockPos origin : ForlaismReactorMultiblockManager.getActive()) {

            if (!serverLevel.isLoaded(origin)) continue;

            // 構造チェック
            if (!ForlaismReactorMultiblockManager.validateStructure(serverLevel, origin)) {
                broken.add(origin);
                continue;
            }

            // 加工処理
            processReaction(serverLevel, origin);
        }

        // 壊れたものを解除
        for (BlockPos pos : broken) {
            ForlaismReactorMultiblockManager.unregister(pos);

            // reactorMode OFF
            BlockPos inserterPos = ForlaismReactorMultiblockManager.getInserterPos(pos);
            if (serverLevel.getBlockEntity(inserterPos) instanceof InserterBlockEntity inserter) {
                inserter.setReactorMode(false);
            }

            // 解除演出
            BlockPos center = ForlaismReactorMultiblockManager.getCenterHole(pos);
            serverLevel.playSound(
                    null, center,
                    SoundEvents.BEACON_DEACTIVATE,
                    SoundSource.BLOCKS,
                    1.0F, 0.8F
            );
        }
    }

        // =========================================================
    // 加工処理
    // =========================================================

    private static void processReaction(ServerLevel level, BlockPos origin) {

        // 搬入機を取得
        BlockPos inserterPos = ForlaismReactorMultiblockManager.getInserterPos(origin);
        if (!(level.getBlockEntity(inserterPos) instanceof InserterBlockEntity inserter)) {
            return;
        }

        // 中央空洞のアイテムを取得
        AABB area = ForlaismReactorMultiblockManager.getCenterAABB(origin);
        List<ItemEntity> items = level.getEntitiesOfClass(
                ItemEntity.class, area,
                e -> e.isAlive() && !e.getItem().isEmpty()
        );

        if (items.isEmpty()) return;

        // =========================================================
        // 登録済みレシピを順番に試す
        // =========================================================
        for (ReactorRecipe recipe : ReactorRecipes.getAll()) {

            // FEチェック
            if (inserter.getQuantumEnergyStorage().getEnergyStored() < recipe.requiredFE()) {
                continue;
            }

            // 素材チェック
            if (!matchesRecipe(items, recipe)) {
                continue;
            }

            // 成立！ 実行して抜ける
            executeRecipe(level, origin, inserter, items, recipe);
            return;
        }
    }

    /**
     * アイテムエンティティ群がレシピの必要素材を満たすか。
     */
    private static boolean matchesRecipe(List<ItemEntity> items, ReactorRecipe recipe) {

        for (var entry : recipe.inputs().entrySet()) {
            net.minecraft.world.item.Item item = entry.getKey();
            int need = entry.getValue();

            int total = 0;
            for (ItemEntity e : items) {
                if (e.getItem().is(item)) {
                    total += e.getItem().getCount();
                }
            }

            if (total < need) return false;
        }

        return true;
    }

    /**
     * レシピ実行。
     */
    private static void executeRecipe(
            ServerLevel level,
            BlockPos origin,
            InserterBlockEntity inserter,
            List<ItemEntity> items,
            ReactorRecipe recipe
    ) {

        // =========================================================
        // 1. 素材消費
        // =========================================================
        for (var entry : recipe.inputs().entrySet()) {
            net.minecraft.world.item.Item item = entry.getKey();
            int remaining = entry.getValue();

            for (ItemEntity e : items) {
                if (remaining <= 0) break;

                ItemStack st = e.getItem();
                if (!st.is(item)) continue;

                int take = Math.min(remaining, st.getCount());
                st.shrink(take);
                remaining -= take;

                if (st.isEmpty()) e.discard();
                else e.setItem(st);
            }
        }

        // =========================================================
        // 2. FE消費
        // =========================================================
        inserter.getQuantumEnergyStorage()
                .extractEnergyInternal(recipe.requiredFE(), false);

        // =========================================================
        // 3. 出力ドロップ
        // =========================================================
        BlockPos center = ForlaismReactorMultiblockManager.getCenterHole(origin);

        ItemEntity outputEntity = new ItemEntity(
                level,
                center.getX() + 0.5, center.getY() + 3.0, center.getZ() + 0.5,
                recipe.output().copy()
        );
        outputEntity.setNoPickUpDelay();
        level.addFreshEntity(outputEntity);

        // =========================================================
        // 4. 加工成功演出
        // =========================================================

        // 音
        level.playSound(null, inserter.getBlockPos(), SoundEvents.BEACON_POWER_SELECT,
                SoundSource.BLOCKS, 1.0F, 1.5F);

        level.playSound(null, inserter.getBlockPos(), SoundEvents.AMETHYST_BLOCK_CHIME,
                SoundSource.BLOCKS, 1.5F, 0.8F);

        level.playSound(null, inserter.getBlockPos(), SoundEvents.RESPAWN_ANCHOR_CHARGE,
                SoundSource.BLOCKS, 1.0F, 2.0F);

        // チェレンコフ光演出をスケジュール登録
        CherenkovEffect.play(level, center);

        // パケット送信
        var packet = new com.tyami.forlaism.network.CherenkovEffectPacket(center, 30);

        for (var player : level.players()) {
            if (player.distanceToSqr(center.getX(), center.getY(), center.getZ()) < 128 * 128) {
                com.tyami.forlaism.network.FactotumPacketHandler.CHANNEL.sendTo(
                        packet,
                        ((net.minecraft.server.level.ServerPlayer) player).connection.connection,
                        net.minecraftforge.network.NetworkDirection.PLAY_TO_CLIENT
                );
            }
        }

        // パーティクル
        level.sendParticles(
                ParticleTypes.END_ROD,
                center.getX() + 0.5, center.getY() + 3.0, center.getZ() + 0.5,
                30, 1.0, 3.0, 1.0, 0.2
        );

        // 近くのプレイヤーにメッセージ
        String outputName = recipe.output().getHoverName().getString();
        level.getPlayers(p -> p.distanceToSqr(
                center.getX(), center.getY(), center.getZ()) < 128 * 128
        ).forEach(p -> p.displayClientMessage(
                net.minecraft.network.chat.Component.literal(
                        "§b§l" + outputName + " §fが §d顕現 §fした…"
                ),
                true
        ));
    }

    // =========================================================
    // アンビエントパーティクル
    // =========================================================

    private static void spawnAmbientParticles(ServerLevel level, BlockPos origin) {
        BlockPos center = ForlaismReactorMultiblockManager.getCenterHole(origin);

        level.sendParticles(
                ParticleTypes.ENCHANT,
                center.getX() + 0.5, center.getY() + 3.0, center.getZ() + 0.5,
                5, 0.8, 3.0, 0.8, 0.05
        );

        level.sendParticles(
                ParticleTypes.END_ROD,
                center.getX() + 0.5, center.getY() + 3.0, center.getZ() + 0.5,
                2, 0.3, 3.0, 0.3, 0.02
        );
    }
}