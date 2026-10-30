package com.tyami.forlaism.block;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;

/**
 * 500倍圧縮土ブロック。
 *
 * 見た目はバニラの土そのもの。
 * 違いは「エンチャントされたような光沢（glint）」だけ。
 * アイテム側 (CompressedDirtItem#isFoil) でグリントを付与する。
 */
public class CompressedDirtBlock extends Block {

    public CompressedDirtBlock(BlockBehaviour.Properties properties) {
        super(properties);
    }
}