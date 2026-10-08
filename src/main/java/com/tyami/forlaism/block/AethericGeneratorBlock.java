package com.tyami.forlaism.block;

import com.tyami.forlaism.block.entity.AethericGeneratorBlockEntity;
import com.tyami.forlaism.registry.BlockEntities;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
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
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraftforge.common.capabilities.ForgeCapabilities;

import javax.annotation.Nullable;

/**
 * 電核機。
 *
 * - アイテムを手に持って右クリック → セット
 * - 素手で右クリック → 取り出し
 */
public class AethericGeneratorBlock extends BaseEntityBlock {

    public AethericGeneratorBlock(Properties properties) {
        super(properties);
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.ENTITYBLOCK_ANIMATED;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new AethericGeneratorBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(
            Level level, BlockState state, BlockEntityType<T> type) {
        return createTickerHelper(
                type,
                BlockEntities.AETHERIC_GENERATOR.get(),
                AethericGeneratorBlockEntity::tick
        );
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos,
                                 Player player, InteractionHand hand, BlockHitResult hit) {

        if (level.isClientSide) {
            return InteractionResult.SUCCESS;
        }

        BlockEntity be = level.getBlockEntity(pos);
        if (!(be instanceof AethericGeneratorBlockEntity gen)) {
            return InteractionResult.PASS;
        }

        ItemStack held = player.getItemInHand(hand);

        // 何かアイテムを持っている → セット
        if (!held.isEmpty()) {

            ItemStack current = gen.getCapability(ForgeCapabilities.ITEM_HANDLER)
                    .map(h -> h.getStackInSlot(0))
                    .orElse(ItemStack.EMPTY);

            if (!current.isEmpty()) {
                player.displayClientMessage(
                        Component.literal("§c既に何か入っています（素手で取り出し）"),
                        true
                );
                return InteractionResult.FAIL;
            }

            // 発電できないアイテムは弾く
            if (AethericGeneratorBlockEntity.getOutputFor(held.getItem()) == null) {
                player.displayClientMessage(
                        Component.literal("§cこのアイテムは燃料になりません…"),
                        true
                );
                return InteractionResult.FAIL;
            }

            ItemStack copy = held.copy();
            copy.setCount(1);

            gen.getCapability(ForgeCapabilities.ITEM_HANDLER)
                    .ifPresent(h -> h.insertItem(0, copy, false));

            if (!player.getAbilities().instabuild) {
                held.shrink(1);
            }

            level.playSound(null, pos,
                    SoundEvents.ITEM_PICKUP,
                    SoundSource.BLOCKS, 1.0F, 1.2F);

            player.displayClientMessage(
                    Component.literal("§d電核機が唸り始めた… §e発電中！"),
                    true
            );
            return InteractionResult.SUCCESS;
        }

        // 素手 → 取り出し
        ItemStack taken = gen.getCapability(ForgeCapabilities.ITEM_HANDLER)
                .map(h -> h.extractItem(0, 64, false))
                .orElse(ItemStack.EMPTY);

        if (!taken.isEmpty()) {
            if (!player.getInventory().add(taken)) {
                player.drop(taken, false);
            }
            level.playSound(null, pos,
                    SoundEvents.ITEM_PICKUP,
                    SoundSource.BLOCKS, 1.0F, 0.8F);
            player.displayClientMessage(
                    Component.literal("§7中身を取り出した"),
                    true
            );
            return InteractionResult.SUCCESS;
        }

        return InteractionResult.PASS;
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos,
                         BlockState newState, boolean isMoving) {
        if (state.getBlock() != newState.getBlock()) {
            if (level.getBlockEntity(pos) instanceof AethericGeneratorBlockEntity gen) {
                gen.drops();
            }
        }
        super.onRemove(state, level, pos, newState, isMoving);
    }
}