package com.tyami.forlaism.block.entity;

import com.tyami.forlaism.registry.BlockEntities;
import com.tyami.forlaism.registry.Items;
import com.tyami.forlaism.util.CustomEnergyStorage;

import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;

public class ReactorBlockEntity extends Tier2MachineBlockEntity {

    public static final int PROCESS_TIME = 1280;

    private static final int ENERGY_CAPACITY = 6_400_000;
    private static final int MAX_ENERGY_RECEIVE = 500_000;

    public ReactorBlockEntity(BlockPos pos, BlockState state) {
        super(
                BlockEntities.FORLAISM_REACTOR.get(),
                pos,
                state,

                s -> s.is(Items.INCOMPLETE_FORLAISM_STAR_CORE.get()),

                new ItemStack(Items.FORLAISM_STAR_CORE.get()),

                PROCESS_TIME,

                20000,

                10000,

                "block.forlaism.forlaism_reactor"
        );

        this.energyStorage =
                new CustomEnergyStorage(
                        ENERGY_CAPACITY,
                        MAX_ENERGY_RECEIVE
                );
    }
}