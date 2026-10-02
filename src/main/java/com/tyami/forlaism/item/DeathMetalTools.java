package com.tyami.forlaism.item;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

/**
 * デスメタルのツール群。
 *
 * 破壊時に 1XP を獲得する。
 * 内部で PickaxeItem / AxeItem などを継承し、onBlockStartBreak をフックする。
 */
public final class DeathMetalTools {

    private DeathMetalTools() {}

    /** 共通: ブロック破壊時に1XP付与。 */
    private static void awardXp(LivingEntity entity) {
        if (entity == null) return;
        if (entity.level().isClientSide) return;
        if (entity instanceof Player p) {
            p.giveExperiencePoints(1);
        }
    }

    // =========================================================
    // Pickaxe
    // =========================================================
    public static class Pickaxe extends PickaxeItem {
        public Pickaxe() {
            super(DeathMetalTier.INSTANCE, 1, -2.8F,
                    new Item.Properties().fireResistant());
        }

        @Override
        public boolean mineBlock(ItemStack stack, Level level, BlockState state,
                                 BlockPos pos, LivingEntity entity) {
            boolean result = super.mineBlock(stack, level, state, pos, entity);
            if (state.getDestroySpeed(level, pos) != 0.0F) {
                awardXp(entity);
            }
            return result;
        }
    }

    // =========================================================
    // Axe
    // =========================================================
    public static class Axe extends AxeItem {
        public Axe() {
            super(DeathMetalTier.INSTANCE, 5.0F, -3.0F,
                    new Item.Properties().fireResistant());
        }

        @Override
        public boolean mineBlock(ItemStack stack, Level level, BlockState state,
                                 BlockPos pos, LivingEntity entity) {
            boolean result = super.mineBlock(stack, level, state, pos, entity);
            if (state.getDestroySpeed(level, pos) != 0.0F) {
                awardXp(entity);
            }
            return result;
        }
    }

    // =========================================================
    // Shovel
    // =========================================================
    public static class Shovel extends ShovelItem {
        public Shovel() {
            super(DeathMetalTier.INSTANCE, 1.5F, -3.0F,
                    new Item.Properties().fireResistant());
        }

        @Override
        public boolean mineBlock(ItemStack stack, Level level, BlockState state,
                                 BlockPos pos, LivingEntity entity) {
            boolean result = super.mineBlock(stack, level, state, pos, entity);
            if (state.getDestroySpeed(level, pos) != 0.0F) {
                awardXp(entity);
            }
            return result;
        }
    }

    // =========================================================
    // Hoe
    // =========================================================
    public static class Hoe extends HoeItem {
        public Hoe() {
            super(DeathMetalTier.INSTANCE, -2, 0.0F,
                    new Item.Properties().fireResistant());
        }

        @Override
        public boolean mineBlock(ItemStack stack, Level level, BlockState state,
                                 BlockPos pos, LivingEntity entity) {
            boolean result = super.mineBlock(stack, level, state, pos, entity);
            if (state.getDestroySpeed(level, pos) != 0.0F) {
                awardXp(entity);
            }
            return result;
        }
    }
}