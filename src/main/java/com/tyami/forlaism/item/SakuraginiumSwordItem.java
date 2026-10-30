package com.tyami.forlaism.item;

import com.tyami.watelib.item.IAnimatedTextItem;
import com.tyami.watelib.text.AnimatedText;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import javax.annotation.Nullable;
import java.util.List;

/**
 * サクラギニウムの剣。
 *
 * - 攻撃力 36
 * - 攻撃速度 1.6（ダイヤ相当）
 * - 耐久 101761
 */
public class SakuraginiumSwordItem extends SwordItem implements IAnimatedTextItem {

    /** 表示攻撃力。 */
    public static final float ATTACK_DAMAGE = 36.0F;

    public SakuraginiumSwordItem(Properties properties) {
        super(
                SakuraginiumTier.INSTANCE,
                (int) ATTACK_DAMAGE - 1, // SwordItemは+1されるので
                2.4F,
                properties
                        .stacksTo(1)
                        .durability(SakuraginiumTier.DURABILITY)
                        .fireResistant()
        );
    }

    @Override
    public AnimatedText createAnimatedName(ItemStack stack) {
        return AnimatedText.of("サクラギニウムの剣")
                .gradient(0xFFFFB7C5, 0xFFFFFFFF, 0xFFFF69B4)
                .gradientSpeed(0.8F)
                .gradientPhase(0.6F)
                .glow(0x60FF69B4, 2);
    }

    @Override
    public void appendHoverText(
            ItemStack stack,
            @Nullable Level level,
            List<Component> tooltip,
            TooltipFlag flag
    ) {
        tooltip.add(Component.literal("§dサクラニウムではない、サクラギニウムなのだ、たぶん")
                .withStyle(ChatFormatting.LIGHT_PURPLE));

    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return true;
    }
}