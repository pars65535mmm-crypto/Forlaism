package com.tyami.forlaism.ironsspells;

import com.tyami.forlaism.Forlaism;
import io.redspace.ironsspellbooks.api.attribute.MagicRangedAttribute;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

/**
 * 刻属性（Koku）用の属性登録。
 *
 * koku_spell_power : 刻魔法の威力
 * koku_magic_resist: 刻魔法への耐性
 */
public class ForlaismKokuAttribute {

    public static final DeferredRegister<Attribute> ATTRIBUTES =
            DeferredRegister.create(ForgeRegistries.ATTRIBUTES, Forlaism.MOD_ID);

    public static final RegistryObject<Attribute> KOKU_SPELL_POWER =
            ATTRIBUTES.register("koku_spell_power",
                    () -> new MagicRangedAttribute(
                            "attribute.forlaism.koku_spell_power",
                            1.0D, -100, 100
                    ).setSyncable(true));

    public static final RegistryObject<Attribute> KOKU_MAGIC_RESIST =
            ATTRIBUTES.register("koku_magic_resist",
                    () -> new MagicRangedAttribute(
                            "attribute.forlaism.koku_magic_resist",
                            1.0D, -100, 100
                    ).setSyncable(true));

    public static void register(IEventBus eventBus) {
        ATTRIBUTES.register(eventBus);
    }
}