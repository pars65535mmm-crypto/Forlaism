package com.tyami.forlaism.quantum;

import com.tyami.forlaism.registry.Items;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

/**
 * 量子融合レシピの登録所。
 * ここに add() していけば増やせる。
 */
public final class QuantumFusionRecipes {

    private static final List<QuantumFusionRecipe> RECIPES = new ArrayList<>();

    private QuantumFusionRecipes() {
    }

    public static void register(QuantumFusionRecipe recipe) {
        RECIPES.add(recipe);
    }

    public static List<QuantumFusionRecipe> getAll() {
        return List.copyOf(RECIPES);
    }

    // =====================================================
    // 初期レシピ（あとで増やしてOK）
    // =====================================================

    public static void bootstrap() {

    // 賢者の石? + 微睡む九十九の夢 → 賢者の石
    register(new QuantumFusionRecipe(
            List.of(
                    new ItemStack(Items.SAGE_STONE.get()),
                    new ItemStack(Items.MADOROMU.get())
            ),
            100_000,   // 100 KFE
            0,         // FO不要
            200,       // 10秒
            new ItemStack(Items.TRUE_SAGE_STONE.get()),
            1.0F       // 成功率100%
    ));

    // null_sugar x64 + 賢者の石 + 1MFE → Re糖分の剣
register(new QuantumFusionRecipe(
        List.of(
                new ItemStack(Items.NULL_SUGAR.get(), 64),
                new ItemStack(Items.TRUE_SAGE_STONE.get())
        ),
        1_000_000,  // 1 MFE
        0,          // FO不要
        400,        // 20秒
        new ItemStack(Items.SWREQUIMET_BLADE.get()),
        1.0F        // 成功率100%
));
// イノチノカケラ x512 + 賢者の石 x64 + 10MFE → 愚者の石
register(new QuantumFusionRecipe(
        List.of(
                new ItemStack(Items.INOCHI_NO_KAKERA.get(), 512),
                new ItemStack(Items.TRUE_SAGE_STONE.get(), 64)
        ),
        10_000_000,  // 10 MFE
        0,           // FO不要
        600,         // 30秒
        new ItemStack(Items.STONE_OF_FOOL.get()),
        1.0F         // 成功率100%
));



}
}