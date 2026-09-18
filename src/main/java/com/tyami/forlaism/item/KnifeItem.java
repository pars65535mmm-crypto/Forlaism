package com.tyami.forlaism.item;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.Tier;
import net.minecraft.world.item.Tiers;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import javax.annotation.Nullable;
import java.util.List;

public class KnifeItem extends SwordItem {

    public KnifeItem(Properties properties) {
        super(
                Tiers.IRON,
                1,      // 攻撃力 2 (SwordItemは+1されるのでベース1)
                -1.4F,  // 攻撃速度 3.0相当
                properties
        );
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level,
                                List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.literal("§7ただの包丁。"));
    }
}