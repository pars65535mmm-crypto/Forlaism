package com.tyami.forlaism.block.entity;
import com.tyami.forlaism.registry.BlockEntities;
import com.tyami.forlaism.registry.Items;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
public class ConcentratorBlockEntity extends Tier2MachineBlockEntity {
    public ConcentratorBlockEntity(BlockPos pos, BlockState state) { super(BlockEntities.FORLAISM_CONCENTRATOR.get(), pos, state, s -> s.is(Items.FORLAISM_IRON_ALLOY.get()), new ItemStack(Items.FORLAISM_CONCENTRATED_IRON_ALLOY.get()), 100, 20, 1000, "block.forlaism.forlaism_concentrator"); }
}
