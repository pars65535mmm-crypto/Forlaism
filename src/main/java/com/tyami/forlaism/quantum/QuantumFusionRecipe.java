package com.tyami.forlaism.quantum;

import net.minecraft.world.item.ItemStack;
import java.util.List;

/**
 * 量子融合のレシピ定義。
 *
 * 後から登録所に add() するだけで増やせる。
 */
public record QuantumFusionRecipe(
        List<ItemStack> inputs,   // 必要なアイテム（Fに投入）
        int requiredFE,           // 必要FE
        int requiredFO,           // 必要FO (mB)
        int durationTicks,        // 加工時間
        ItemStack output,         // 出力
        float successRate         // 成功率 0.0〜1.0
) {
    public QuantumFusionRecipe {
        inputs = List.copyOf(inputs);
        output = output.copy();
    }
}