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
// 愚者の石 + rough_steel ×200 → メタアダマンタイン
register(new QuantumFusionRecipe(
        List.of(
                new ItemStack(Items.STONE_OF_FOOL.get()),
                new ItemStack(Items.ROUGH_STEEL.get(), 200)
        ),
        100_000_000,  // 100 MFE
        0,            // FO不要
        1200,         // 60秒
        new ItemStack(Items.META_ADAMANTINE.get()),
        1.0F          // 成功率100%
));
// カカオ豆×64 + 大罪の石×64 + 1MFE → カッカオカッカオ
register(new QuantumFusionRecipe(
        List.of(
                new ItemStack(net.minecraft.world.item.Items.COCOA_BEANS, 64),
                new ItemStack(Items.STONE_OF_SIN.get(), 64)
        ),
        1_000_000,  // 1 MFE
        0,
        400,        // 20秒
        new ItemStack(Items.CACACA.get()),
        1.0F
));
// 鉄ブロック×64 ×9 + 大罪の石×2 + 10MFE → 双刀 ムラツクモ
register(new QuantumFusionRecipe(
        List.of(
                new ItemStack(net.minecraft.world.item.Items.IRON_BLOCK, 575),
                new ItemStack(Items.STONE_OF_SIN.get(), 2)
        ),
        10_000_000,  // 10 MFE
        0,           // FO不要
        800,         // 40秒
        new ItemStack(Items.MURATSUKUMO.get()),
        1.0F         // 成功率100%
));
    // ネザライトブロック×128 + 賢者の石 + ネザースター×64 + 1000MFE → 臨界核
    register(new QuantumFusionRecipe(
            List.of(
                    new ItemStack(net.minecraft.world.item.Items.NETHERITE_BLOCK, 128),
                    new ItemStack(Items.TRUE_SAGE_STONE.get()),
                    new ItemStack(net.minecraft.world.item.Items.NETHER_STAR, 64)
            ),
            1_000_000_000,  // 1000 MFE
            0,
            1200,           // 60秒
            new ItemStack(Items.CRITICAL_CORE.get()),
            1.0F
    ));

    // 臨界核 + パラドックス + メタアダマンタイン + 1GFE → 核
    register(new QuantumFusionRecipe(
            List.of(
                    new ItemStack(Items.CRITICAL_CORE.get()),
                    new ItemStack(Items.PARADOX.get()),
                    new ItemStack(Items.META_ADAMANTINE.get())
            ),
            1_000_000_000,  // 1 GFE (int限界近く)
            0,
            2400,           // 120秒
            new ItemStack(Items.NUCLEUS.get()),
            1.0F
    ));
    // リンネディウム + コンニャクダイト + 100MFE → チェレンコフコンニャクダイト
register(new QuantumFusionRecipe(
        List.of(
                new ItemStack(Items.RINNEDIUM_INGOT.get()),
                new ItemStack(Items.KONNYAKU_DAITE.get())
        ),
        100_000_000,  // 100 MFE
        0,            // FO不要
        600,          // 30秒
        new ItemStack(Items.CHERENKOV_KONNYAKU_DAITE.get()),
        1.0F          // 成功率100%
));


}
}