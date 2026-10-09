package com.tyami.forlaism.block;

import com.tyami.forlaism.block.entity.AltarBlockEntity;
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

import org.jetbrains.annotations.Nullable;

/**
 * 儀式祭壇ブロック。
 *
 * - アイテム持ち右クリック → カーソル位置に配置
 * - 素手右クリック → カーソル位置から取り出し（完成品優先）
 * - スニーク右クリック → 全アイテム回収
 */
public class AltarBlock extends BaseEntityBlock {

    public AltarBlock(Properties properties) {
        super(properties);
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new AltarBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(
            Level level, BlockState state, BlockEntityType<T> type) {
        return createTickerHelper(
                type,
                BlockEntities.RITUAL_ALTAR.get(),
                AltarBlockEntity::tick
        );
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos,
                                 Player player, InteractionHand hand, BlockHitResult hit) {

        if (!(level.getBlockEntity(pos) instanceof AltarBlockEntity altar)) {
            return InteractionResult.PASS;
        }

        ItemStack held = player.getItemInHand(hand);

        if (level.isClientSide) {
            return InteractionResult.SUCCESS;
        }

        // =========================================================
        // スニーク → 全回収
        // =========================================================
        if (player.isShiftKeyDown()) {
            if (altar.isEmpty() && !altar.hasOutput()) {
                player.displayClientMessage(
                        Component.literal("§7祭壇は空だ…"), true);
                return InteractionResult.SUCCESS;
            }

            if (altar.hasOutput()) {
                ItemStack out = altar.extractOutput();
                if (out != null && !player.getInventory().add(out)) {
                    player.drop(out, false);
                }
            }

            for (int i = 0; i < AltarBlockEntity.SLOTS; i++) {
                ItemStack s = altar.getItem(i);
                if (!s.isEmpty()) {
                    if (!player.getInventory().add(s.copy())) {
                        player.drop(s.copy(), false);
                    }
                    altar.setItem(i, ItemStack.EMPTY);
                }
            }

            level.playSound(null, pos,
                    SoundEvents.ITEM_PICKUP, SoundSource.BLOCKS, 1.0F, 1.0F);

            player.displayClientMessage(
                    Component.literal("§d祭壇のアイテムを回収した"), true);
            return InteractionResult.SUCCESS;
        }

        // =========================================================
        // アイテム持ち → カーソル位置に配置
        // =========================================================
        if (!held.isEmpty()) {

            if (altar.isCrafting()) {
                player.displayClientMessage(
                        Component.literal("§c儀式中は配置できない…"), true);
                return InteractionResult.FAIL;
            }

            if (!altar.placeAtCursor(held)) {
                player.displayClientMessage(
                        Component.literal("§cこのマスには既に何か置いてある"), true);
                return InteractionResult.FAIL;
            }

            if (!player.getAbilities().instabuild) {
                held.shrink(1);
            }

            level.playSound(null, pos,
                    SoundEvents.ITEM_PICKUP, SoundSource.BLOCKS, 0.8F, 1.5F);

            player.displayClientMessage(
                    Component.literal("§a(" + altar.getCursorCol() + ", "
                            + altar.getCursorRow() + ") §7に配置"),
                    true);

            return InteractionResult.SUCCESS;
        }

        // =========================================================
        // 素手 → 完成品優先、なければカーソル位置のアイテム
        // =========================================================
        if (altar.hasOutput()) {
            ItemStack out = altar.extractOutput();
            if (out != null) {
                if (!player.getInventory().add(out)) {
                    player.drop(out, false);
                }

                level.playSound(null, pos,
                        SoundEvents.ITEM_PICKUP, SoundSource.BLOCKS, 1.0F, 1.0F);

                player.displayClientMessage(
                        Component.literal("§d完成品を受け取った"), true);
                return InteractionResult.SUCCESS;
            }
        }

        ItemStack taken = altar.takeFromCursor();
        if (!taken.isEmpty()) {
            if (!player.getInventory().add(taken.copy())) {
                player.drop(taken.copy(), false);
            }

            level.playSound(null, pos,
                    SoundEvents.ITEM_PICKUP, SoundSource.BLOCKS, 0.8F, 0.8F);

            player.displayClientMessage(
                    Component.literal("§a(" + altar.getCursorCol() + ", "
                            + altar.getCursorRow() + ") §7から回収"),
                    true);
            return InteractionResult.SUCCESS;
        }

        player.displayClientMessage(
                Component.literal("§7このマスは空だ"), true);
        return InteractionResult.PASS;
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos,
                         BlockState newState, boolean isMoving) {
        if (state.getBlock() != newState.getBlock()) {
            if (level.getBlockEntity(pos) instanceof AltarBlockEntity altar) {
                altar.drops();
            }
        }
        super.onRemove(state, level, pos, newState, isMoving);
    }
}