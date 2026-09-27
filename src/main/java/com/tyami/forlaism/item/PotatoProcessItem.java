package com.tyami.forlaism.item;

import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import javax.annotation.Nullable;
import java.util.List;

/**
 * 加工途中のジャガイモ。
 *
 * NBT "Stage" で段階管理:
 *   0 = RAW       (ただのジャガイモ)
 *   1 = WASHED    (洗った)
 *   2 = CUT       (切った)
 *   3 = DRIED     (乾燥した切った)
 *   4 = POWDER    (粉)
 *   5 = KNEADED   (練った入りボウル)
 *   6 = HARDENED  (固まった)
 *
 * 右クリック処理:
 *   WASHED    + オフハンドに剣 → CUT
 *   DRIED     → POWDER
 *   POWDER    + 水入り瓶をオフハンド → KNEADED
 *   KNEADED   + 凝灰岩をオフハンド → HARDENED
 *   HARDENED  → 温泉に投げる (PotatoProcessHandler 側で処理)
 */
public class PotatoProcessItem extends Item {

    public static final String TAG_STAGE = "Stage";
    public static final String TAG_DRY_PROGRESS = "DryProgress";

    public static final int STAGE_RAW      = 0;
    public static final int STAGE_WASHED   = 1;
    public static final int STAGE_CUT      = 2;
    public static final int STAGE_DRIED    = 3;
    public static final int STAGE_POWDER   = 4;
    public static final int STAGE_KNEADED  = 5;
    public static final int STAGE_HARDENED = 6;

    /** 乾燥に必要なtick数。2分 = 2400 tick。 */
    public static final int DRY_REQUIRED_TICKS = 20 * 120;

    public PotatoProcessItem(Properties properties) {
        super(properties);
    }

    // =========================================================
    // NBTヘルパー
    // =========================================================

    public static int getStage(ItemStack stack) {
        CompoundTag tag = stack.getTag();
        return tag == null ? STAGE_RAW : tag.getInt(TAG_STAGE);
    }

    public static void setStage(ItemStack stack, int stage) {
        stack.getOrCreateTag().putInt(TAG_STAGE, stage);
        stack.getOrCreateTag().putInt(TAG_DRY_PROGRESS, 0);
    }

    public static int getDryProgress(ItemStack stack) {
        CompoundTag tag = stack.getTag();
        return tag == null ? 0 : tag.getInt(TAG_DRY_PROGRESS);
    }

    public static void setDryProgress(ItemStack stack, int progress) {
        stack.getOrCreateTag().putInt(TAG_DRY_PROGRESS, progress);
    }

    // =========================================================
    // 右クリック: 段階に応じた加工
    // =========================================================

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        int stage = getStage(stack);

        // ---------------------------------------------------------
        // WASHED + オフハンドに剣 → CUT (切る)
        // ---------------------------------------------------------
        if (stage == STAGE_WASHED) {
            ItemStack off = player.getOffhandItem();
            if (off.getItem() instanceof SwordItem) {
                if (!level.isClientSide) {
                    setStage(stack, STAGE_CUT);

                    level.playSound(null, player.getX(), player.getY(), player.getZ(),
                            SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.PLAYERS, 0.8F, 1.5F);

                    // 剣の耐久を1減らす
                    off.hurtAndBreak(1, player,
                            p -> p.broadcastBreakEvent(InteractionHand.OFF_HAND));
                }
                return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
            }
        }

        // ---------------------------------------------------------
        // DRIED → POWDER (粉砕)
        // ---------------------------------------------------------
        if (stage == STAGE_DRIED) {
            if (!level.isClientSide) {
                setStage(stack, STAGE_POWDER);

                level.playSound(null, player.getX(), player.getY(), player.getZ(),
                        SoundEvents.STONE_BREAK, SoundSource.PLAYERS, 1.0F, 1.2F);
            }
            return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
        }

        // ---------------------------------------------------------
        // POWDER + 水入り瓶(オフハンド) → KNEADED (練る)
        // ---------------------------------------------------------
        if (stage == STAGE_POWDER) {
            ItemStack off = player.getOffhandItem();

            if (isWaterBottle(off)) {
                if (!level.isClientSide) {
                    setStage(stack, STAGE_KNEADED);

                    // 水入り瓶をガラス瓶に
                    off.shrink(1);
                    ItemStack bottle = new ItemStack(net.minecraft.world.item.Items.GLASS_BOTTLE);
                    if (!player.getInventory().add(bottle)) {
                        player.drop(bottle, false);
                    }

                    level.playSound(null, player.getX(), player.getY(), player.getZ(),
                            SoundEvents.BREWING_STAND_BREW, SoundSource.PLAYERS, 1.0F, 1.0F);
                }
                return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
            }
        }

        // ---------------------------------------------------------
        // KNEADED + 凝灰岩(オフハンド) → HARDENED (固める)
        // ---------------------------------------------------------
        if (stage == STAGE_KNEADED) {
            ItemStack off = player.getOffhandItem();

            if (off.is(net.minecraft.world.item.Items.TUFF)) {
                if (!level.isClientSide) {
                    setStage(stack, STAGE_HARDENED);

                    // 凝灰岩を1個消費
                    off.shrink(1);

                    level.playSound(null, player.getX(), player.getY(), player.getZ(),
                            SoundEvents.BASALT_BREAK, SoundSource.PLAYERS, 1.0F, 0.8F);
                }
                return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
            }
        }

        return InteractionResultHolder.pass(stack);
    }

    /**
     * 水入り瓶かどうか。
     */
    private static boolean isWaterBottle(ItemStack stack) {
        if (!stack.is(net.minecraft.world.item.Items.POTION)) return false;
        CompoundTag tag = stack.getTag();
        if (tag == null) return false;
        if (!tag.contains("Potion")) return false;
        return tag.getString("Potion").equals("minecraft:water");
    }

    // =========================================================
    // Tooltip
    // =========================================================

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level,
                                List<Component> tooltip, TooltipFlag flag) {
        int stage = getStage(stack);
        switch (stage) {
            case STAGE_RAW -> {
                tooltip.add(Component.literal("§7ただのジャガイモ。").withStyle(ChatFormatting.GRAY));
                tooltip.add(Component.literal("§8水に投げ入れてみよう。").withStyle(ChatFormatting.DARK_GRAY));
            }
            case STAGE_WASHED -> {
                tooltip.add(Component.literal("§b洗ったジャガイモ。").withStyle(ChatFormatting.AQUA));
                tooltip.add(Component.literal("§8オフハンドに剣を持って右クリックで切る。").withStyle(ChatFormatting.DARK_GRAY));
            }
            case STAGE_CUT -> {
                tooltip.add(Component.literal("§e切ったジャガイモ。").withStyle(ChatFormatting.YELLOW));
                tooltip.add(Component.literal("§8足場の上に置いて2分待っても.....。").withStyle(ChatFormatting.DARK_GRAY));
            }
            case STAGE_DRIED -> {
                tooltip.add(Component.literal("§6乾燥した切ったジャガイモ。").withStyle(ChatFormatting.GOLD));
                tooltip.add(Component.literal("§8[右クリックで粉砕]").withStyle(ChatFormatting.DARK_GRAY));
            }
            case STAGE_POWDER -> {
                tooltip.add(Component.literal("§fジャガイモの粉。").withStyle(ChatFormatting.WHITE));
                tooltip.add(Component.literal("§8オフハンドに水入り瓶を持って右クリックで練る。").withStyle(ChatFormatting.DARK_GRAY));
            }
            case STAGE_KNEADED -> {
                tooltip.add(Component.literal("§d練ったジャガイモ。").withStyle(ChatFormatting.LIGHT_PURPLE));
                tooltip.add(Component.literal("§8オフハンドに凝灰岩を持って右クリックで固める。").withStyle(ChatFormatting.DARK_GRAY));
            }
            case STAGE_HARDENED -> {
                tooltip.add(Component.literal("§5固まった練ったジャガイモ入りボウル。").withStyle(ChatFormatting.DARK_PURPLE));
                tooltip.add(Component.literal("§8温泉に投げ入れよう。").withStyle(ChatFormatting.DARK_GRAY));
            }
        }
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        int stage = getStage(stack);
        return stage == STAGE_HARDENED || stage == STAGE_KNEADED;
    }
}