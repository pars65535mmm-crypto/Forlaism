package com.tyami.forlaism.item;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterials;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import javax.annotation.Nullable;
import java.util.List;

/**
 * 魔法使いの帽子。
 *
 * - バニラ防具枠（頭スロット）で装備可能
 * - 効果なし（純粋に見た目だけの帽子）
 * - 防具性能は革と同じ（防御1）
 */
public class MahouTsukaiNoBoushiItem extends ArmorItem {

    public MahouTsukaiNoBoushiItem(Properties properties) {
        super(
                ArmorMaterials.LEATHER,
                ArmorItem.Type.HELMET,
                properties
                        .stacksTo(1)
                        .durability(80) // 革の帽子と同じ耐久
        );
    }

    @Override
    public void appendHoverText(
            ItemStack stack,
            @Nullable Level level,
            List<Component> tooltip,
            TooltipFlag flag
    ) {
        tooltip.add(Component.literal("§6魔法使いの証。")
                .withStyle(ChatFormatting.GOLD));
        tooltip.add(Component.literal("§7被るだけで、少し賢くなった気がする。")
                .withStyle(ChatFormatting.GRAY));
    }
}