package com.tyami.forlaism.block;

import com.tyami.forlaism.block.entity.InfiniteLavaTankBlockEntity;
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
 * 無限マグマタンク。
 *
 * - IFluidHandler で無限のマグマを供給（パイプで吸われても減らない）
 * - 空バケツで右クリック → マグマ入りバケツになる
 */
public class InfiniteLavaTankBlock extends BaseEntityBlock {

    public InfiniteLavaTankBlock(BlockBehaviour.Properties properties) {
        super(properties);
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new InfiniteLavaTankBlockEntity(pos, state);
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos,
                                 Player player, InteractionHand hand, BlockHitResult hit) {

        ItemStack held = player.getItemInHand(hand);

        if (held.is(Items.BUCKET)) {
            if (!level.isClientSide) {
                ItemStack lavaBucket = new ItemStack(Items.LAVA_BUCKET);

                if (!player.getAbilities().instabuild) {
                    held.shrink(1);
                }

                if (held.isEmpty()) {
                    player.setItemInHand(hand, lavaBucket);
                } else if (!player.getInventory().add(lavaBucket)) {
                    player.drop(lavaBucket, false);
                }

                level.playSound(null, pos,
                        SoundEvents.BUCKET_FILL_LAVA,
                        SoundSource.BLOCKS, 1.0F, 1.0F);
            }
            return InteractionResult.sidedSuccess(level.isClientSide);
        }

        return InteractionResult.PASS;
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos,
                         BlockState newState, boolean isMoving) {
        if (state.getBlock() != newState.getBlock()) {
            if (level.getBlockEntity(pos) instanceof InfiniteLavaTankBlockEntity be) {
                be.drops();
            }
        }
        super.onRemove(state, level, pos, newState, isMoving);
    }
}