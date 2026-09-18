package com.tyami.forlaism.item;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import javax.annotation.Nullable;
import java.util.List;

public class DreamBookItem extends Item {

    public DreamBookItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        if (!level.isClientSide && player instanceof ServerPlayer serverPlayer) {
            // 100XPレベルを追加（経験値レベルを直接加算）
            int currentLevel = serverPlayer.experienceLevel;
            int newLevel = currentLevel + 100;
            serverPlayer.setExperienceLevels(newLevel);

            // 1個消費
            if (!player.getAbilities().instabuild) {
                stack.shrink(1);
            }

            // メッセージ表示
            serverPlayer.displayClientMessage(
                    Component.literal("§d§l夢幻の書 §fが §b100XPレベル §fを付与しました！"),
                    true
            );
        }

        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("item.forlaism.dream_book.tooltip").withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("item.forlaism.dream_book.tooltip2").withStyle(ChatFormatting.LIGHT_PURPLE));
        tooltip.add(Component.translatable("item.forlaism.dream_book.tooltip3").withStyle(ChatFormatting.GOLD));
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return true;
    }
}