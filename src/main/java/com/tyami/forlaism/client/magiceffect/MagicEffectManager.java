package com.tyami.forlaism.client.magiceffect;

import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * ItemStackから適用すべきMagicEffectStyleを解決・取得するマネージャークラス。
 */
public final class MagicEffectManager {

    private MagicEffectManager() {
    }

    /**
     * 指定されたItemStackに対して有効なMagicEffectStyleを取得する。
     * エフェクトが無効、または設定されていない場合は null を返す。
     *
     * @param stack 対象のItemStack
     * @return 有効なMagicEffectStyle、または null
     */
    @Nullable
    public static MagicEffectStyle getStyle(@NotNull ItemStack stack) {
        if (stack.isEmpty()) {
            return null;
        }

        // 1. アイテム自身が IMagicEffectItem を実装している場合
        if (stack.getItem() instanceof IMagicEffectItem magicItem) {
            MagicEffectStyle style = magicItem.getMagicEffect(stack);
            if (style != null && style.isEnabled()) {
                return style;
            }
        }

        // 2. レジストリに登録されている場合
        MagicEffectStyle registeredStyle = MagicEffectRegistry.findStyle(stack);
        if (registeredStyle != null && registeredStyle.isEnabled()) {
            return registeredStyle;
        }

        return null;
    }

    /**
     * 指定されたItemStackが魔法エフェクトを持つかどうかを高速に判定する。
     */
    public static boolean hasEffect(@NotNull ItemStack stack) {
        return getStyle(stack) != null;
    }
}
