package com.tyami.forlaism.registry;

import com.tyami.forlaism.Forlaism;
import com.tyami.forlaism.effect.OrcliamMobEffect;
import net.minecraft.world.effect.MobEffect;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModEffects {

    public static final DeferredRegister<MobEffect> MOB_EFFECTS =
            DeferredRegister.create(ForgeRegistries.MOB_EFFECTS, Forlaism.MOD_ID);

    public static final RegistryObject<MobEffect> ORCLIAM =
            MOB_EFFECTS.register("orcliam", OrcliamMobEffect::new);
}
