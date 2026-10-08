package com.tyami.forlaism.world;

import com.tyami.forlaism.Forlaism;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.dimension.DimensionType;

/**
 * Fakedreamディメンションの定義。
 *
 * 実体はデータパック側
 *   data/forlaism/dimension/fakedream.json
 *   data/forlaism/dimension_type/fakedream.json
 * にある。
 *
 * このクラスはResourceKeyを提供するだけ。
 *
 * ─ 特徴 ────────────────────────────────────
 *   ・空は常に真昼間
 *   ・空が薄くピンク色
 *   ・洞窟が存在しない
 *   ・ほぼバニラの草原
 *   ・ほぼ高原（平均Y=120）
 *   ・海なし・川なし
 *   ・死んでも帰れない（その場リスポーン）
 *   ・(0, 120, 0) に桜の大木
 */
public final class FakedreamDimension {

    /** Fakedreamディメンションのキー。 */
    public static final ResourceKey<Level> FAKEDREAM_LEVEL =
            ResourceKey.create(
                    Registries.DIMENSION,
                    new ResourceLocation(Forlaism.MOD_ID, "fakedream")
            );

    /** Fakedreamディメンションタイプのキー。 */
    public static final ResourceKey<DimensionType> FAKEDREAM_TYPE =
            ResourceKey.create(
                    Registries.DIMENSION_TYPE,
                    new ResourceLocation(Forlaism.MOD_ID, "fakedream")
            );

    private FakedreamDimension() {
    }
}