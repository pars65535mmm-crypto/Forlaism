package com.tyami.forlaism.item;

import com.tyami.forlaism.registry.Fluids;

import net.minecraft.core.BlockPos;
import com.tyami.forlaism.event.BathPowderHandler;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;

public class BathPowderItem extends Item {

    public BathPowderItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {

        Level level = context.getLevel();
        BlockPos clickedPos = context.getClickedPos();

        // 黒曜石以外には使用不可
        if (!level.getBlockState(clickedPos).is(Blocks.OBSIDIAN)) {
            return InteractionResult.PASS;
        }

        // クライアント側では成功だけ返す
        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
        }

        ServerLevel serverLevel =
                (ServerLevel) level;

        /*
         * 黒曜石の上を基準にする。
         *
         * 7x7の外周にオークの原木
         * 高さ2
         *
         * 中央5x5は後で温泉になる。
         */

        BlockPos base =
                clickedPos.above();

        for (int x = -3; x <= 3; x++) {
            for (int z = -3; z <= 3; z++) {

                // 外周だけ原木にする
                if (Math.abs(x) != 3
                        && Math.abs(z) != 3) {
                    continue;
                }

                for (int y = 0; y < 2; y++) {

                    BlockPos pos =
                            base.offset(x, y, z);

                    serverLevel.setBlock(
                            pos,
                            Blocks.OAK_LOG.defaultBlockState(),
                            3
                    );
                }
            }
        }

        /*
         * 0.3秒後
         * = 6 tick後
         *
         * 中央5x5を温泉にする。
         */

        BathPowderHandler.scheduleOnsen(
                serverLevel,
                base,
                6
        );

        // アイテムを1個消費
        ItemStack stack =
                context.getItemInHand();

        stack.shrink(1);

        return InteractionResult.SUCCESS;
    }
}