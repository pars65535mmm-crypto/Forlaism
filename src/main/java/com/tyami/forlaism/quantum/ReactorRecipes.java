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

        // ここにじゃんじゃん追加していく
    }
}