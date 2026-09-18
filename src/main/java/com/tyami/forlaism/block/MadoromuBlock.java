package com.tyami.forlaism.block;

import com.tyami.forlaism.block.entity.InserterBlockEntity;
import com.tyami.forlaism.registry.Blocks;
import com.tyami.forlaism.registry.Items;
import com.tyami.forlaism.world.MadoromuMultiblockManager;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

public class MadoromuBlock extends Block {

    public MadoromuBlock(BlockBehaviour.Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos,
                                 Player player, InteractionHand hand, BlockHitResult hit) {

        ItemStack stack = player.getItemInHand(hand);

        // 賢者の石？ でなければ通常処理
        if (!stack.is(Items.SAGE_STONE.get())) {
            return InteractionResult.PASS;
        }

        if (level.isClientSide) {
            return InteractionResult.SUCCESS;
        }

        // 近くの搬入機を探す
        BlockPos inserterPos = findNeighborInserter(level, pos, hit.getDirection());

        if (inserterPos == null) {
            player.displayClientMessage(
                    Component.literal("§c近くにフォラリス搬入機が見つかりません。"),
                    true
            );
            return InteractionResult.FAIL;
        }

        // 内向き法線を検出
        Direction inward = MadoromuMultiblockManager.detectInwardNormal(level, inserterPos);

        if (inward == null) {
            player.displayClientMessage(
                    Component.literal("§c搬入機の向きを判定できません。"),
                    true
            );
            return InteractionResult.FAIL;
        }

        // 構造チェック
        if (!MadoromuMultiblockManager.validateStructure(level, inserterPos, inward)) {
            player.displayClientMessage(
                    Component.literal("§c構造が正しくありません。5×5×10の筒を確認してください。"),
                    true
            );
            return InteractionResult.FAIL;
        }

        // 登録
        MadoromuMultiblockManager.register(inserterPos);

        // =========================================================
        // Fを量子融合モードに切替
        // =========================================================
        if (level.getBlockEntity(inserterPos) instanceof InserterBlockEntity inserter) {
            inserter.setQuantumFusionMode(true);
        }

        // 演出
        if (level instanceof ServerLevel serverLevel) {
            serverLevel.playSound(null, inserterPos, SoundEvents.BEACON_ACTIVATE,
                    SoundSource.BLOCKS, 1.0F, 1.5F);
        }

        player.displayClientMessage(
                Component.literal("§d§l微睡む九十九の夢 §fが §b目覚め §fを始めた…"),
                true
        );

        return InteractionResult.SUCCESS;
    }

    /**
     * 指定位置の周囲6方向から搬入機を探す。
     */
    private static BlockPos findNeighborInserter(Level level, BlockPos pos, Direction hint) {
        // まずヒント方向を優先
        BlockPos hinted = pos.relative(hint.getOpposite());
        if (level.getBlockState(hinted).is(Blocks.FORLAISM_INSERTER.get())) {
            return hinted;
        }

        // 全方向を走査
        for (Direction d : Direction.values()) {
            BlockPos candidate = pos.relative(d);
            if (level.getBlockState(candidate).is(Blocks.FORLAISM_INSERTER.get())) {
                return candidate;
            }
        }

        return null;
    }
}