package com.tyami.forlaism.item;

import com.tyami.forlaism.client.magiceffect.IMagicEffectItem;
import com.tyami.forlaism.client.magiceffect.MagicEffectStyle;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import top.theillusivec4.curios.api.type.capability.ICurioItem;

public class HaloOfTheFirmamentItem extends Item implements ICurioItem, IMagicEffectItem {

    public static final MagicEffectStyle EFFECT_STYLE = MagicEffectStyle.builder()
            .color(0x80FFA500) // 燃えるような深紅橙〜黄金色の光（Tooltip準拠）
            .intensity(1.8f)   // 天穹を照らす力強い黄金の輝き
            .scale(1.1f)       // アイテムアイコンを優雅に包むサイズ
            .speed(0.7f)       // ゆったりとした優美な回転
            .ringCount(0)      // 魔法陣なし
            .particleCount(0)  // 公転粒子なし（中央に1つの✦のみ）
            .flares(true)      // 中央に凛と輝く太陽のような4芒星「✦」
            .coreGlow(true)    // 神聖な黄金の中心光オーラ
            .build();

    public HaloOfTheFirmamentItem(Properties properties) {
        super(properties);
    }

    @Override
    public MagicEffectStyle getMagicEffect(ItemStack stack) {
        return EFFECT_STYLE;
    }
}