package com.tyami.forlaism.damage;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;

/**
 * リベンジダメージ。
 *
 * 輪廻ダメージを超える超強力ダメージ。
 * - 無敵時間貫通
 * - 防具・エンチャント・耐性無視
 * - 最大HP削り（累積・固定）
 * - 回復阻害（永続）
 * - 不死身貫通
 * - 独自リベンジカウンター蓄積（100で強制執行）
 * - die(), discard(), remove() 妨害や、常時HP固定Mobの貫通消滅
 */
public final class RevengeDamageSource {

    public static final ResourceKey<DamageType> REVENGE =
            ResourceKey.create(
                    Registries.DAMAGE_TYPE,
                    new ResourceLocation("forlaism", "revenge")
            );

    private RevengeDamageSource() {
    }

    public static DamageSource of(Level level, Entity attacker) {
        return new DamageSource(
                level.registryAccess()
                        .registryOrThrow(Registries.DAMAGE_TYPE)
                        .getHolderOrThrow(REVENGE),
                attacker
        );
    }
}
