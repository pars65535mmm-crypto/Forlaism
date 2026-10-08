package com.tyami.forlaism.registry;

import com.tyami.forlaism.Forlaism;

import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.item.alchemy.Potion;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

/**
 * Forlaism の独自ポーション。
 *
 * 「目覚め薬」用の Potion を定義。
 * 効果自体は AwakeningPotionItem.use() 側で処理するので、
 * ここではダミーの効果を入れておく（バニラのツールチップ表示が空にならないように）。
 */
public class ModPotions {

    public static final DeferredRegister<Potion> POTIONS =
            DeferredRegister.create(ForgeRegistries.POTIONS, Forlaism.MOD_ID);

    /**
     * 目覚めのポーション。
     *
     * 効果は AwakeningPotionItem.use() で処理する。
     * バニラのポーションシステムに乗せるためのダミー。
     */
    public static final RegistryObject<Potion> AWAKENING =
            POTIONS.register("awakening",
                    () -> new Potion("awakening"));
}