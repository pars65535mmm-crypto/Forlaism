package com.tyami.forlaism.registry;

import com.tyami.forlaism.Forlaism;
import com.tyami.forlaism.fluid.BaseFluidType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraftforge.common.SoundActions;
import net.minecraftforge.fluids.FluidType;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import org.joml.Vector3f;

public class FluidTypes {

    public static final DeferredRegister<FluidType> FLUID_TYPES =
            DeferredRegister.create(
                    ForgeRegistries.Keys.FLUID_TYPES,
                    Forlaism.MOD_ID
            );

    public static final ResourceLocation CULTURE_STILL = new ResourceLocation(Forlaism.MOD_ID, "block/forlaism_culture_still");
    public static final ResourceLocation CULTURE_FLOW = new ResourceLocation(Forlaism.MOD_ID, "block/forlaism_culture_flow");

    public static final ResourceLocation POLYCRYSTAL_STILL = new ResourceLocation(Forlaism.MOD_ID, "block/forlaism_polycrystal_solution_still");
    public static final ResourceLocation POLYCRYSTAL_FLOW = new ResourceLocation(Forlaism.MOD_ID, "block/forlaism_polycrystal_solution_flow");

    public static final ResourceLocation FO_STILL = new ResourceLocation(Forlaism.MOD_ID, "block/fo_still");
    public static final ResourceLocation FO_FLOW = new ResourceLocation(Forlaism.MOD_ID, "block/fo_flow");

    public static final ResourceLocation WATER_OVERLAY = new ResourceLocation("minecraft", "block/water_overlay");

    public static final RegistryObject<FluidType> FORLAISM_CULTURE =
            FLUID_TYPES.register(
                    "forlaism_culture",
                    () -> new BaseFluidType(
                            CULTURE_STILL,
                            CULTURE_FLOW,
                            WATER_OVERLAY,
                            0xFFFFFFFF,
                            new Vector3f(0.3F, 0.8F, 0.9F),
                            FluidType.Properties.create()
                                    .descriptionId("block.forlaism.forlaism_culture")
                                    .canSwim(true)
                                    .canDrown(true)
                                    .supportsBoating(true)
                                    .sound(SoundActions.BUCKET_FILL, SoundEvents.BUCKET_FILL)
                                    .sound(SoundActions.BUCKET_EMPTY, SoundEvents.BUCKET_EMPTY)
                    )
            );

    public static final RegistryObject<FluidType> FORLAISM_POLYCRYSTAL_SOLUTION =
            FLUID_TYPES.register(
                    "forlaism_polycrystal_solution",
                    () -> new BaseFluidType(
                            POLYCRYSTAL_STILL,
                            POLYCRYSTAL_FLOW,
                            WATER_OVERLAY,
                            0xFFFFFFFF,
                            new Vector3f(0.2F, 0.9F, 1.0F),
                            FluidType.Properties.create()
                                    .descriptionId("block.forlaism.forlaism_polycrystal_solution")
                                    .canSwim(true)
                                    .canDrown(true)
                                    .supportsBoating(true)
                                    .sound(SoundActions.BUCKET_FILL, SoundEvents.BUCKET_FILL)
                                    .sound(SoundActions.BUCKET_EMPTY, SoundEvents.BUCKET_EMPTY)
                    )
            );

    public static final RegistryObject<FluidType> FO =
            FLUID_TYPES.register(
                    "fo",
                    () -> new BaseFluidType(
                            FO_STILL,
                            FO_FLOW,
                            WATER_OVERLAY,
                            0xFFFFFFFF,
                            new Vector3f(0.0F, 0.9F, 1.0F),
                            FluidType.Properties.create()
                                    .descriptionId("block.forlaism.fo")
                                    .canSwim(true)
                                    .canDrown(true)
                                    .supportsBoating(true)
                                    .sound(SoundActions.BUCKET_FILL, SoundEvents.BUCKET_FILL)
                                    .sound(SoundActions.BUCKET_EMPTY, SoundEvents.BUCKET_EMPTY)
                    )
            );


public static final ResourceLocation ONSEN_STILL =
        new ResourceLocation(Forlaism.MOD_ID, "block/onsen_still");

public static final ResourceLocation ONSEN_FLOW =
        new ResourceLocation(Forlaism.MOD_ID, "block/onsen_flow");

public static final RegistryObject<FluidType> ONSEN =
        FLUID_TYPES.register(
                "onsen",
                () -> new BaseFluidType(
                        ONSEN_STILL,
                        ONSEN_FLOW,
                        WATER_OVERLAY,
                        0xFFFFFFFF,
                        new Vector3f(0.85F, 0.85F, 0.75F),
                        FluidType.Properties.create()
                                .descriptionId("block.forlaism.onsen")
                                .canSwim(true)
                                .canDrown(true)
                                .supportsBoating(true)
                                .sound(SoundActions.BUCKET_FILL, SoundEvents.BUCKET_FILL)
                                .sound(SoundActions.BUCKET_EMPTY, SoundEvents.BUCKET_EMPTY)
                )
        );

}