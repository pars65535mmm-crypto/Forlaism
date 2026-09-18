package com.tyami.forlaism.util;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;

import java.util.List;

public final class FactotumMiningHelper {

    private FactotumMiningHelper() {
    }

    /**
     * 指定された範囲のブロックを何でも破壊・回収する。
     * 硬度制限（岩盤等）を無視し、ドロップアイテムを確実にプレイヤーのインベントリに回収する。
     */
    public static void mineArea(ServerLevel level, ServerPlayer player, BlockPos originPos, int range, ItemStack tool) {
        if (range <= 0) range = 1;

        // プレイヤーの視線方向（掘っている面）を取得
        HitResult hitResult = player.pick(6.0D, 0.0F, false);
        Direction hitFace = Direction.UP;
        if (hitResult instanceof BlockHitResult blockHitResult) {
            hitFace = blockHitResult.getDirection();
        }

        int half = range / 2;
        int rem = range - half - 1;

        int minX, maxX, minY, maxY, minZ, maxZ;

        // 掘っている面に応じて自然な範囲を展開
        switch (hitFace.getAxis()) {
            case Y -> {
                minX = originPos.getX() - half;
                maxX = originPos.getX() + rem;
                minZ = originPos.getZ() - half;
                maxZ = originPos.getZ() + rem;
                minY = (hitFace == Direction.UP) ? originPos.getY() - (range - 1) : originPos.getY();
                maxY = (hitFace == Direction.UP) ? originPos.getY() : originPos.getY() + (range - 1);
            }
            case X -> {
                minY = originPos.getY() - half;
                maxY = originPos.getY() + rem;
                minZ = originPos.getZ() - half;
                maxZ = originPos.getZ() + rem;
                minX = (hitFace == Direction.EAST) ? originPos.getX() - (range - 1) : originPos.getX();
                maxX = (hitFace == Direction.EAST) ? originPos.getX() : originPos.getX() + (range - 1);
            }
            case Z -> {
                minX = originPos.getX() - half;
                maxX = originPos.getX() + rem;
                minY = originPos.getY() - half;
                maxY = originPos.getY() + rem;
                minZ = (hitFace == Direction.SOUTH) ? originPos.getZ() - (range - 1) : originPos.getZ();
                maxZ = (hitFace == Direction.SOUTH) ? originPos.getZ() : originPos.getZ() + (range - 1);
            }
            default -> {
                minX = originPos.getX() - half;
                maxX = originPos.getX() + rem;
                minY = originPos.getY() - half;
                maxY = originPos.getY() + rem;
                minZ = originPos.getZ() - half;
                maxZ = originPos.getZ() + rem;
            }
        }

        for (int x = minX; x <= maxX; x++) {
            for (int y = minY; y <= maxY; y++) {
                for (int z = minZ; z <= maxZ; z++) {
                    BlockPos targetPos = new BlockPos(x, y, z);
                    mineSingleBlock(level, player, targetPos, tool);
                }
            }
        }
    }

    private static void mineSingleBlock(ServerLevel level, ServerPlayer player, BlockPos pos, ItemStack tool) {
        BlockState state = level.getBlockState(pos);
        if (state.isAir()) {
            return;
        }

        BlockEntity blockEntity = level.getBlockEntity(pos);
        List<ItemStack> drops = Block.getDrops(state, level, pos, blockEntity, player, tool);

        // 「なんでも回収できる」仕様: ドロップがないブロック（岩盤など）でもアイテム化して回収
        if (drops.isEmpty()) {
            ItemStack fallbackItem = new ItemStack(state.getBlock().asItem());
            if (!fallbackItem.isEmpty()) {
                drops = List.of(fallbackItem);
            }
        }

        // ブロック消去（硬度無視・BlockEntity処理）
        level.removeBlockEntity(pos);
        level.setBlock(pos, Blocks.AIR.defaultBlockState(), 3);

        // ドロップ品をプレイヤーのインベントリに回収
        for (ItemStack drop : drops) {
            if (drop.isEmpty()) continue;
            if (!player.getInventory().add(drop)) {
                ItemEntity itemEntity = new ItemEntity(level, player.getX(), player.getY() + 0.5, player.getZ(), drop);
                itemEntity.setNoPickUpDelay();
                level.addFreshEntity(itemEntity);
            }
        }
    }
}
