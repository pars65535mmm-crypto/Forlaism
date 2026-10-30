package com.tyami.forlaism.item;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;

import javax.annotation.Nullable;
import java.util.List;

/**
 * 500倍圧縮土のアイテム形態。
 *
 * 見た目はただの土。ただしエンチャントされたように光る。
 */
public class CompressedDirtItem extends BlockItem {

    public CompressedDirtItem(Block block, Properties properties) {
        super(block, properties);
    }

    /** エンチャントされたような光沢（glint）を常にONにする。 */
    @Override
    public boolean isFoil(ItemStack stack) {
        return true;
    }

    @Override
    public void appendHoverText(
            ItemStack stack,
            @Nullable Level level,
            List<Component> tooltip,
            TooltipFlag flag
    ) {
        tooltip.add(Component.literal("文字化けしてたらしいよ、..この...土..?なのか.?")
                .withStyle(ChatFormatting.GRAY));
    }
}