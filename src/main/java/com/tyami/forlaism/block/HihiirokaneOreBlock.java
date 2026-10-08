package com.tyami.forlaism.block;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;

/**
 * ヒヒイロカネ鉱石。
 *
 * - メタアダマンタインのツルハシでのみ採掘可能
 * - それ以外のツール（ネザライト含む）では壊しても何もドロップしない
 */
public class HihiirokaneOreBlock extends Block {

    public HihiirokaneOreBlock(BlockBehaviour.Properties properties) {
        super(properties);
    }

    /**
     * 正しいツールで掘ったときのみドロップする判定。
     *
     * バニラの isCorrectToolForDrops を上書きして、
     * メタアダマンタインのツルハシを持っているときだけ true を返す。
     */
    @Override
    public boolean canHarvestBlock(
            BlockState state,
            BlockGetter level,
            BlockPos pos,
            Player player
    ) {
        if (player == null) return false;

        ItemStack tool = player.getMainHandItem();

        // メタアダマンタインのツルハシのみ許可
        return tool.is(com.tyami.forlaism.registry.Items.META_ADAMANTINE_PICKAXE.get());
    }
}