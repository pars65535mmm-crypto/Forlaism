package com.tyami.forlaism.event;

import com.tyami.forlaism.item.StarOreBookItem;
import com.tyami.forlaism.registry.Items;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AnvilMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.EnchantedBookItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.EnchantmentInstance;
import net.minecraftforge.event.AnvilUpdateEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.Map;

@Mod.EventBusSubscriber
public class StarOreBookHandler {

    @SubscribeEvent
    public static void onAnvilUpdate(AnvilUpdateEvent event) {
        ItemStack left = event.getLeft();
        ItemStack right = event.getRight();

        // 右側が星鉱の書かチェック
        if (right.isEmpty() || !right.is(Items.STAR_ORE_BOOK.get())) {
            return;
        }

        // 左側がエンチャントされていないor空なら終了
        if (left.isEmpty() || !left.isEnchanted()) {
            return;
        }

        // 既に最大ブーストに達しているかチェック（NBTで管理）
        int currentBoost = getBoostLevel(left);
        if (currentBoost >= StarOreBookItem.MAX_BOOST) {
            event.setCanceled(true);
            return;
        }

        // エンチャントを取得して+1
        Map<Enchantment, Integer> enchantments = EnchantmentHelper.getEnchantments(left);
        boolean anyBoosted = false;

        for (Map.Entry<Enchantment, Integer> entry : enchantments.entrySet()) {
            Enchantment ench = entry.getKey();
            int currentLevel = entry.getValue();
            int maxLevel = ench.getMaxLevel();

            // すでに最大レベルならスキップ（ただし星鉱の書の上限+10とは別）
            if (currentLevel >= maxLevel + StarOreBookItem.MAX_BOOST) {
                continue;
            }

            // +1
            enchantments.put(ench, currentLevel + 1);
            anyBoosted = true;
        }

        if (!anyBoosted) {
            event.setCanceled(true);
            return;
        }

        // 新しいアイテムを作成
        ItemStack result = left.copy();
        EnchantmentHelper.setEnchantments(enchantments, result);

        // ブースト回数を+1
        setBoostLevel(result, currentBoost + 1);

        // 金床のコスト設定（エンチャント数 × 10 + ブースト回数 × 5）
        int cost = enchantments.size() * 10 + (currentBoost + 1) * 5;
        event.setOutput(result);
        event.setCost(cost);
        event.setMaterialCost(1);
    }

    // === NBTヘルパー ===

    private static final String BOOST_TAG = "StarOreBoost";

    private static int getBoostLevel(ItemStack stack) {
        return stack.getOrCreateTag().getInt(BOOST_TAG);
    }

    private static void setBoostLevel(ItemStack stack, int level) {
        stack.getOrCreateTag().putInt(BOOST_TAG, level);
    }
}