package com.tyami.forlaism.item;

import com.tyami.forlaism.client.magiceffect.IMagicEffectItem;
import com.tyami.forlaism.client.magiceffect.MagicEffectStyle;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import top.theillusivec4.curios.api.type.capability.ICurioItem;

public class HaloOfTheAdventItem extends Item implements ICurioItem, IMagicEffectItem {

    public static final MagicEffectStyle EFFECT_STYLE = MagicEffectStyle.builder()
            .color(0x80B0FFFF) // 神秘的な純白〜シアンの光（Tooltip準拠）
            .intensity(1.8f)   // 神々しい強い発光
            .scale(1.1f)       // アイテムアイコンを優雅に包むサイズ
            .speed(0.6f)       // 荘厳でゆったりとした回転
            .ringCount(0)      // 魔法陣なし
            .particleCount(0)  // 公転粒子なし（中央に1つの✦のみ）
            .flares(true)      // 中央に凛と輝く4芒星「✦」
            .coreGlow(true)    // 神聖な中心光オーラ
            .build();

    public HaloOfTheAdventItem(Properties properties) {
        super(properties);
    }

    @Override
    public MagicEffectStyle getMagicEffect(ItemStack stack) {
        return EFFECT_STYLE;
    }
}