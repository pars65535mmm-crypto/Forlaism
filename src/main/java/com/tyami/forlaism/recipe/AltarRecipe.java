package com.tyami.forlaism.recipe;

import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

/**
 * 儀式祭壇のレシピ。
 */
public class AltarRecipe {

    private final String[] pattern;
    private final java.util.Map<Character, ItemStack> key;
    private final ItemStack output;
    private final List<ItemStack> shapelessInputs;

    private AltarRecipe(String[] pattern,
                        java.util.Map<Character, ItemStack> key,
                        List<ItemStack> shapelessInputs,
                        ItemStack output) {
        this.pattern = pattern;
        this.key = key;
        this.shapelessInputs = shapelessInputs;
        this.output = output;
    }

    public static AltarRecipe shaped(String[] pattern,
                                     java.util.Map<Character, ItemStack> key,
                                     ItemStack output) {
        if (pattern.length != 9) {
            throw new IllegalArgumentException("pattern must be 9 rows");
        }
        for (String row : pattern) {
            if (row.length() != 9) {
                throw new IllegalArgumentException("each row must be 9 chars");
            }
        }
        return new AltarRecipe(pattern, key, null, output);
    }

    public static AltarRecipe shapeless(List<ItemStack> inputs, ItemStack output) {
        return new AltarRecipe(null, null, inputs, output);
    }

    public boolean matches(ItemStack[] items) {
        if (pattern != null) {
            return matchesShaped(items);
        } else {
            return matchesShapeless(items);
        }
    }

    private boolean matchesShaped(ItemStack[] items) {
        for (int row = 0; row < 9; row++) {
            for (int col = 0; col < 9; col++) {
                int slot = row * 9 + col;
                char c = pattern[row].charAt(col);
                ItemStack actual = items[slot];

                if (c == ' ') {
                    if (!actual.isEmpty()) return false;
                    continue;
                }

                if (c == '?') {
                    if (actual.isEmpty()) return false;
                    continue;
                }

                ItemStack expected = key.get(c);
                if (expected == null) return false;
                if (!ItemStack.isSameItemSameTags(actual, expected)) return false;
                if (actual.getCount() < expected.getCount()) return false;
            }
        }
        return true;
    }

    private boolean matchesShapeless(ItemStack[] items) {
        List<ItemStack> pool = new ArrayList<>();
        for (ItemStack s : items) {
            if (!s.isEmpty()) pool.add(s.copy());
        }

        for (ItemStack need : shapelessInputs) {
            int remaining = need.getCount();

            for (ItemStack have : pool) {
                if (remaining <= 0) break;
                if (ItemStack.isSameItemSameTags(have, need)) {
                    int take = Math.min(remaining, have.getCount());
                    have.shrink(take);
                    remaining -= take;
                }
            }

            if (remaining > 0) return false;
        }

        for (ItemStack s : pool) {
            if (!s.isEmpty()) return false;
        }

        return true;
    }

    public ItemStack getOutput() {
        return output;
    }
}