package com.tyami.forlaism.damage;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;

/**
 * 月光ダメージ。
 *
 * - 防御貫通
 * - エンチャント無視
 * - 耐性貫通
 *
 * 実処理は mixin (LivingEntityMixin) 側で行う。
 */
public final class MoonlightDamageSource {

    public static final ResourceKey<DamageType> MOONLIGHT =
            ResourceKey.create(
                    Registries.DAMAGE_TYPE,
                    new ResourceLocation("forlaism", "moonlight")
            );

    private MoonlightDamageSource() {
    }

    public static DamageSource of(Level level, Entity attacker) {
        return new DamageSource(
                level.registryAccess()
                        .registryOrThrow(Registries.DAMAGE_TYPE)
                        .getHolderOrThrow(MOONLIGHT),
                attacker
        );
    }
}