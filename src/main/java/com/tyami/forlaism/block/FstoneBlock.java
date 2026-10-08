package com.tyami.forlaism.block;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;

import javax.annotation.Nullable;
import java.util.List;

/**
 * Fstone（エフストーン）。
 *
 * 内部ID: forlaism:fstone
 * 表示名: 石
 *
 * ・見た目は普通の石
 * ・バニラの石と同じ名前
 * ・でも壊せない（岩盤と同じ硬さ）
 * ・「壊せそうで壊せない」= FakeDreamの象徴
 */
public class FstoneBlock extends Block {

    public FstoneBlock(BlockBehaviour.Properties properties) {
        super(properties);
    }

    /**
     * ピックアップ不可。
     */
    @Override
    public boolean canHarvestBlock(
            net.minecraft.world.level.block.state.BlockState state,
            BlockGetter level,
            net.minecraft.core.BlockPos pos,
            net.minecraft.world.entity.player.Player player
    ) {
        return false;
    }

    /**
     * ツールチップは無い（= 普通の石に見える）
     * ただ、もし何かの拍子に表示された時に備えて何も書かない。
     */
    @Override
    public void appendHoverText(
            ItemStack stack,
            @Nullable BlockGetter level,
            List<Component> tooltip,
            TooltipFlag flag
    ) {
        super.appendHoverText(stack, level, tooltip, flag);
        // 何も書かない = 普通の石と同じ
    }
}