package com.tyami.forlaism.effect;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;

/**
 * orcliam (空回り) エフェクト。
 *
 * ジルスレイトを所持している間発動する。
 * 視界に濃密で現実のような霧がかかり、10ブロック以上先が見えなくなる。
 */
public class OrcliamMobEffect extends MobEffect {

    public OrcliamMobEffect() {
        super(MobEffectCategory.NEUTRAL, 0xD0D7DE);
    }
}
