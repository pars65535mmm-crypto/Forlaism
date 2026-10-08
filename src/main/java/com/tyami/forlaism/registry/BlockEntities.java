package com.tyami.forlaism.registry;

import com.tyami.forlaism.Forlaism;
import com.tyami.forlaism.block.entity.FusionMachineBlockEntity;
import com.tyami.forlaism.block.entity.InserterBlockEntity;
import com.tyami.forlaism.block.entity.QuantumTransferDeviceInBlockEntity;
import com.tyami.forlaism.block.entity.QuantumTransferDeviceOutBlockEntity;
import com.tyami.forlaism.block.entity.AlloyMachineBlockEntity;
import com.tyami.forlaism.block.entity.ConcentratorBlockEntity;
import com.tyami.forlaism.block.entity.ReactorBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import com.tyami.forlaism.block.entity.AethericGeneratorBlockEntity;
import com.tyami.forlaism.block.entity.QuantumTransferDeviceInBlockEntity;
import com.tyami.forlaism.block.entity.QuantumTransferDeviceOutBlockEntity;

public class BlockEntities {

    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(ForgeRegistries.BLOCK_ENTITY_TYPES, Forlaism.MOD_ID);

    public static final RegistryObject<BlockEntityType<FusionMachineBlockEntity>> FORLAISM_FUSION_MACHINE =
            BLOCK_ENTITIES.register("forlaism_fusion_machine", () ->
                    BlockEntityType.Builder.of(FusionMachineBlockEntity::new,
                            Blocks.FORLAISM_FUSION_MACHINE.get()).build(null));

    public static final RegistryObject<BlockEntityType<AlloyMachineBlockEntity>> FORLAISM_ALLOY_MACHINE =
            BLOCK_ENTITIES.register("forlaism_alloy_machine", () ->
                    BlockEntityType.Builder.of(AlloyMachineBlockEntity::new,
                            Blocks.FORLAISM_ALLOY_MACHINE.get()).build(null));

    public static final RegistryObject<BlockEntityType<ConcentratorBlockEntity>> FORLAISM_CONCENTRATOR =
            BLOCK_ENTITIES.register("forlaism_concentrator", () ->
                    BlockEntityType.Builder.of(ConcentratorBlockEntity::new,
                            Blocks.FORLAISM_CONCENTRATOR.get()).build(null));

    public static final RegistryObject<BlockEntityType<ReactorBlockEntity>> FORLAISM_REACTOR =
            BLOCK_ENTITIES.register("forlaism_reactor", () ->
                    BlockEntityType.Builder.of(ReactorBlockEntity::new,
                            Blocks.FORLAISM_REACTOR.get()).build(null));
    public static final RegistryObject<BlockEntityType<InserterBlockEntity>> FORLAISM_INSERTER = BLOCK_ENTITIES.register("forlaism_inserter", () ->
                    BlockEntityType.Builder.of(InserterBlockEntity::new,
                            Blocks.FORLAISM_INSERTER.get()).build(null));









public static final RegistryObject<BlockEntityType<AethericGeneratorBlockEntity>> AETHERIC_GENERATOR =
        BLOCK_ENTITIES.register("aetheric_generator", () ->
                BlockEntityType.Builder.of(
                        AethericGeneratorBlockEntity::new,
                        Blocks.AETHERIC_GENERATOR.get()
                ).build(null));
                


public static final RegistryObject<BlockEntityType<QuantumTransferDeviceInBlockEntity>> QUANTUM_TRANSFER_DEVICE_IN =
        BLOCK_ENTITIES.register("quantum_transfer_device_in", () ->
                BlockEntityType.Builder.of(QuantumTransferDeviceInBlockEntity::new,
                        Blocks.QUANTUM_TRANSFER_DEVICE_IN.get()).build(null));

public static final RegistryObject<BlockEntityType<QuantumTransferDeviceOutBlockEntity>> QUANTUM_TRANSFER_DEVICE_OUT =
        BLOCK_ENTITIES.register("quantum_transfer_device_out", () ->
                BlockEntityType.Builder.of(QuantumTransferDeviceOutBlockEntity::new,
                        Blocks.QUANTUM_TRANSFER_DEVICE_OUT.get()).build(null));

    public static final RegistryObject<BlockEntityType<com.tyami.forlaism.block.entity.WarpSenderBlockEntity>> WARP_SENDER =
            BLOCK_ENTITIES.register("warp_sender", () ->
                    BlockEntityType.Builder.of(
                            com.tyami.forlaism.block.entity.WarpSenderBlockEntity::new,
                            Blocks.WARP_SENDER.get()
                    ).build(null));

public static final RegistryObject<BlockEntityType<com.tyami.forlaism.block.entity.InfiniteWaterTankBlockEntity>> INFINITE_WATER_TANK =
        BLOCK_ENTITIES.register("infinite_water_tank", () ->
                BlockEntityType.Builder.of(
                        com.tyami.forlaism.block.entity.InfiniteWaterTankBlockEntity::new,
                        Blocks.INFINITE_WATER_TANK.get()
                ).build(null));
public static final RegistryObject<BlockEntityType<com.tyami.forlaism.block.entity.InfiniteLavaTankBlockEntity>> INFINITE_LAVA_TANK =
        BLOCK_ENTITIES.register("infinite_lava_tank", () ->
                BlockEntityType.Builder.of(
                        com.tyami.forlaism.block.entity.InfiniteLavaTankBlockEntity::new,
                        Blocks.INFINITE_LAVA_TANK.get()
                ).build(null));
public static final RegistryObject<BlockEntityType<com.tyami.forlaism.block.entity.InfiniteCobblestoneTankBlockEntity>> INFINITE_COBBLESTONE_TANK =
        BLOCK_ENTITIES.register("infinite_cobblestone_tank", () ->
                BlockEntityType.Builder.of(
                        com.tyami.forlaism.block.entity.InfiniteCobblestoneTankBlockEntity::new,
                        Blocks.INFINITE_COBBLESTONE_TANK.get()
                ).build(null));

public static final RegistryObject<BlockEntityType<com.tyami.forlaism.block.entity.InfiniteSoilBlockEntity>> INFINITE_SOIL =
        BLOCK_ENTITIES.register("infinite_soil", () ->
                BlockEntityType.Builder.of(
                        com.tyami.forlaism.block.entity.InfiniteSoilBlockEntity::new,
                        Blocks.INFINITE_SOIL.get()
                ).build(null));
}
