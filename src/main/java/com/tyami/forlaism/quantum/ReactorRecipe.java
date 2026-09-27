package com.tyami.forlaism.quantum;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 輪廻再転式量子融合炉（Reactor マルチブロック）のレシピ定義。
 *
 * 複数素材 + FE + 出力をひとまとめに持つ。
 * QuantumFusionRecipe と似てるけど、
 * Reactor は「中央の縦穴にアイテムを落とす」方式なので別物。
 */
public record ReactorRecipe(
        Map<Item, Integer> inputs,
        int requiredFE,
        ItemStack output
) {
    public ReactorRecipe {
        // 挿入順保持 & 不変コピー
        inputs = java.util.Collections.unmodifiableMap(new LinkedHashMap<>(inputs));
        output = output.copy();
    }

    /** Map を可変長引数っぽく組み立てるヘルパ。 */
    public static Map<Item, Integer> inputs(Object... pairs) {
        if (pairs.length % 2 != 0) {
            throw new IllegalArgumentException("inputs must be Item,Integer pairs");
        }
        Map<Item, Integer> map = new LinkedHashMap<>();
        for (int i = 0; i < pairs.length; i += 2) {
            map.put((Item) pairs[i], (Integer) pairs[i + 1]);
        }
        return map;
    }
}