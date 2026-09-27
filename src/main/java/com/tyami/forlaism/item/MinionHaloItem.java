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
 * 眷属の光輪。
 *
 * - 頭装備（バニラ防具スロット HEAD）
 * - 耐久値 21億
 * - 即死耐性（MinionHaloResistanceHandler で処理）
 * - ランダム突進 / 遠距離タイプはバックステップ（AI側で処理）
 */
public class MinionHaloItem extends ArmorItem {

    /** 耐久値 21億。 */
    public static final int DURABILITY = 2_100_000_000;

    public MinionHaloItem(Properties properties) {
        super(
                ArmorMaterials.NETHERITE,
                ArmorItem.Type.HELMET,
                properties
                        .stacksTo(1)
                        .durability(DURABILITY)
                        .fireResistant()
        );
    }

    @Override
    public void appendHoverText(
            ItemStack stack,
            @Nullable Level level,
            List<Component> tooltip,
            TooltipFlag flag
    ) {
        tooltip.add(Component.literal("§5眷属に授けられし光輪。")
                .withStyle(ChatFormatting.DARK_PURPLE));
        tooltip.add(Component.literal("§7着けし者は死を拒む。")
                .withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.literal("§8耐久: 21億")
                .withStyle(ChatFormatting.DARK_GRAY));
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return true;
    }
}