package com.tyami.forlaism.registry;

import com.tyami.forlaism.Forlaism;
import com.tyami.forlaism.enchantment.MugenEnchantment;
import com.tyami.forlaism.enchantment.PercentageReturnEnchantment;
import com.tyami.forlaism.enchantment.RevengeEnchantment;

import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModEnchantments {

    public static final DeferredRegister<Enchantment> ENCHANTMENTS =
            DeferredRegister.create(ForgeRegistries.ENCHANTMENTS, Forlaism.MOD_ID);

    public static final RegistryObject<Enchantment> PERCENTAGE_RETURN =
            ENCHANTMENTS.register("percentage_return",
                    PercentageReturnEnchantment::new);

    public static final RegistryObject<Enchantment> REVENGE =
            ENCHANTMENTS.register("revenge",
                    RevengeEnchantment::new);

    public static final RegistryObject<Enchantment> MUGEN =
            ENCHANTMENTS.register("mugen",
                    MugenEnchantment::new);
}