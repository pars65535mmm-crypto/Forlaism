package com.tyami.forlaism.item;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import com.tyami.forlaism.event.RingSlotData;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import top.theillusivec4.curios.api.CuriosApi;

import javax.annotation.Nullable;
import java.util.List;

/**
 * リングオブリング。
 *
 * 使用するとCuriosの ring スロットを1つ永続的に増やす。
 * 何度でも重ねがけ可能。
 */
public class RingOfRingsItem extends Item {

    public RingOfRingsItem(Properties properties) {
        super(properties.stacksTo(16));
    }

    @Override
    public InteractionResultHolder<ItemStack> use(
            Level level,
            Player player,
            InteractionHand hand
    ) {
        ItemStack stack = player.getItemInHand(hand);

        if (level.isClientSide) {
            return InteractionResultHolder.sidedSuccess(stack, true);
        }

        // =========================================================
        // Curiosの ring スロットを1つ増やす
        // =========================================================
        boolean success = CuriosApi.getCuriosInventory(player)
                .map(handler -> {
                    handler.growSlotType("ring", 1);
                    return true;
                })
                .orElse(false);

        if (!success) {
            player.displayClientMessage(
                    Component.literal("§cリングスロットを拡張できませんでした…"),
                    true
            );
            return InteractionResultHolder.fail(stack);
        }

        // =========================================================
        // 永続化フラグを立てる（ログイン時に再適用するため）
        // =========================================================
        if (player instanceof ServerPlayer serverPlayer) {
            RingSlotData.addExtraSlots(serverPlayer, 1);
        }

        // =========================================================
        // 消費
        // =========================================================
        if (!player.getAbilities().instabuild) {
            stack.shrink(1);
        }

        // =========================================================
        // 演出
        // =========================================================
        level.playSound(
                null,
                player.getX(), player.getY(), player.getZ(),
                SoundEvents.AMETHYST_BLOCK_CHIME,
                SoundSource.PLAYERS,
                1.0F,
                1.5F
        );

        player.displayClientMessage(
                Component.literal("§d§lリングスロットが §b1つ §d増えた…"),
                true
        );

        return InteractionResultHolder.sidedSuccess(stack, false);
    }

    @Override
    public void appendHoverText(
            ItemStack stack,
            @Nullable Level level,
            List<Component> tooltip,
            TooltipFlag flag
    ) {
        tooltip.add(Component.literal("§d使用するとリングスロットが1つ増える。")
                .withStyle(ChatFormatting.LIGHT_PURPLE));
        tooltip.add(Component.literal("§7永続。何度でも重ねがけ可能。")
                .withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.literal("§8指輪が指輪を呼ぶ。")
                .withStyle(ChatFormatting.DARK_GRAY));
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return true;
    }
}