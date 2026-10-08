package com.tyami.forlaism.item;

import com.tyami.watelib.effect.Waveform;
import com.tyami.watelib.item.IAnimatedTextItem;
import com.tyami.watelib.text.AnimatedText;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/**
 * ヒヒイロカネ。
 *
 * ピンクに輝きながら、ふわふわ揺れる名前で表示する。
 */
public class HihiirokaneItem extends Item implements IAnimatedTextItem {

    public HihiirokaneItem(Properties properties) {
        super(properties);
    }

    @Override
    public AnimatedText createAnimatedName(ItemStack stack) {
        return AnimatedText.of("ヒヒイロカネ")
                .dropIn(50.0F, 12.0F, 3.0F)
                // ピンク -> 白 -> ピンク の動くグラデ
                .gradient(0xFFFF69B4, 0xFFFFFFFF, 0xFFFF69B4)
                .gradientSpeed(0.8F)
                .gradientPhase(0.6F)
                // キラキラ
                .sparkle(0xFFFFFFFF, 3.5F, 0.8F)
                // ピンクの発光
                .glow(0x60FF69B4, 2);
    }
}