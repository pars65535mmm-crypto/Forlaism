package com.tyami.forlaism.registry;

import com.tyami.forlaism.Forlaism;
import net.minecraft.world.level.material.FlowingFluid;
import net.minecraft.world.level.material.Fluid;
import net.minecraftforge.fluids.ForgeFlowingFluid;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class Fluids {

    public static final DeferredRegister<Fluid> FLUIDS =
            DeferredRegister.create(ForgeRegistries.FLUIDS, Forlaism.MOD_ID);

    private static ForgeFlowingFluid.Properties makeCultureProperties() {
        return new ForgeFlowingFluid.Properties(
                FluidTypes.FORLAISM_CULTURE,
                () -> FORLAISM_CULTURE.get(),
                () -> FORLAISM_CULTURE_FLOWING.get()
        )
                .block(() -> Blocks.FORLAISM_CULTURE.get())
                .bucket(() -> Items.FORLAISM_CULTURE_BUCKET.get())
                .slopeFindDistance(4)
                .levelDecreasePerBlock(1);
    }

    public static final RegistryObject<FlowingFluid> FORLAISM_CULTURE =
            FLUIDS.register(
                    "forlaism_culture",
                    () -> new ForgeFlowingFluid.Source(
                            makeCultureProperties()
                    )
            );

    public static final RegistryObject<FlowingFluid> FORLAISM_CULTURE_FLOWING =
            FLUIDS.register(
                    "forlaism_culture_flowing",
                    () -> new ForgeFlowingFluid.Flowing(
                            makeCultureProperties()
                    )
            );

    private static ForgeFlowingFluid.Properties makePolycrystalSolutionProperties() {
        return new ForgeFlowingFluid.Properties(
                FluidTypes.FORLAISM_POLYCRYSTAL_SOLUTION,
                () -> FORLAISM_POLYCRYSTAL_SOLUTION.get(),
                () -> FORLAISM_POLYCRYSTAL_SOLUTION_FLOWING.get()
        )
                .block(() -> Blocks.FORLAISM_POLYCRYSTAL_SOLUTION.get())
                .bucket(() -> Items.FORLAISM_POLYCRYSTAL_SOLUTION_BUCKET.get())
                .slopeFindDistance(4)
                .levelDecreasePerBlock(1);
    }

    public static final RegistryObject<FlowingFluid> FORLAISM_POLYCRYSTAL_SOLUTION =
            FLUIDS.register(
                    "forlaism_polycrystal_solution",
                    () -> new ForgeFlowingFluid.Source(
                            makePolycrystalSolutionProperties()
                    )
            );

    public static final RegistryObject<FlowingFluid> FORLAISM_POLYCRYSTAL_SOLUTION_FLOWING =
            FLUIDS.register(
                    "forlaism_polycrystal_solution_flowing",
                    () -> new ForgeFlowingFluid.Flowing(
                            makePolycrystalSolutionProperties()
                    )
            );

    private static ForgeFlowingFluid.Properties makeFOProperties() {
        return new ForgeFlowingFluid.Properties(
                FluidTypes.FO,
                () -> FO.get(),
                () -> FO_FLOWING.get()
        )
                .block(() -> Blocks.FO.get())
                .bucket(() -> Items.FO_BUCKET.get())
                .slopeFindDistance(4)
                .levelDecreasePerBlock(1);
    }

    public static final RegistryObject<FlowingFluid> FO =
            FLUIDS.register(
                    "fo",
                    () -> new ForgeFlowingFluid.Source(
                            makeFOProperties()
                    )
            );

    public static final RegistryObject<FlowingFluid> FO_FLOWING =
            FLUIDS.register(
                    "fo_flowing",
                    () -> new ForgeFlowingFluid.Flowing(
                            makeFOProperties()
                    )
            );

private static ForgeFlowingFluid.Properties makeOnsenProperties() {
    return new ForgeFlowingFluid.Properties(
            FluidTypes.ONSEN,
            () -> ONSEN.get(),
            () -> ONSEN_FLOWING.get()
    )
            .block(() -> Blocks.ONSEN.get())
            .slopeFindDistance(4)
            .levelDecreasePerBlock(1);
}

public static final RegistryObject<FlowingFluid> ONSEN =
        FLUIDS.register(
                "onsen",
                () -> new ForgeFlowingFluid.Source(
                        makeOnsenProperties()
                )
        );

public static final RegistryObject<FlowingFluid> ONSEN_FLOWING =
        FLUIDS.register(
                "onsen_flowing",
                () -> new ForgeFlowingFluid.Flowing(
                        makeOnsenProperties()
                )
        );

}
