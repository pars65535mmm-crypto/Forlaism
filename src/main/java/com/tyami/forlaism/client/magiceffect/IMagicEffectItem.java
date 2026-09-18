package com.tyami.forlaism.client.magiceffect;

import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * 幻想的な魔法エフェクトを持つアイテムが実装するインターフェース。
 */
public interface IMagicEffectItem {

    /**
     * 指定されたItemStackに対するエフェクトスタイルを返す。
     * null を返した場合、または style.isEnabled() が false の場合はエフェクトは描画されない。
     *
     * @param stack 対象のItemStack
     * @return 魔法エフェクトスタイル、または null
     */
    @Nullable
    MagicEffectStyle getMagicEffect(@NotNull ItemStack stack);
}
