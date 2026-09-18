package com.tyami.forlaism.item;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.Tiers;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import javax.annotation.Nullable;
import java.util.List;

/**
 * 刃こぼれしたナイフ。
 *
 * - 攻撃力 15
 * - 攻撃速度 25
 * - 耐久値 3
 *
 * fo_knifeを壊れるまで使い続けると入手。
 */
public class RKnifeItem extends SwordItem {

    public RKnifeItem(Properties properties) {
        super(
                Tiers.NETHERITE,
                14,      // 攻撃力 15
                24.0F,  // 攻撃速度 25相当
                properties.durability(3)
        );
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level,
                                List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.literal("§7刃こぼれしてボロボロだ…"));
        tooltip.add(Component.literal("§8残り耐久: " + (stack.getMaxDamage() - stack.getDamageValue())));
    }
}