package com.tyami.forlaism.world;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class TimeAccelerationManager {

    private static final Map<ResourceKey<Level>, Map<BlockPos, AcceleratedEntry>> ACCELERATED_BLOCKS = new ConcurrentHashMap<>();

    public static class AcceleratedEntry {
        public int remainingTicks;
        public final int speedMultiplier;

        public AcceleratedEntry(int remainingTicks, int speedMultiplier) {
            this.remainingTicks = remainingTicks;
            this.speedMultiplier = speedMultiplier;
        }
    }

    public static boolean isAccelerated(Level level, BlockPos pos) {
        Map<BlockPos, AcceleratedEntry> map = ACCELERATED_BLOCKS.get(level.dimension());
        return map != null && map.containsKey(pos);
    }

    public static boolean accelerate(Level level, BlockPos pos, int durationTicks, int speedMultiplier) {
        if (level.isClientSide) return false;
        Map<BlockPos, AcceleratedEntry> map = ACCELERATED_BLOCKS.computeIfAbsent(level.dimension(), k -> new ConcurrentHashMap<>());
        if (map.containsKey(pos)) {
            return false; // 重複無効
        }
        map.put(pos.immutable(), new AcceleratedEntry(durationTicks, speedMultiplier));
        return true;
    }

    public static void onLevelTick(Level level) {
        if (level.isClientSide || !(level instanceof ServerLevel serverLevel)) return;

        Map<BlockPos, AcceleratedEntry> map = ACCELERATED_BLOCKS.get(level.dimension());
        if (map == null || map.isEmpty()) return;

        map.entrySet().removeIf(entry -> {
            BlockPos pos = entry.getKey();
            AcceleratedEntry data = entry.getValue();

            if (data.remainingTicks <= 0 || level.isEmptyBlock(pos)) {
                return true;
            }

            data.remainingTicks--;

            BlockState state = level.getBlockState(pos);
            BlockEntity blockEntity = level.getBlockEntity(pos);

            int extraTicks = data.speedMultiplier - 1; // 16倍速なら追加で15回tick

            if (blockEntity != null) {
                tickBlockEntity(serverLevel, pos, state, blockEntity, extraTicks);
            } else if (state.isRandomlyTicking()) {
                for (int i = 0; i < extraTicks; i++) {
                    state.randomTick(serverLevel, pos, serverLevel.getRandom());
                }
            }

            // パーティクル演出
            if (serverLevel.getRandom().nextFloat() < 0.4F) {
                serverLevel.sendParticles(
                        ParticleTypes.ENCHANT,
                        pos.getX() + 0.5D,
                        pos.getY() + 0.5D,
                        pos.getZ() + 0.5D,
                        3,
                        0.3D,
                        0.3D,
                        0.3D,
                        0.05D
                );
            }

            return false;
        });
    }

    @SuppressWarnings("unchecked")
    private static <T extends BlockEntity> void tickBlockEntity(ServerLevel level, BlockPos pos, BlockState state, T blockEntity, int extraTicks) {
        BlockEntityType<T> type = (BlockEntityType<T>) blockEntity.getType();
        BlockEntityTicker<T> ticker = state.getTicker(level, type);
        if (ticker != null) {
            for (int i = 0; i < extraTicks; i++) {
                if (blockEntity.isRemoved()) break;
                ticker.tick(level, pos, state, blockEntity);
            }
        }
    }
}
