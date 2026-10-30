package com.tyami.forlaism.item;

import com.tyami.forlaism.entity.FireArrowEntity;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.item.ArrowItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import javax.annotation.Nullable;
import java.util.List;

/**
 * 火の矢。
 *
 * - ダメージ 5
 * - 着弾時に相手を燃やす（8秒）
 * - 回収可能
 */
public class FireArrowItem extends ArrowItem {

    public FireArrowItem(Properties properties) {
        super(properties);
    }

    @Override
    public AbstractArrow createArrow(Level level, ItemStack stack, LivingEntity shooter) {
        FireArrowEntity arrow = new FireArrowEntity(level, shooter);
        arrow.pickup = AbstractArrow.Pickup.ALLOWED;
        return arrow;
    }

    @Override
    public void appendHoverText(
            ItemStack stack,
            @Nullable Level level,
            List<Component> tooltip,
            TooltipFlag flag
    ) {
        tooltip.add(Component.literal("§c木の矢を火で炙ったもの。")
                .withStyle(ChatFormatting.RED));

    }
}