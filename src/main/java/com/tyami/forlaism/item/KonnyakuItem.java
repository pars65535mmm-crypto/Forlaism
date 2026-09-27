package com.tyami.forlaism.item;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.Level;

import javax.annotation.Nullable;
import java.util.List;

/**
 * こんにゃく。
 *
 * 回復0 / 隠し回復(満腹度)1。
 * 満腹度だけ回復する摩訶不思議な食べ物。
 */
public class KonnyakuItem extends Item {

    public KonnyakuItem(Properties properties) {
        super(properties.food(
                new FoodProperties.Builder()
                        .nutrition(0)          // 回復0
                        .saturationMod(1.0F)   // 隠し回復1
                        .alwaysEat()           // 満腹でも食べられる
                        .build()
        ));
    }

    @Override
    public UseAnim getUseAnimation(ItemStack stack) {
        return UseAnim.EAT;
    }

    @Override
    public SoundEvent getEatingSound() {
        return SoundEvents.GENERIC_EAT;
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level,
                                List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.literal("§7ぷるぷる。").withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.literal("§8鉄を斬る剣でも切れないらしい。").withStyle(ChatFormatting.DARK_GRAY));
    }
}