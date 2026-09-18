package com.tyami.forlaism.item;

import com.tyami.forlaism.ForalisItem;
import com.tyami.forlaism.registry.Blocks;
import com.tyami.forlaism.registry.Fluids;
import com.tyami.forlaism.registry.Items;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUtils;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;

public class PolycrystalBucketItem extends ForalisItem {

    public PolycrystalBucketItem(Properties properties) {
        super(properties.stacksTo(16));
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack itemStack = player.getItemInHand(hand);
        BlockHitResult hitResult = getPlayerPOVHitResult(level, player, ClipContext.Fluid.SOURCE_ONLY);

        if (hitResult.getType() == HitResult.Type.MISS) {
            return InteractionResultHolder.pass(itemStack);
        } else if (hitResult.getType() != HitResult.Type.BLOCK) {
            return InteractionResultHolder.pass(itemStack);
        }

        BlockPos blockPos = hitResult.getBlockPos();
        FluidState fluidState = level.getFluidState(blockPos);

        if (fluidState.getType() == Fluids.FORLAISM_POLYCRYSTAL_SOLUTION.get()) {
            // 多結晶養液のみを回収できる
            level.setBlock(blockPos, net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(), 11);
            player.awardStat(Stats.ITEM_USED.get(this));
            level.playSound(player, blockPos, SoundEvents.BUCKET_FILL, SoundSource.BLOCKS, 1.0F, 1.0F);

            ItemStack filledBucket = new ItemStack(Items.FORLAISM_POLYCRYSTAL_SOLUTION_BUCKET.get());
            ItemStack resultStack = ItemUtils.createFilledResult(itemStack, player, filledBucket);
            return InteractionResultHolder.sidedSuccess(resultStack, level.isClientSide());
        }

        return InteractionResultHolder.pass(itemStack);
    }
}
