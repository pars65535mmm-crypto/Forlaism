package com.tyami.forlaism.recipe;

import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 儀式祭壇のレシピ登録所。
 */
public final class AltarRecipes {

    private static final List<AltarRecipe> RECIPES = new ArrayList<>();

    private AltarRecipes() {
    }

    public static void register(AltarRecipe recipe) {
        RECIPES.add(recipe);
    }

    public static List<AltarRecipe> getAll() {
        return List.copyOf(RECIPES);
    }

    public static void bootstrap() {

        // =========================================================
        // サンプル1: 真ん中にダイヤブロック1個 → ネザライト
        // =========================================================
        {
            Map<Character, ItemStack> key = new HashMap<>();
            key.put('D', new ItemStack(net.minecraft.world.item.Items.DIAMOND_BLOCK));

            String[] pattern = {
                    "         ",
                    "         ",
                    "         ",
                    "         ",
                    "    D    ",
                    "         ",
                    "         ",
                    "         ",
                    "         ",
            };

            register(AltarRecipe.shaped(
                    pattern,
                    key,
                    new ItemStack(net.minecraft.world.item.Items.NETHERITE_INGOT)
            ));
        }

        // =========================================================
        // サンプル2: 金インゴット x4 → ダイヤ
        // =========================================================
        {
            List<ItemStack> inputs = new ArrayList<>();
            inputs.add(new ItemStack(net.minecraft.world.item.Items.GOLD_INGOT, 4));

            register(AltarRecipe.shapeless(
                    inputs,
                    new ItemStack(net.minecraft.world.item.Items.DIAMOND)
            ));
        }

        // =========================================================
        // サンプル3: 中央にエンダーパール8個（3x3の輪）→ エンダーアイ
        // =========================================================
        {
            Map<Character, ItemStack> key = new HashMap<>();
            key.put('P', new ItemStack(net.minecraft.world.item.Items.ENDER_PEARL));

            String[] pattern = {
                    "         ",
                    "         ",
                    "         ",
                    "   PPP   ",
                    "   P P   ",
                    "   PPP   ",
                    "         ",
                    "         ",
                    "         ",
            };

            register(AltarRecipe.shaped(
                    pattern,
                    key,
                    new ItemStack(net.minecraft.world.item.Items.ENDER_EYE, 4)
            ));
        }
    }
}