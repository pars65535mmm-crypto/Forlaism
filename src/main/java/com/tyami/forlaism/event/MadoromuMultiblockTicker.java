package com.tyami.forlaism.event;

import com.tyami.forlaism.Forlaism;
import com.tyami.forlaism.block.entity.InserterBlockEntity;
import com.tyami.forlaism.quantum.QuantumFusionRecipe;
import com.tyami.forlaism.quantum.QuantumFusionRecipes;
import com.tyami.forlaism.world.MadoromuMultiblockManager;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
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

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Mod.EventBusSubscriber(modid = Forlaism.MOD_ID)
public class MadoromuMultiblockTicker {

    private static final int CHECK_INTERVAL = 20; // 1秒

    @SubscribeEvent
    public static void onLevelTick(TickEvent.LevelTickEvent event) {
        
        if (event.phase != TickEvent.Phase.END) return;
        if (event.level.isClientSide) return;
        if (!(event.level instanceof ServerLevel serverLevel)) return;

        if (serverLevel.getGameTime() % CHECK_INTERVAL != 0) return;
        if (MadoromuMultiblockManager.getActive().isEmpty()) return;

        Set<BlockPos> broken = new HashSet<>();
        // デバッグ: 範囲を可視化（1tickごと）
if (serverLevel.getGameTime() % 5 == 0) { // 5tickごと（重すぎないように）
    for (BlockPos inserterPos : MadoromuMultiblockManager.getActive()) {
        if (!serverLevel.isLoaded(inserterPos)) continue;
        Direction inward = MadoromuMultiblockManager.detectInwardNormal(serverLevel, inserterPos);
        if (inward == null) continue;
        visualizeRange(serverLevel, inserterPos, inward);
    }
}
        

        for (BlockPos inserterPos : MadoromuMultiblockManager.getActive()) {

            if (!serverLevel.isLoaded(inserterPos)) continue;

            Direction inward = MadoromuMultiblockManager.detectInwardNormal(serverLevel, inserterPos);

            if (inward == null
                    || !MadoromuMultiblockManager.validateStructure(serverLevel, inserterPos, inward)) {
                broken.add(inserterPos);
                continue;
            }

            // 稼働中パーティクル
            if (serverLevel.getGameTime() % (CHECK_INTERVAL * 4) == 0) {
                spawnAmbientParticles(serverLevel, inserterPos, inward);
            }
            

            // =========================================================
            // 量子融合の加工処理
            // =========================================================
            processQuantumFusion(serverLevel, inserterPos, inward);
        }

        for (BlockPos pos : broken) {
            MadoromuMultiblockManager.unregister(pos);
            if (serverLevel.getBlockEntity(pos) instanceof InserterBlockEntity inserter) {
                inserter.setQuantumFusionMode(false);
            }
        }
    }

    // =========================================================
    // 量子融合処理
    // =========================================================

    private static void processQuantumFusion(ServerLevel level, BlockPos inserterPos, Direction inward) {

        if (!(level.getBlockEntity(inserterPos) instanceof InserterBlockEntity inserter)) return;
        if (!inserter.isQuantumFusionMode()) return;

        // 内部空洞の中心（Fから内向き2マス、Y+1）
        BlockPos center = inserterPos.relative(inward, 2).above(1);

        // Y2層の内部3×3 のアイテムエンティティを取得
        AABB area = new AABB(
                center.getX() - 1.5, center.getY() - 1.0, center.getZ() - 1.5,
                center.getX() + 1.5, center.getY() + 1.0, center.getZ() + 1.5
        );

        List<ItemEntity> items = level.getEntitiesOfClass(ItemEntity.class, area,
                e -> e.isAlive() && !e.getItem().isEmpty());

         if (level.getGameTime() % 20 == 0) {
        for (ItemEntity e : items) {
            System.out.println("  - " + e.getItem().getItem()
                    + " x" + e.getItem().getCount()
                    + " @ " + e.position());
        }
        System.out.println("[QuantumFusion] Recipes: " + QuantumFusionRecipes.getAll().size());
    }

        if (items.isEmpty()) return;

        // 各レシピをチェック
        for (QuantumFusionRecipe recipe : QuantumFusionRecipes.getAll()) {

            // 1. FEチェック
            if (inserter.getQuantumEnergyStorage().getEnergyStored() < recipe.requiredFE()) continue;

            // 2. アイテムマッチング
            if (!matchesRecipe(items, recipe)) continue;

            // 3. 加工実行！
            executeFusion(level, inserterPos, center, items, recipe, inserter);
            return;
        }
    }

    /**
     * アイテムエンティティのリストがレシピにマッチするかチェック。
     * 各必要アイテムについて、対応する ItemEntity が存在するか。
     */
private static boolean matchesRecipe(List<ItemEntity> items, QuantumFusionRecipe recipe) {

    for (ItemStack required : recipe.inputs()) {

        long totalCount = 0;

        for (ItemEntity entity : items) {
            ItemStack stack = entity.getItem();
            if (ItemStack.isSameItemSameTags(stack, required)) {
                totalCount += stack.getCount();
            }
        }

        if (totalCount < required.getCount()) {
            return false;
        }
    }
    return true;
}

    /**
     * 加工を実行。
     */
private static void executeFusion(ServerLevel level, BlockPos inserterPos, BlockPos center,
                                  List<ItemEntity> items, QuantumFusionRecipe recipe,
                                  InserterBlockEntity inserter) {

    // =========================================================
    // 1. アイテム消費（合計数ベースで複数ItemEntityから引く）
    // =========================================================
    for (ItemStack required : recipe.inputs()) {

        int remaining = required.getCount();

        for (ItemEntity entity : items) {
            if (remaining <= 0) break;

            ItemStack stack = entity.getItem();

            if (!ItemStack.isSameItemSameTags(stack, required)) continue;

            int toConsume = Math.min(remaining, stack.getCount());

            stack.shrink(toConsume);
            remaining -= toConsume;

            if (stack.isEmpty()) {
                entity.discard();
            } else {
                entity.setItem(stack);
            }
        }
    }

    // =========================================================
    // 2. FE消費
    // =========================================================
    inserter.getQuantumEnergyStorage().extractEnergyInternal(recipe.requiredFE(), false);

    // =========================================================
    // 3. 成功率判定
    // =========================================================
    boolean success = level.getRandom().nextFloat() <= recipe.successRate();

        if (success) {
            // 4. 出力をドロップ
            ItemStack output = recipe.output().copy();
            ItemEntity outputEntity = new ItemEntity(
                    level,
                    center.getX() + 0.5, center.getY() + 0.5, center.getZ() + 0.5,
                    output
            );
            outputEntity.setNoPickUpDelay();
            level.addFreshEntity(outputEntity);

            // 成功演出
            level.playSound(null, inserterPos, SoundEvents.BEACON_POWER_SELECT,
                    SoundSource.BLOCKS, 1.0F, 1.5F);

            level.sendParticles(
                    ParticleTypes.END_ROD,
                    center.getX() + 0.5, center.getY() + 0.5, center.getZ() + 0.5,
                    50, 1.0, 1.0, 1.0, 0.15
            );
        } else {
            // 失敗演出
            level.playSound(null, inserterPos, SoundEvents.FIRE_EXTINGUISH,
                    SoundSource.BLOCKS, 1.0F, 0.5F);

            level.sendParticles(
                    ParticleTypes.SMOKE,
                    center.getX() + 0.5, center.getY() + 0.5, center.getZ() + 0.5,
                    30, 0.5, 0.5, 0.5, 0.05
            );
        }

        // 加工完了メッセージ（任意）
        // 近くのプレイヤーに通知
        level.getPlayers(p -> p.distanceToSqr(center.getX(), center.getY(), center.getZ()) < 100)
                .forEach(p -> p.displayClientMessage(
                        success
                                ? net.minecraft.network.chat.Component.literal("§d量子融合 §a成功 §f→ " + recipe.output().getHoverName().getString())
                                : net.minecraft.network.chat.Component.literal("§d量子融合 §c失敗…"),
                        true
                ));
    }

    // =========================================================
    // パーティクル
    // =========================================================

    private static void spawnAmbientParticles(ServerLevel level, BlockPos inserterPos, Direction inward) {
        BlockPos center = inserterPos.relative(inward, 2).above(4);

        level.sendParticles(
                ParticleTypes.ENCHANT,
                center.getX() + 0.5, center.getY() + 0.5, center.getZ() + 0.5,
                15, 2.0, 3.0, 2.0, 0.05
        );

        level.sendParticles(
                ParticleTypes.END_ROD,
                center.getX() + 0.5, center.getY() + 0.5, center.getZ() + 0.5,
                5, 1.5, 3.0, 1.5, 0.02
        );
    }


    /**
 * 判定範囲（AABB）をパーティクルで可視化。
 * AABBの8隅 + 12辺に沿って粒子を配置。
 */
private static void visualizeRange(ServerLevel level, BlockPos inserterPos, Direction inward) {
    net.minecraft.world.phys.AABB aabb = MadoromuMultiblockManager.getInnerAABB(inserterPos, inward);

    double x0 = aabb.minX, y0 = aabb.minY, z0 = aabb.minZ;
    double x1 = aabb.maxX, y1 = aabb.maxY, z1 = aabb.maxZ;

    // 8隅
    spawnMarker(level, x0, y0, z0);
    spawnMarker(level, x1, y0, z0);
    spawnMarker(level, x0, y0, z1);
    spawnMarker(level, x1, y0, z1);
    spawnMarker(level, x0, y1, z0);
    spawnMarker(level, x1, y1, z0);
    spawnMarker(level, x0, y1, z1);
    spawnMarker(level, x1, y1, z1);

    // 12辺（各辺3点で）
    int steps = 4;
    for (int i = 1; i < steps; i++) {
        double t = (double) i / steps;
        // X方向の辺
        spawnMarker(level, x0 + (x1-x0)*t, y0, z0);
        spawnMarker(level, x0 + (x1-x0)*t, y1, z0);
        spawnMarker(level, x0 + (x1-x0)*t, y0, z1);
        spawnMarker(level, x0 + (x1-x0)*t, y1, z1);
        // Y方向の辺
        spawnMarker(level, x0, y0 + (y1-y0)*t, z0);
        spawnMarker(level, x1, y0 + (y1-y0)*t, z0);
        spawnMarker(level, x0, y0 + (y1-y0)*t, z1);
        spawnMarker(level, x1, y0 + (y1-y0)*t, z1);
        // Z方向の辺
        spawnMarker(level, x0, y0, z0 + (z1-z0)*t);
        spawnMarker(level, x0, y1, z0 + (z1-z0)*t);
        spawnMarker(level, x1, y0, z0 + (z1-z0)*t);
        spawnMarker(level, x1, y1, z0 + (z1-z0)*t);
    }
}

/** 1点にパーティクルを出す。 */
private static void spawnMarker(ServerLevel level, double x, double y, double z) {
    level.sendParticles(
            ParticleTypes.END_ROD,   // 光る粒子
            x, y, z,
            1,
            0, 0, 0,
            0
    );
}
}