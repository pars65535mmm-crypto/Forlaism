package com.tyami.forlaism.block;

import com.tyami.forlaism.block.entity.QuantumTransferDeviceInBlockEntity;
import com.tyami.forlaism.block.entity.QuantumTransferDeviceOutBlockEntity;
import com.tyami.forlaism.item.QuantumWrenchItem;
import com.tyami.forlaism.quantum.QuantumTransferNetwork;
import com.tyami.forlaism.registry.BlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraftforge.common.Tags;
import org.jetbrains.annotations.Nullable;
import com.tyami.forlaism.item.QuantumWrenchItem;
import com.tyami.forlaism.registry.ModTags;

public class QuantumTransferDeviceInBlock extends BaseEntityBlock {

    public QuantumTransferDeviceInBlock(BlockBehaviour.Properties properties) {
        super(properties);
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new QuantumTransferDeviceInBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return createTickerHelper(type, BlockEntities.QUANTUM_TRANSFER_DEVICE_IN.get(),
                QuantumTransferDeviceInBlockEntity::tick);
    }

@Override
public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player,
                             InteractionHand hand, BlockHitResult hit) {

    if (!(level.getBlockEntity(pos) instanceof QuantumTransferDeviceInBlockEntity be)) {
        return InteractionResult.PASS;
    }

    ItemStack held = player.getItemInHand(hand);
    boolean isWrench = held.getItem() instanceof QuantumWrenchItem
            || held.is(ModTags.Items.WRENCH);

    if (!level.isClientSide) {
        if (!isWrench) {
            player.displayClientMessage(
                    Component.literal("§b[Quantum In] §fNetwork: §e" + be.getNetwork()),
                    true
            );
        } else {
            int delta = player.isSecondaryUseActive() ? -1 : 1;
            be.cycleNetwork(delta);
            player.displayClientMessage(
                    Component.literal("§b[Quantum In] §fNetwork: §e" + be.getNetwork()),
                    true
            );
        }
    }
    return InteractionResult.sidedSuccess(level.isClientSide);
}
}