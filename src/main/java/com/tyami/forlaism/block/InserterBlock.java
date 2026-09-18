package com.tyami.forlaism.block;

import com.tyami.forlaism.block.entity.InserterBlockEntity;
import com.tyami.forlaism.registry.BlockEntities;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraftforge.network.NetworkHooks;
import org.jetbrains.annotations.Nullable;

public class InserterBlock extends BaseEntityBlock {

    public InserterBlock(BlockBehaviour.Properties properties) {
        super(properties);
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new InserterBlockEntity(pos, state);
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player,
                                 InteractionHand hand, BlockHitResult hit) {

        if (!level.isClientSide) {
            BlockEntity entity = level.getBlockEntity(pos);

            if (entity instanceof InserterBlockEntity inserter) {

                // =========================================================
                // 量子融合モード時: GUIを開かず、メッセージだけ表示
                // =========================================================
                if (inserter.isQuantumFusionMode()) {
                    player.displayClientMessage(
                            Component.literal("§dこの搬入機は §b量子融合機 §dとして覚醒している…"),
                            true
                    );
                    return InteractionResult.SUCCESS;
                }

                // 通常モード: GUIを開く
                if (player instanceof ServerPlayer serverPlayer) {
                    NetworkHooks.openScreen(serverPlayer, inserter, pos);
                }
            }
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return createTickerHelper(type, BlockEntities.FORLAISM_INSERTER.get(), InserterBlockEntity::tick);
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean isMoving) {
        if (state.getBlock() != newState.getBlock()) {
            BlockEntity blockEntity = level.getBlockEntity(pos);
            if (blockEntity instanceof InserterBlockEntity inserterEntity) {
                inserterEntity.drops();
            }
        }
        super.onRemove(state, level, pos, newState, isMoving);
    }
}