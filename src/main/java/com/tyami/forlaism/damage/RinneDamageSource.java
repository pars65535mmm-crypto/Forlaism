package com.tyami.forlaism.damage;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;

/**
 * 輪廻ダメージ。
 *
 * - 無敵時間貫通
 * - 最大HP削り
 * - 回復阻害
 * - 不死身貫通
 *
 * 実処理は RinneDamageMixin 側で行う。
 */
public final class RinneDamageSource {

    public static final ResourceKey<DamageType> RINNE =
            ResourceKey.create(
                    Registries.DAMAGE_TYPE,
                    new ResourceLocation("forlaism", "rinne")
            );

    private RinneDamageSource() {
    }

    public static DamageSource of(Level level, Entity attacker) {
        return new DamageSource(
                level.registryAccess()
                        .registryOrThrow(Registries.DAMAGE_TYPE)
                        .getHolderOrThrow(RINNE),
                attacker
        );
    }
}