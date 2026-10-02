package com.tyami.forlaism.annihilation;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;

/**
 * GMB 消去専用ダメージソース。
 */
public final class GMBEraseDamageSource {

    public static final ResourceKey<DamageType> GMB_ERASE =
            ResourceKey.create(
                    Registries.DAMAGE_TYPE,
                    new ResourceLocation("forlaism", "erase")
            );

    private GMBEraseDamageSource() {
    }

    public static DamageSource of(Level level, Entity attacker) {
        return new DamageSource(
                level.registryAccess()
                        .registryOrThrow(Registries.DAMAGE_TYPE)
                        .getHolderOrThrow(GMB_ERASE),
                attacker
        );
    }
}