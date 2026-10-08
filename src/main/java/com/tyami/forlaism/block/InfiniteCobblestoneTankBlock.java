package com.tyami.forlaism.block;

import com.tyami.forlaism.block.entity.InfiniteCobblestoneTankBlockEntity;
import com.tyami.forlaism.registry.BlockEntities;

import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

import org.jetbrains.annotations.Nullable;

/**
 * 無限丸石タンク。
 *
 * - IItemHandler で丸石を無限に供給（パイプ等に吸われても減らない）
 * - 右クリックで丸石を64個ずつインベントリに取り出せる
 */
public class InfiniteCobblestoneTankBlock extends BaseEntityBlock {

    public InfiniteCobblestoneTankBlock(BlockBehaviour.Properties properties) {
        super(properties);
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new InfiniteCobblestoneTankBlockEntity(pos, state);
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos,
                                 Player player, InteractionHand hand, BlockHitResult hit) {

        if (!level.isClientSide) {
            ItemStack cobble = new ItemStack(Items.COBBLESTONE, 64);

            if (!player.getInventory().add(cobble)) {
                player.drop(cobble, false);
            }

            level.playSound(null, pos,
                    SoundEvents.ITEM_PICKUP,
                    SoundSource.BLOCKS, 1.0F, 1.2F);
        }

        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos,
                         BlockState newState, boolean isMoving) {
        if (state.getBlock() != newState.getBlock()) {
            if (level.getBlockEntity(pos) instanceof InfiniteCobblestoneTankBlockEntity be) {
                be.drops();
            }
        }
        super.onRemove(state, level, pos, newState, isMoving);
    }
}