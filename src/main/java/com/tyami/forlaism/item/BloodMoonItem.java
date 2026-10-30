package com.tyami.forlaism.item;

import com.tyami.forlaism.ForalisItem;
import com.tyami.forlaism.event.BloodMoonHandler;
import com.tyami.forlaism.world.BloodMoonManager;
import com.tyami.watelib.item.IAnimatedTextItem;
import com.tyami.watelib.text.AnimatedText;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import javax.annotation.Nullable;
import java.util.List;

/**
 * 血の月の珠。
 *
 * 使用するとブラッドムーンが発動する。
 * 一晩だけ続き、夜明けで自動解除。
 * 使用すると消える。
 */
public class BloodMoonItem extends ForalisItem implements IAnimatedTextItem {

    public BloodMoonItem(Properties properties) {
        super(properties.stacksTo(1).fireResistant().rarity(Rarity.EPIC));
    }

    @Override
    public AnimatedText createAnimatedName(ItemStack stack) {
        return AnimatedText.of("血の月の珠")
                .wave(2.5F, 0.35F, 0.50F)
                .gradient(0xFF000000, 0xFF8B0000, 0xFFFF2222, 0xFF8B0000)
                .gradientSpeed(0.8F)
                .gradientPhase(0.6F);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        if (level.isClientSide) {
            return InteractionResultHolder.sidedSuccess(stack, true);
        }

        if (!(level instanceof ServerLevel sl)) return InteractionResultHolder.pass(stack);
        if (!(player instanceof ServerPlayer sp)) return InteractionResultHolder.pass(stack);

        // 既に発動中
        if (BloodMoonManager.isActive(sl)) {
            sp.displayClientMessage(
                    Component.literal("§c既に血の月が昇っている…")
                            .withStyle(ChatFormatting.RED),
                    true
            );
            return InteractionResultHolder.fail(stack);
        }

        // 発動
        BloodMoonHandler.start(sl);

        // 夜でないなら時間を夜へ
        long time = sl.getDayTime() % 24000L;
        if (time < 13000L || time >= 23000L) {
            sl.setDayTime(14000L); // 夜に飛ばす
        }

        // 演出
        sl.playSound(null, sp.getX(), sp.getY(), sp.getZ(),
                SoundEvents.WITHER_SPAWN, SoundSource.PLAYERS, 2.0F, 0.5F);
        sl.playSound(null, sp.getX(), sp.getY(), sp.getZ(),
                SoundEvents.ENDER_DRAGON_GROWL, SoundSource.PLAYERS, 2.0F, 0.6F);

        // 消費
        if (!sp.getAbilities().instabuild) {
            stack.shrink(1);
        }

        return InteractionResultHolder.sidedSuccess(stack, false);
    }

    @Override
    public void appendHoverText(
            ItemStack stack, @Nullable Level level,
            List<Component> tooltip, TooltipFlag flag
    ) {
        tooltip.add(Component.literal("§4§l血の月を呼び覚ます。")
                .withStyle(ChatFormatting.DARK_RED));
        tooltip.add(Component.literal("§7使用すると一晩だけブラッドムーンが昇る。")
                .withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.literal("§8モブは強化され、眠ることはできない。")
                .withStyle(ChatFormatting.DARK_GRAY));
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return true;
    }
}