package com.tyami.forlaism.block.entity;

import com.tyami.forlaism.registry.BlockEntities;
import com.tyami.forlaism.registry.Items;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;

public class AlloyMachineBlockEntity extends Tier2MachineBlockEntity {

    public AlloyMachineBlockEntity(BlockPos pos, BlockState state) {
        super(
                BlockEntities.FORLAISM_ALLOY_MACHINE.get(),
                pos,
                state,
                s -> s.is(net.minecraft.world.item.Items.IRON_INGOT)
                        || s.is(Items.GARBAGE_METAL.get()),
                new ItemStack(Items.FORLAISM_IRON_ALLOY.get()),
                100,
                20,
                1000,
                "block.forlaism.forlaism_alloy_machine"
        );
    }

    @Override
    protected ItemStack getOutputForInput(ItemStack input) {
        if (input.is(Items.GARBAGE_METAL.get())) {
            return new ItemStack(Items.ADAMETAL.get());
        }

        return new ItemStack(Items.FORLAISM_IRON_ALLOY.get());
    }
}