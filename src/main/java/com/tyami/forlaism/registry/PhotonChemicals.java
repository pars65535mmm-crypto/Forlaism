package com.tyami.forlaism.registry;

import com.tyami.forlaism.Forlaism;
import mekanism.api.MekanismAPI;
import mekanism.api.chemical.gas.Gas;
import mekanism.api.chemical.gas.GasBuilder;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

/** Mekanism Gas registration for the transportable photon substance. */
public final class PhotonChemicals {
    private PhotonChemicals() {}
    public static final DeferredRegister<Gas> GASES = DeferredRegister.create(MekanismAPI.GAS_REGISTRY_NAME, Forlaism.MOD_ID);
    public static final RegistryObject<Gas> PHOTON = GASES.register("photon", () -> new Gas(
            GasBuilder.builder(new ResourceLocation(Forlaism.MOD_ID, "item/photon")).tint(0xFFF27A)
    ));
}
