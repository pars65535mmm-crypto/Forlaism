package com.tyami.forlaism.world;

import com.tyami.forlaism.Forlaism;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.dimension.DimensionType;

/**
 * Dreamディメンションの定義。
 *
 * 実体はデータパック側
 *   data/forlaism/dimension/dream.json
 *   data/forlaism/dimension_type/dream.json
 * にある。
 *
 * このクラスはResourceKeyを提供するだけ。
 */
public final class DreamDimension {

    /** Dreamディメンションのキー。 */
    public static final ResourceKey<Level> DREAM_LEVEL =
            ResourceKey.create(
                    Registries.DIMENSION,
                    new ResourceLocation(Forlaism.MOD_ID, "dream")
            );

    /** Dreamディメンションタイプのキー。 */
    public static final ResourceKey<DimensionType> DREAM_TYPE =
            ResourceKey.create(
                    Registries.DIMENSION_TYPE,
                    new ResourceLocation(Forlaism.MOD_ID, "dream")
            );

    private DreamDimension() {
    }
}