package com.tyami.forlaism.item;

import com.tyami.forlaism.client.magiceffect.IMagicEffectItem;
import com.tyami.forlaism.client.magiceffect.MagicEffectStyle;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.Tiers;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import javax.annotation.Nullable;
import java.util.List;

/**
 * 儀式の短剣。
 *
 * - 攻撃力 1
 * - 攻撃速度 1
 * - 耐久値 ∞ (壊れない)
 */
public class KnifeOfSacrificeItem extends SwordItem implements IMagicEffectItem {

    public static final MagicEffectStyle EFFECT_STYLE = MagicEffectStyle.builder()
            .color(0x80AA0000) // 深い血の赤
            .intensity(1.4f)
            .scale(1.0f)
            .speed(0.4f)
            .ringCount(0)
            .particleCount(0)
            .flares(true)
            .coreGlow(true)
            .build();

    public KnifeOfSacrificeItem(Properties properties) {
        super(
                Tiers.WOOD,
                0,      // 攻撃力 1 (ベース1 + 0)
                -3.0F,  // 攻撃速度 1.0相当 (ベース4.0 - 3.0)
                properties.stacksTo(1).fireResistant()
        );
    }

    @Override
    public MagicEffectStyle getMagicEffect(ItemStack stack) {
        return EFFECT_STYLE;
    }

    /**
     * 耐久値を減らさない（∞）。
     */
    @Override
    public boolean isDamageable(ItemStack stack) {
        return false;
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return true;
    }

    @Override
    public void appendHoverText(
            ItemStack stack,
            @Nullable Level level,
            List<Component> tooltip,
            TooltipFlag flag
    ) {
        tooltip.add(Component.literal("§4生贄を捧げるための短剣。")
                .withStyle(ChatFormatting.DARK_RED));
        tooltip.add(Component.literal("§7その刃は決して鈍らない。")
                .withStyle(ChatFormatting.GRAY));
    }
}