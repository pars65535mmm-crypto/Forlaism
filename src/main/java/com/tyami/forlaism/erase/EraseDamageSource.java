package com.tyami.forlaism.erase;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;

/**
 * OverOverNull専用ダメージソース。
 *
 * bypasses_invulnerability, bypasses_armor, bypasses_magic,
 * bypasses_cooldown, bypasses_effects を全て持つ。
 */
public final class EraseDamageSource {

    public static final ResourceKey<DamageType> ERASE =
            ResourceKey.create(
                    Registries.DAMAGE_TYPE,
                    new ResourceLocation("forlaism", "erase")
            );

    private EraseDamageSource() {
    }

    public static DamageSource of(Level level, Entity attacker) {
        return new DamageSource(
                level.registryAccess()
                        .registryOrThrow(Registries.DAMAGE_TYPE)
                        .getHolderOrThrow(ERASE),
                attacker
        );
    }
}