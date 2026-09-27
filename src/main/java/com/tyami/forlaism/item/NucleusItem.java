package com.tyami.forlaism.item;

import com.tyami.forlaism.ForalisItem;
import com.tyami.forlaism.registry.Items;
import com.tyami.forlaism.world.NucleusContaminationManager;
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
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import javax.annotation.Nullable;
import java.util.List;

/**
 * 核。
 *
 * 左手で使用すると、その土地を永続汚染する。
 *
 * - 左手以外では使用不可
 * - 使用すると自爆死
 * - 大罪の石を所持しているとリンネディウムインゴットに変化
 * - 最大64個まで同時に処理される（スタック数分）
 */
public class NucleusItem extends ForalisItem implements IAnimatedTextItem {

    public NucleusItem(Properties properties) {
        super(properties.stacksTo(64).fireResistant());
    }

    @Override
    public AnimatedText createAnimatedName(ItemStack stack) {
        return AnimatedText.of("核")
                .wave(3.0F, 0.50F, 0.50F)
                .gradient(0xFF000000, 0xFFFF0000, 0xFF000000, 0xFFFF6600)
                .gradientSpeed(1.2F)
                .gradientPhase(0.5F);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(
            Level level,
            Player player,
            InteractionHand hand
    ) {
        ItemStack stack = player.getItemInHand(hand);

        // 左手以外は使用不可
        if (hand != InteractionHand.OFF_HAND) {
            if (!level.isClientSide) {
                player.displayClientMessage(
                        Component.literal("§c核は左手でしか起動できない…"),
                        true
                );
            }
            return InteractionResultHolder.fail(stack);
        }

        // クライアントは何もしない
        if (level.isClientSide || !(level instanceof ServerLevel serverLevel)) {
            return InteractionResultHolder.sidedSuccess(stack, true);
        }

        if (!(player instanceof ServerPlayer serverPlayer)) {
            return InteractionResultHolder.fail(stack);
        }

        // スタック数（最大64）
        int count = Math.min(stack.getCount(), 64);

        // =========================================================
        // 大罪の石を持っているか確認
        // =========================================================
        boolean hasSinStone = false;
        int sinStoneSlot = -1;

        for (int i = 0; i < serverPlayer.getInventory().getContainerSize(); i++) {
            ItemStack inv = serverPlayer.getInventory().getItem(i);
            if (inv.is(Items.STONE_OF_SIN.get())) {
                hasSinStone = true;
                sinStoneSlot = i;
                break;
            }
        }

        // =========================================================
        // 汚染開始（爆心地 = プレイヤー座標）
        // =========================================================
        NucleusContaminationManager.detonate(
                serverLevel,
                serverPlayer.blockPosition(),
                count
        );

        // =========================================================
        // 大罪の石 → リスポーン時にリンネディウム付与
        // =========================================================
        if (hasSinStone && sinStoneSlot >= 0) {
            ItemStack sinStone = serverPlayer.getInventory().getItem(sinStoneSlot);

            // 大罪の石を消費
            sinStone.shrink(1);

            // リスポーン時の付与予約
            com.tyami.forlaism.event.PendingRinnediumData.schedule(
                    serverPlayer,
                    count
            );

            serverPlayer.displayClientMessage(
                    Component.literal("§d§l大罪の石が変容を始めた… §5リスポーン時に §dリンネディウム ×" + count),
                    true
            );
        }

        // =========================================================
        // 核を消費
        // =========================================================
        if (!serverPlayer.getAbilities().instabuild) {
            stack.shrink(count);
        }

        // =========================================================
        // 自爆死（無敵貫通・クリエイティブ貫通）
        // =========================================================
        serverPlayer.invulnerableTime = 0;
        serverPlayer.setHealth(0.0F);

        if (!serverPlayer.isDeadOrDying()) {
            serverPlayer.die(
                    serverPlayer.damageSources().explosion(serverPlayer, serverPlayer)
            );
        }

        // =========================================================
        // 演出
        // =========================================================
        serverLevel.playSound(
                null,
                serverPlayer.getX(),
                serverPlayer.getY(),
                serverPlayer.getZ(),
                SoundEvents.WARDEN_SONIC_BOOM,
                SoundSource.PLAYERS,
                5.0F,
                0.3F
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
        tooltip.add(Component.literal("§4§l【左手専用】")
                .withStyle(ChatFormatting.DARK_RED));
        tooltip.add(Component.literal("§c使用した土地は §4永続的に汚染 §cされる。")
                .withStyle(ChatFormatting.RED));
        tooltip.add(Component.literal("§7・半径16チャンク：毎tick 21億貫通ダメージ")
                .withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.literal("§7・20チャンク以内：烈火")
                .withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.literal("§7・30チャンク以内：240ダメージ")
                .withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.literal("§7・45チャンク以内のMob：3,000,000ダメージ")
                .withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.empty());
        tooltip.add(Component.literal("§5大罪の石を所持していると §dリンネディウム §5に変わる…")
                .withStyle(ChatFormatting.DARK_PURPLE));
        tooltip.add(Component.literal("§8その土地は、諦めろ。")
                .withStyle(ChatFormatting.DARK_GRAY));
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return true;
    }
}