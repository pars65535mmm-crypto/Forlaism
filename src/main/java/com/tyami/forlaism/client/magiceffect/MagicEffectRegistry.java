package com.tyami.forlaism.client.magiceffect;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Function;
import java.util.function.Predicate;

/**
 * 既存アイテムや他Modのアイテムに対して外部からMagicEffectStyleを登録・提供するためのレジストリ。
 */
public final class MagicEffectRegistry {

    private static final Map<Item, Function<ItemStack, MagicEffectStyle>> ITEM_STYLES = new ConcurrentHashMap<>();
    private static final CopyOnWriteArrayList<PredicateProviderEntry> PREDICATE_STYLES = new CopyOnWriteArrayList<>();

    private record PredicateProviderEntry(Predicate<ItemStack> predicate, Function<ItemStack, MagicEffectStyle> provider) {}

    private MagicEffectRegistry() {
    }

    /**
     * 特定のアイテムに対して固定のMagicEffectStyleを登録する。
     */
    public static void register(@NotNull Item item, @NotNull MagicEffectStyle style) {
        ITEM_STYLES.put(item, stack -> style);
    }

    /**
     * 特定のアイテムに対してItemStackに応じたMagicEffectStyleを提供する関数を登録する。
     */
    public static void register(@NotNull Item item, @NotNull Function<ItemStack, MagicEffectStyle> provider) {
        ITEM_STYLES.put(item, provider);
    }

    /**
     * 特定の条件（Predicate）を満たすItemStackに対してMagicEffectStyleを提供する関数を登録する。
     */
    public static void register(@NotNull Predicate<ItemStack> predicate, @NotNull Function<ItemStack, MagicEffectStyle> provider) {
        PREDICATE_STYLES.add(new PredicateProviderEntry(predicate, provider));
    }

    /**
     * 特定の条件（Predicate）を満たすItemStackに対して固定のMagicEffectStyleを登録する。
     */
    public static void register(@NotNull Predicate<ItemStack> predicate, @NotNull MagicEffectStyle style) {
        register(predicate, stack -> style);
    }

    /**
     * 登録されたスタイルを検索して返す。
     */
    @Nullable
    public static MagicEffectStyle findStyle(@NotNull ItemStack stack) {
        if (stack.isEmpty()) {
            return null;
        }

        // 1. アイテム単位の登録をチェック
        Function<ItemStack, MagicEffectStyle> itemProvider = ITEM_STYLES.get(stack.getItem());
        if (itemProvider != null) {
            MagicEffectStyle style = itemProvider.apply(stack);
            if (style != null) {
                return style;
            }
        }

        // 2. Predicateの登録を順次チェック
        for (PredicateProviderEntry entry : PREDICATE_STYLES) {
            if (entry.predicate().test(stack)) {
                MagicEffectStyle style = entry.provider().apply(stack);
                if (style != null) {
                    return style;
                }
            }
        }

        return null;
    }

    /**
     * 登録解除（主にデバッグ・リセット用）
     */
    public static void clear() {
        ITEM_STYLES.clear();
        PREDICATE_STYLES.clear();
    }
}
