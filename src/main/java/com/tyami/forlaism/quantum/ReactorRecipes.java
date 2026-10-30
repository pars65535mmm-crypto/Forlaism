package com.tyami.forlaism.quantum;

import com.tyami.forlaism.registry.Items;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

/**
 * 輪廻再転式量子融合炉のレシピ登録所。
 *
 * ここに register() するだけで追加できる。
 */
public final class ReactorRecipes {

    private static final List<ReactorRecipe> RECIPES = new ArrayList<>();

    private ReactorRecipes() {
    }

    public static void register(ReactorRecipe recipe) {
        RECIPES.add(recipe);
    }

    public static List<ReactorRecipe> getAll() {
        return List.copyOf(RECIPES);
    }

    // =========================================================
    // 初期レシピ
    // =========================================================
    //
    // Forlaism.commonSetup から bootstrap() が呼ばれる想定。

    public static void bootstrap() {

        // =========================================================
        // レールガンブレード
        //   リンネディウム × 10
        //   チェレンコフコンニャクダイト × 10
        //   メタアダマンタイン × 20
        //   パラドックス × 1
        //   FE: 2,000,000,000 (2 GFE)
        // =========================================================
        register(new ReactorRecipe(
                ReactorRecipe.inputs(
                        Items.RINNEDIUM_INGOT.get(), 10,
                        Items.CHERENKOV_KONNYAKU_DAITE.get(), 10,
                        Items.META_ADAMANTINE.get(), 20,
                        Items.PARADOX.get(), 1
                ),
                2_000_000_000,
                new ItemStack(Items.CHERENKOV_META_ADAMANEDIUM_RAILGUN_BLADE.get())
        ));
// =========================================================
// 夢幻
//   夢幻の欠片 × 576
//   賢者の石 × 64
//   FE: 1,000,000,000 (1 GFE)
// =========================================================
register(new ReactorRecipe(
        ReactorRecipe.inputs(
                com.tyami.forlaism.registry.Items.DREAM_FRAGMENT.get(), 576,
                com.tyami.forlaism.registry.Items.TRUE_SAGE_STONE.get(), 64
        ),
        1_000_000_000,
        new ItemStack(com.tyami.forlaism.registry.Items.DREAM.get())
));

        // =========================================================
        // 500倍圧縮土
        // 入力: 土 ×500 + 想像 ×1
        // 出力: 500倍圧縮土 ×1
        // FE: 1,000,000
        // =========================================================



// =========================================================
// 微睡
//   微睡む九十九の夢の塊 × 512
//   草ブロック × 1
//   FE: 1,000,000,000 (1 GFE)
// =========================================================
register(new ReactorRecipe(
        ReactorRecipe.inputs(
                com.tyami.forlaism.registry.Items.MADOROMU_BLOCK.get(), 512,
                net.minecraft.world.item.Items.GRASS_BLOCK, 1
        ),
        1_000_000_000,
        new ItemStack(com.tyami.forlaism.registry.Items.MADOROMI.get())
));


        // =========================================================
        // 500倍圧縮土
        // 入力: 土 ×500 + 想像 ×1
        // FE: 1,000,000
        // 出力: 500倍圧縮土 ×1
        // =========================================================
        register(new ReactorRecipe(
                ReactorRecipe.inputs(
                        net.minecraft.world.item.Items.DIRT, 500,
                        com.tyami.forlaism.registry.Items.IMAGINATION.get(), 1
                ),
                1_000_000,
                new ItemStack(com.tyami.forlaism.registry.Items.COMPRESSED_DIRT.get())
        ));


    }
}