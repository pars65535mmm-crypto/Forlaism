package com.tyami.forlaism.event;

import com.tyami.forlaism.registry.Items;
import net.minecraft.world.item.EnchantedBookItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.EnchantmentInstance;
import net.minecraftforge.event.AnvilUpdateEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;

@Mod.EventBusSubscriber
public class DreamBookHandler {

    private static final Random RANDOM = new Random();
    private static final int ENCHANT_COUNT = 10;
    private static final int MIN_LEVEL = 1;
    private static final int MAX_LEVEL = 20;

    @SubscribeEvent
    public static void onAnvilUpdate(AnvilUpdateEvent event) {
        ItemStack left = event.getLeft();
        ItemStack right = event.getRight();

        // 右側が夢幻の書かチェック
        if (right.isEmpty() || !right.is(Items.DREAM_BOOK.get())) {
            return;
        }

        // 左側が空なら終了
        if (left.isEmpty()) {
            return;
        }

        // ランダムで10種のエンチャントを生成
        List<Enchantment> allEnchantments = new ArrayList<>(ForgeRegistries.ENCHANTMENTS.getValues());
        List<Enchantment> selectedEnchantments = new ArrayList<>();

        // エンチャントをランダムに10種選択（重複なし）
        for (int i = 0; i < ENCHANT_COUNT && !allEnchantments.isEmpty(); i++) {
            int index = RANDOM.nextInt(allEnchantments.size());
            Enchantment ench = allEnchantments.remove(index);

            // 呪い（Curse）は除外（任意）
            if (ench.isCurse()) {
                i--;
                continue;
            }

            selectedEnchantments.add(ench);
        }

        // エンチャントがない場合は終了（念のため）
        if (selectedEnchantments.isEmpty()) {
            event.setCanceled(true);
            return;
        }

        // エンチャントをアイテムに適用
        ItemStack result = left.copy();

        // 既存のエンチャントを取得（上書きではなく追加）
        Map<Enchantment, Integer> existingEnchants = EnchantmentHelper.getEnchantments(result);

        for (Enchantment ench : selectedEnchantments) {
            int level = RANDOM.nextInt(MAX_LEVEL - MIN_LEVEL + 1) + MIN_LEVEL; // 1〜20
            // 既存のエンチャントより低いレベルならスキップ（任意）
            if (existingEnchants.containsKey(ench) && existingEnchants.get(ench) >= level) {
                continue;
            }
            existingEnchants.put(ench, level);
        }

        // エンチャントをセット
        EnchantmentHelper.setEnchantments(existingEnchants, result);

        // コスト設定（エンチャント数 × 5 + レベル合計 / 2）
        int totalLevels = existingEnchants.values().stream().mapToInt(Integer::intValue).sum();
        int cost = existingEnchants.size() * 5 + totalLevels / 2;
        cost = Math.min(cost, 100); // 最大100に抑える

        event.setOutput(result);
        event.setCost(cost);
        event.setMaterialCost(1);
    }
}