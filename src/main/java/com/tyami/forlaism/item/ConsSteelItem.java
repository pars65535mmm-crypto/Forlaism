package com.tyami.forlaism.item;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import javax.annotation.Nullable;
import java.util.List;

/**
 * コンスチール。
 *
 * コンクリート + コミメタル + 鉛 から作られる合金素材。
 */
public class ConsSteelItem extends Item {

    public ConsSteelItem(Properties properties) {
        super(properties);
    }

    @Override
    public void appendHoverText(
            ItemStack stack,
            @Nullable Level level,
            List<Component> tooltip,
            TooltipFlag flag
    ) {
        tooltip.add(Component.literal("§7通称 コンクリート合金。")
                .withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.literal("§8重い、硬い、安い")
                .withStyle(ChatFormatting.DARK_GRAY));
    }
}