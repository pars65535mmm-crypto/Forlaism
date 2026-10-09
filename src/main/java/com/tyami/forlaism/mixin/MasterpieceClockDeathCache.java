package com.tyami.forlaism.mixin;

import net.minecraft.world.item.ItemStack;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 死亡時ドロップから MasterpieceClock を退避する一時キャッシュ。
 */
public final class MasterpieceClockDeathCache {

    private static final Map<UUID, ItemStack> CACHE = new ConcurrentHashMap<>();

    private MasterpieceClockDeathCache() {
    }

    public static void store(UUID uuid, ItemStack stack) {
        CACHE.put(uuid, stack);
    }

    public static ItemStack consume(UUID uuid) {
        return CACHE.remove(uuid);
    }
}