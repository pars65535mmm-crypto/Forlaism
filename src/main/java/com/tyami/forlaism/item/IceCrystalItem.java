package com.tyami.forlaism.item;

import com.tyami.watelib.item.IAnimatedTextItem;
import com.tyami.watelib.text.AnimatedText;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import javax.annotation.Nullable;
import java.util.List;

/**
 * 氷結結晶。
 *
 * フラスラティアの素材となる極寒の結晶。
 */
public class IceCrystalItem extends Item implements IAnimatedTextItem {

    public IceCrystalItem(Properties properties) {
        super(properties.fireResistant().rarity(Rarity.RARE));
    }

    @Override
    public AnimatedText createAnimatedName(ItemStack stack) {
        return AnimatedText.of("氷結結晶")
                .wave(2.0F, 0.15F, 0.40F)
                .gradient(0xFFFFFFFF, 0xFF88DDFF, 0xFFFFFFFF)
                .gradientSpeed(0.6F)
                .gradientPhase(0.5F);
    }



    @Override
    public boolean isFoil(ItemStack stack) {
        return true;
    }
}