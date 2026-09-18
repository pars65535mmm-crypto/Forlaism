package com.tyami.forlaism.block;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;

/**
 * 夢幻の床。
 *
 * - 岩盤より硬い
 * - ツルハシでも掘れない (destroyTime = -1)
 * - 爆発耐性最大
 * - ドロップなし
 * - 入手不可能
 */
public class DreamFloorBlock extends Block {

    public DreamFloorBlock(BlockBehaviour.Properties properties) {
        super(properties);
    }
}