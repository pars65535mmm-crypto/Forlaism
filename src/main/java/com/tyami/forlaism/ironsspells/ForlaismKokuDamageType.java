package com.tyami.forlaism.ironsspells;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.damagesource.DamageType;

/**
 * 刻属性（Koku）用のDamageTypeキー。
 *
 * データパック側の damage_type/koku_magic.json と紐づく。
 */
public class ForlaismKokuDamageType {

    public static final ResourceKey<DamageType> KOKU_MAGIC =
            ResourceKey.create(
                    Registries.DAMAGE_TYPE,
                    new ResourceLocation("forlaism", "koku_magic")
            );

    private ForlaismKokuDamageType() {
    }
}