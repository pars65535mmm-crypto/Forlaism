package com.tyami.forlaism.util;

import net.minecraft.world.item.ItemStack;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 死亡時ドロップから MasterpieceClock を退避する一時キャッシュ。
 *
 * ※ 元々は com.tyami.forlaism.mixin パッケージにあったが、
 *    Mixin専用パッケージ内のクラスは他クラスから直接参照できないため、
 *    util パッケージへ移動した。
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