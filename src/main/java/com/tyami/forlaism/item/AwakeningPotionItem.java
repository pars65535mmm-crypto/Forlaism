package com.tyami.forlaism.item;

import com.tyami.forlaism.registry.ModPotions;
import com.tyami.forlaism.event.DreamCollapseHandler;
import com.tyami.forlaism.world.DreamDimension;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.PotionItem;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.alchemy.PotionUtils;
import net.minecraft.world.level.Level;

import javax.annotation.Nullable;
import java.util.List;

/**
 * 目覚め薬。
 *
 * PotionItem を継承することで、バニラのポーション扱いになる。
 * 中身の Potion は ModPotions.AWAKENING。
 *
 * Dreamで飲むと崩壊イベント発動。
 * Dream以外では吐き気 + 暗視だけ。
 */
public class AwakeningPotionItem extends PotionItem {

    public AwakeningPotionItem(Properties properties) {
        super(properties.stacksTo(1).rarity(Rarity.EPIC));
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        if (level.isClientSide) {
            return InteractionResultHolder.sidedSuccess(stack, true);
        }

        if (!(player instanceof ServerPlayer sp)) {
            return InteractionResultHolder.fail(stack);
        }

        // ポーションの中身を確認（念のため）
        var potion = PotionUtils.getPotion(stack);
        if (potion != ModPotions.AWAKENING.get()) {
            return super.use(level, player, hand);
        }

        // =========================================================
        // Dreamディメンション判定
        // =========================================================
        boolean inDream = player.level().dimension().equals(DreamDimension.DREAM_LEVEL);

        if (inDream) {
            DreamCollapseHandler.startCollapse(sp);

            sp.displayClientMessage(
                    Component.literal("§5§l世界が… 目を覚まそうとしている…")
                            .withStyle(ChatFormatting.DARK_PURPLE),
                    true
            );
        } else {
            // 通常世界 → デバフだけ
            player.addEffect(new MobEffectInstance(
                    MobEffects.CONFUSION, 400, 1, false, true, true
            ));
            player.addEffect(new MobEffectInstance(
                    MobEffects.NIGHT_VISION, 400, 0, false, true, true
            ));

            sp.displayClientMessage(
                    Component.literal("§7少し吐き気がする…")
                            .withStyle(ChatFormatting.GRAY),
                    true
            );
        }

        level.playSound(
                null,
                player.getX(), player.getY(), player.getZ(),
                SoundEvents.BREWING_STAND_BREW,
                SoundSource.PLAYERS,
                1.0F, 0.8F
        );

        // 消費
        if (!player.getAbilities().instabuild) {
            stack.shrink(1);

            // ガラス瓶を返す
            ItemStack bottle = new ItemStack(net.minecraft.world.item.Items.GLASS_BOTTLE);
            if (!player.getInventory().add(bottle)) {
                player.drop(bottle, false);
            }
        }

        return InteractionResultHolder.sidedSuccess(stack, false);
    }

    @Override
    public void appendHoverText(
            ItemStack stack,
            @Nullable Level level,
            List<Component> tooltip,
            TooltipFlag flag
    ) {
        // 親（PotionItem）のTooltipは「ポーション効果」を表示してしまうので、
        // super を呼ばずに独自Tooltipだけ表示する
        tooltip.add(Component.literal("§5§l何に使うのだろうか..")
                .withStyle(ChatFormatting.DARK_PURPLE));
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return true;
    }

    @Override
public String getDescriptionId(ItemStack stack) {
    return "item.forlaism.awakening_potion";
}
}