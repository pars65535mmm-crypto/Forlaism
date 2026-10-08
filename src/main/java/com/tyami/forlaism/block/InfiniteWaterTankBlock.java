package com.tyami.forlaism.block;

import com.tyami.forlaism.block.entity.InfiniteWaterTankBlockEntity;
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
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

import org.jetbrains.annotations.Nullable;

/**
 * 無限水源タンク。
 *
 * - IFluidHandler で無限の水を供給（パイプで吸われても減らない）
 * - 空バケツで右クリック → 水入りバケツになる
 */
public class InfiniteWaterTankBlock extends BaseEntityBlock {

    public InfiniteWaterTankBlock(BlockBehaviour.Properties properties) {
        super(properties);
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new InfiniteWaterTankBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(
            Level level, BlockState state, BlockEntityType<T> type) {
        // 特にtick処理は不要（drain で無限を返すだけ）
        return null;
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos,
                                 Player player, InteractionHand hand, BlockHitResult hit) {

        ItemStack held = player.getItemInHand(hand);

        // 空バケツで右クリック → 水入りバケツに
        if (held.is(Items.BUCKET)) {
            if (!level.isClientSide) {
                ItemStack waterBucket = new ItemStack(Items.WATER_BUCKET);

                if (!player.getAbilities().instabuild) {
                    held.shrink(1);
                }

                if (held.isEmpty()) {
                    player.setItemInHand(hand, waterBucket);
                } else if (!player.getInventory().add(waterBucket)) {
                    player.drop(waterBucket, false);
                }

                level.playSound(null, pos,
                        SoundEvents.BUCKET_FILL,
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
            if (level.getBlockEntity(pos) instanceof InfiniteWaterTankBlockEntity be) {
                be.drops();
            }
        }
        super.onRemove(state, level, pos, newState, isMoving);
    }
}