package com.tyami.forlaism.block;

import com.tyami.forlaism.block.entity.QuantumTransferDeviceOutBlockEntity;
import com.tyami.forlaism.item.QuantumWrenchItem;
import com.tyami.forlaism.registry.BlockEntities;
import com.tyami.forlaism.registry.ModTags;
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
import org.jetbrains.annotations.Nullable;

public class QuantumTransferDeviceOutBlock extends BaseEntityBlock {

    public QuantumTransferDeviceOutBlock(BlockBehaviour.Properties properties) {
        super(properties);
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new QuantumTransferDeviceOutBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return createTickerHelper(type, BlockEntities.QUANTUM_TRANSFER_DEVICE_OUT.get(),
                QuantumTransferDeviceOutBlockEntity::tick);
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player,
                                 InteractionHand hand, BlockHitResult hit) {

        if (!(level.getBlockEntity(pos) instanceof QuantumTransferDeviceOutBlockEntity be)) {
            return InteractionResult.PASS;
        }

        ItemStack held = player.getItemInHand(hand);
        boolean isWrench = held.getItem() instanceof QuantumWrenchItem
                || held.is(ModTags.Items.WRENCH);

        if (!level.isClientSide) {
            if (!isWrench) {
                // レンチ無し → 現在のネットワーク番号を表示
                player.displayClientMessage(
                        Component.literal("§d[Quantum Out] §fNetwork: §e" + be.getNetwork()),
                        true
                );
            } else {
                // レンチあり → 切り替え
                int delta = player.isSecondaryUseActive() ? -1 : 1;
                be.cycleNetwork(delta);
                player.displayClientMessage(
                        Component.literal("§d[Quantum Out] §fNetwork: §e" + be.getNetwork()),
                        true
                );
            }
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }
}