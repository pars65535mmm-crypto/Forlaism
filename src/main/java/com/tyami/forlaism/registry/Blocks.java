package com.tyami.forlaism.registry;

import com.tyami.forlaism.Forlaism;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import net.minecraft.world.level.block.SoundType;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import com.tyami.forlaism.block.AethericGeneratorBlock;
import com.tyami.forlaism.block.QuantumTransferDeviceInBlock;
import com.tyami.forlaism.block.QuantumTransferDeviceOutBlock;
import com.tyami.forlaism.block.QuantumTransferDeviceInBlock;
import com.tyami.forlaism.block.QuantumTransferDeviceOutBlock;

import net.minecraftforge.registries.RegistryObject;

public class Blocks {

    public static final DeferredRegister<Block> BLOCKS =
            DeferredRegister.create(ForgeRegistries.BLOCKS, Forlaism.MOD_ID);

    public static final RegistryObject<LiquidBlock> FORLAISM_CULTURE =
            BLOCKS.register(
                    "forlaism_culture",
                    () -> new LiquidBlock(
                            Fluids.FORLAISM_CULTURE,
                            BlockBehaviour.Properties.of()
                                    .mapColor(MapColor.WATER)
                                    .replaceable()
                                    .noCollission()
                                    .strength(100.0F)
                                    .pushReaction(PushReaction.DESTROY)
                                    .noLootTable()
                                    .liquid()
                                    .sound(SoundType.EMPTY)
                    )
            );

    public static final RegistryObject<LiquidBlock> FORLAISM_POLYCRYSTAL_SOLUTION =
            BLOCKS.register(
                    "forlaism_polycrystal_solution",
                    () -> new LiquidBlock(
                            Fluids.FORLAISM_POLYCRYSTAL_SOLUTION,
                            BlockBehaviour.Properties.of()
                                    .mapColor(MapColor.COLOR_LIGHT_BLUE)
                                    .replaceable()
                                    .noCollission()
                                    .strength(100.0F)
                                    .pushReaction(PushReaction.DESTROY)
                                    .noLootTable()
                                    .liquid()
                                    .sound(SoundType.EMPTY)
                    )
            );

    public static final RegistryObject<LiquidBlock> FO =
            BLOCKS.register(
                    "fo",
                    () -> new LiquidBlock(
                            Fluids.FO,
                            BlockBehaviour.Properties.of()
                                    .mapColor(MapColor.COLOR_CYAN)
                                    .replaceable()
                                    .noCollission()
                                    .strength(100.0F)
                                    .pushReaction(PushReaction.DESTROY)
                                    .noLootTable()
                                    .liquid()
                                    .sound(SoundType.EMPTY)
                    )
            );

public static final RegistryObject<LiquidBlock> ONSEN =
        BLOCKS.register(
                "onsen",
                () -> new LiquidBlock(
                        Fluids.ONSEN,
                        BlockBehaviour.Properties.of()
                                .mapColor(MapColor.COLOR_LIGHT_GRAY)
                                .replaceable()
                                .noCollission()
                                .strength(100.0F)
                                .pushReaction(PushReaction.DESTROY)
                                .noLootTable()
                                .liquid()
                                .sound(SoundType.EMPTY)
                )
        );

    public static final RegistryObject<Block> FORLAISM_ORE =
            BLOCKS.register("forlaism_ore", () ->
                    new Block(BlockBehaviour.Properties.of()
                            .mapColor(MapColor.STONE)
                            .requiresCorrectToolForDrops()
                            .strength(3.0F, 3.0F)
                            .sound(SoundType.STONE)));

    public static final RegistryObject<Block> DEEPSLATE_FORLAISM_ORE =
            BLOCKS.register("deepslate_forlaism_ore", () ->
                    new Block(BlockBehaviour.Properties.of()
                            .mapColor(MapColor.DEEPSLATE)
                            .requiresCorrectToolForDrops()
                            .strength(4.5F, 3.0F)
                            .sound(SoundType.DEEPSLATE)));

    public static final RegistryObject<Block> FORLAISM_CRYSTAL_BLOCK =
            BLOCKS.register("forlaism_crystal_block", () ->
                    new Block(BlockBehaviour.Properties.of()
                            .mapColor(MapColor.COLOR_LIGHT_BLUE)
                            .requiresCorrectToolForDrops()
                            .strength(4.0F, 6.0F)
                            .sound(SoundType.AMETHYST)));

    public static final RegistryObject<Block> STEEL_BLOCK =
            BLOCKS.register("steel_block", () ->
                    new Block(BlockBehaviour.Properties.of()
                            .mapColor(MapColor.METAL)
                            .requiresCorrectToolForDrops()
                            .strength(5.0F, 6.0F)
                            .sound(SoundType.METAL)));

    public static final RegistryObject<Block> FORLAISM_HIGH_CRYSTAL_BLOCK =
            BLOCKS.register("forlaism_high_crystal_block", () ->
                    new Block(BlockBehaviour.Properties.of()
                            .mapColor(MapColor.COLOR_CYAN)
                            .requiresCorrectToolForDrops()
                            .strength(50.0F, 1200.0F)
                            .sound(SoundType.NETHERITE_BLOCK)));

    public static final RegistryObject<Block> FORLAISM_FUSION_MACHINE =
            BLOCKS.register("forlaism_fusion_machine", () ->
                    new com.tyami.forlaism.block.FusionMachineBlock(BlockBehaviour.Properties.of()
                            .mapColor(MapColor.METAL)
                            .requiresCorrectToolForDrops()
                            .strength(4.0F, 6.0F)
                            .sound(SoundType.METAL)));

    public static final RegistryObject<Block> FORLAISM_INSERTER =
            BLOCKS.register("forlaism_inserter", () ->
                    new com.tyami.forlaism.block.InserterBlock(BlockBehaviour.Properties.of()
                            .mapColor(MapColor.METAL)
                            .requiresCorrectToolForDrops()
                            .strength(4.0F, 6.0F)
                            .sound(SoundType.METAL)));

    // Tier 2 machines
    public static final RegistryObject<Block> FORLAISM_ALLOY_MACHINE =
            BLOCKS.register("forlaism_alloy_machine", () ->
                    new com.tyami.forlaism.block.AlloyMachineBlock(BlockBehaviour.Properties.of()
                            .mapColor(MapColor.METAL)
                            .requiresCorrectToolForDrops()
                            .strength(4.0F, 6.0F)
                            .sound(SoundType.METAL)));

    public static final RegistryObject<Block> FORLAISM_CONCENTRATOR =
            BLOCKS.register("forlaism_concentrator", () ->
                    new com.tyami.forlaism.block.ConcentratorBlock(BlockBehaviour.Properties.of()
                            .mapColor(MapColor.METAL)
                            .requiresCorrectToolForDrops()
                            .strength(4.0F, 6.0F)
                            .sound(SoundType.METAL)));

    public static final RegistryObject<Block> FORLAISM_REACTOR =
            BLOCKS.register("forlaism_reactor", () ->
                    new com.tyami.forlaism.block.ReactorBlock(BlockBehaviour.Properties.of()
                            .mapColor(MapColor.METAL)
                            .requiresCorrectToolForDrops()
                            .strength(4.0F, 6.0F)
                            .sound(SoundType.METAL)));


public static final RegistryObject<Block> AETHERIC_GENERATOR =
        BLOCKS.register("aetheric_generator", () ->
                new AethericGeneratorBlock(BlockBehaviour.Properties.of()
                        .mapColor(MapColor.METAL)
                        .requiresCorrectToolForDrops()
                        .strength(4.0F, 6.0F)
                        .sound(SoundType.METAL)));




public static final RegistryObject<Block> MADOROMU_BLOCK =
        BLOCKS.register("madoromu_block", () ->
                new com.tyami.forlaism.block.MadoromuBlock(
                        BlockBehaviour.Properties.of()
                                .mapColor(MapColor.COLOR_PURPLE)
                                .requiresCorrectToolForDrops()
                                .strength(40.0F, 12000.0F)
                                .sound(SoundType.AMETHYST)
                ));


public static final RegistryObject<Block> QUANTUM_TRANSFER_DEVICE_IN =
        BLOCKS.register("quantum_transfer_device_in", () ->
                new QuantumTransferDeviceInBlock(BlockBehaviour.Properties.of()
                        .mapColor(MapColor.COLOR_CYAN)
                        .requiresCorrectToolForDrops()
                        .strength(4.0F, 1200.0F)
                        .sound(SoundType.METAL)));

public static final RegistryObject<Block> QUANTUM_TRANSFER_DEVICE_OUT =
        BLOCKS.register("quantum_transfer_device_out", () ->
                new QuantumTransferDeviceOutBlock(BlockBehaviour.Properties.of()
                        .mapColor(MapColor.COLOR_MAGENTA)
                        .requiresCorrectToolForDrops()
                        .strength(4.0F, 1200.0F)
                        .sound(SoundType.METAL)));


    public static final RegistryObject<Block> DREAM_FLOOR =
            BLOCKS.register("dream_floor", () ->
                    new com.tyami.forlaism.block.DreamFloorBlock(
                            BlockBehaviour.Properties.of()
                                    .mapColor(MapColor.COLOR_BLACK)
                                    .strength(-1.0F, 3_600_000.0F) // 岩盤より硬い
                                    .noLootTable()
                                    .sound(SoundType.DEEPSLATE)
                    ));
    public static final RegistryObject<Block> WARP_SENDER =
            BLOCKS.register("warp_sender", () ->
                    new com.tyami.forlaism.block.WarpSenderBlock(
                            BlockBehaviour.Properties.of()
                                    .mapColor(MapColor.COLOR_CYAN)
                                    .requiresCorrectToolForDrops()
                                    .strength(4.0F, 1200.0F)
                                    .sound(SoundType.METAL)
                    ));
//гғ•гӮ©гғ©гғӘгӮ№гӮ¬гғ©гӮ№って何..?

public static final RegistryObject<Block> FORLAISM_GLASS =
        BLOCKS.register("forlaism_glass", () ->
                new net.minecraft.world.level.block.GlassBlock(
                        BlockBehaviour.Properties.of()
                                .mapColor(MapColor.COLOR_LIGHT_BLUE)
                                .strength(2.0F, 1200.0F)
                                .sound(SoundType.GLASS)
                                .noOcclusion()
                                .isValidSpawn((s, l, p, e) -> false)
                                .isRedstoneConductor((s, l, p) -> false)
                                .isSuffocating((s, l, p) -> false)
                                .isViewBlocking((s, l, p) -> false)
                ));

    public static final RegistryObject<Block> INFINITE_WATER_TANK =
            BLOCKS.register("infinite_water_tank", () ->
                    new com.tyami.forlaism.block.InfiniteWaterTankBlock(
                            BlockBehaviour.Properties.of()
                                    .mapColor(MapColor.COLOR_LIGHT_BLUE)
                                    .strength(2.0F, 1200.0F)
                                    .sound(SoundType.GLASS)
                                    .noOcclusion()
                                    .isValidSpawn((s, l, p, e) -> false)
                                    .isRedstoneConductor((s, l, p) -> false)
                                    .isSuffocating((s, l, p) -> false)
                                    .isViewBlocking((s, l, p) -> false)
                    ));

    public static final RegistryObject<Block> FIREPROOF_FORLAISM_GLASS =
            BLOCKS.register("fireproof_forlaism_glass", () ->
                    new net.minecraft.world.level.block.Block(
                            BlockBehaviour.Properties.of()
                                    .mapColor(MapColor.COLOR_BLACK)
                                    .strength(2.0F, 1200.0F)
                                    .sound(SoundType.GLASS)
                                    .noOcclusion()
                                    .isValidSpawn((s, l, p, e) -> false)
                                    .isRedstoneConductor((s, l, p) -> false)
                                    .isSuffocating((s, l, p) -> false)
                                    .isViewBlocking((s, l, p) -> false)
                    ));


    public static final RegistryObject<Block> INFINITE_LAVA_TANK =
            BLOCKS.register("infinite_lava_tank", () ->
                    new com.tyami.forlaism.block.InfiniteLavaTankBlock(
                            BlockBehaviour.Properties.of()
                                    .mapColor(MapColor.COLOR_ORANGE)
                                    .strength(2.0F, 1200.0F)
                                    .sound(SoundType.GLASS)
                                    .noOcclusion()
                                    .isValidSpawn((s, l, p, e) -> false)
                                    .isRedstoneConductor((s, l, p) -> false)
                                    .isSuffocating((s, l, p) -> false)
                                    .isViewBlocking((s, l, p) -> false)
                    ));

    public static final RegistryObject<Block> INFINITE_COBBLESTONE_TANK =
            BLOCKS.register("infinite_cobblestone_tank", () ->
                    new com.tyami.forlaism.block.InfiniteCobblestoneTankBlock(
                            BlockBehaviour.Properties.of()
                                    .mapColor(MapColor.STONE)
                                    .strength(2.0F, 1200.0F)
                                    .sound(SoundType.GLASS)
                                    .noOcclusion()
                                    .isValidSpawn((s, l, p, e) -> false)
                                    .isRedstoneConductor((s, l, p) -> false)
                                    .isSuffocating((s, l, p) -> false)
                                    .isViewBlocking((s, l, p) -> false)
                    ));

    public static final RegistryObject<Block> INFINITE_SOIL =
            BLOCKS.register("infinite_soil", () ->
                    new com.tyami.forlaism.block.InfiniteSoilBlock(
                            BlockBehaviour.Properties.of()
                                    .mapColor(MapColor.DIRT)
                                    .strength(2.0F, 1200.0F)
                                    .sound(SoundType.GRASS)
                                    .noOcclusion()
                                    .isValidSpawn((s, l, p, e) -> false)
                                    .isRedstoneConductor((s, l, p) -> false)
                                    .isSuffocating((s, l, p) -> false)
                                    .isViewBlocking((s, l, p) -> false)
                    ));

    // =========================================================
    // Fakedream: Fstone
    // =========================================================
    public static final RegistryObject<Block> FSTONE =
            BLOCKS.register("fstone", () ->
                    new com.tyami.forlaism.block.FstoneBlock(
                            BlockBehaviour.Properties.of()
                                    .mapColor(MapColor.STONE)
                                    .requiresCorrectToolForDrops()
                                    .strength(-1.0F, 3_600_000.0F) // 岩盤と同じ
                                    .noLootTable()
                                    .sound(SoundType.STONE)
                    ));

    // =========================================================
    // ヒヒイロカネ鉱石
    // メタアダマンタインのツルハシでのみ採掘可能
    // =========================================================
    public static final RegistryObject<Block> HIHIIROKANE_ORE =
            BLOCKS.register("hihiirokane_ore", () ->
                    new com.tyami.forlaism.block.HihiirokaneOreBlock(
                            BlockBehaviour.Properties.of()
                                    .mapColor(MapColor.STONE)
                                    .requiresCorrectToolForDrops()
                                    .strength(2.0F, 9.0F)
                                    .sound(SoundType.NETHERITE_BLOCK)));

}
