package com.tyami.forlaism.item;

import com.tyami.forlaism.entity.WoodArrowEntity;

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
 * 木の矢。
 *
 * - ダメージ 3（バニラ矢の半分）
 * - 木材だけで量産可能
 * - 回収可能
 */
public class WoodArrowItem extends ArrowItem {

    public WoodArrowItem(Properties properties) {
        super(properties);
    }

    @Override
    public AbstractArrow createArrow(Level level, ItemStack stack, LivingEntity shooter) {
        WoodArrowEntity arrow = new WoodArrowEntity(level, shooter);
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
        tooltip.add(Component.literal("§7木だけで作られた簡素な矢。")
                .withStyle(ChatFormatting.GRAY));
    }
}