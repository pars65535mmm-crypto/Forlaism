package com.tyami.forlaism.item;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import top.theillusivec4.curios.api.type.capability.ICurioItem;

import javax.annotation.Nullable;
import java.util.List;

/**
 * フックリング。
 *
 * 壁に密着しながらShiftを押すと壁に貼り付く。
 */
public class HookRingItem extends Item implements ICurioItem {

    public HookRingItem(Properties properties) {
        super(properties.stacksTo(1));
    }

    @Override
    public void appendHoverText(
            ItemStack stack,
            @Nullable Level level,
            List<Component> tooltip,
            TooltipFlag flag
    ) {
        tooltip.add(Component.literal("§e壁に密着してShift: 壁に張り付く")
                .withStyle(ChatFormatting.YELLOW));
        tooltip.add(Component.literal("§7壁を掴む指輪。")
                .withStyle(ChatFormatting.GRAY));
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return false;
    }
}