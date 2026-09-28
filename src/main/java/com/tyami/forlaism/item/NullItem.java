package com.tyami.forlaism.item;

import com.tyami.forlaism.erase.EraseHelper;
import com.tyami.watelib.item.IAnimatedTextItem;
import com.tyami.watelib.text.AnimatedText;

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

/**
 * null。
 *
 * 右クリックで発動者以外の半径30m以内の全Entityを抹消する。
 * 演出なし。静かに、確実に。
 */
public class NullItem extends Item implements IAnimatedTextItem {

    public NullItem(Properties properties) {
        super(properties.stacksTo(1).fireResistant());
    }

    @Override
    public AnimatedText createAnimatedName(ItemStack stack) {
        return AnimatedText.of("null")
                .wave(2.5F, 0.30F, 0.50F)
                .gradient(0xFF000000, 0xFF8800FF, 0xFF000000)
                .gradientSpeed(0.6F)
                .gradientPhase(1.0F);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {

        ItemStack stack = player.getItemInHand(hand);

        if (level.isClientSide) {
            return InteractionResultHolder.sidedSuccess(stack, true);
        }

        if (!(player instanceof ServerPlayer serverPlayer)) {
            return InteractionResultHolder.fail(stack);
        }

        // 半径30m以内の全Entityを抹消
        EraseHelper.eraseAround(serverPlayer);

        // 消費なし（使い放題）
        return InteractionResultHolder.sidedSuccess(stack, false);
    }

    @Override
    public void appendHoverText(
            ItemStack stack,
            @Nullable Level level,
            List<Component> tooltip,
            TooltipFlag flag
    ) {
        tooltip.add(Component.literal("§0§lnull")
                .withStyle(ChatFormatting.BLACK));
        tooltip.add(Component.literal("§7右クリックで半径30m以内の §0全て §7を抹消する。")
                .withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.literal("§8発動者以外。永続。復活不能。")
                .withStyle(ChatFormatting.DARK_GRAY));
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return true;
    }
}