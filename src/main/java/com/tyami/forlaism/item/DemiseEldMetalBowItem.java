package com.tyami.forlaism.item;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ArrowItem;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public class DemiseEldMetalBowItem extends BowItem {

    /**
     * アダメタル弓の最大チャージ時間。
     * 通常弓の20tickに対して1.5倍。
     */
    private static final int ADMETAL_MAX_DRAW_DURATION = 30;

    /**
     * 最大チャージ時の速度倍率。
     */
    private static final float SPEED_MULTIPLIER = 10.0F;

    public DemiseEldMetalBowItem(Properties properties) {
        super(properties);
    }

    /**
     * ここはVanilla弓と同じ。
     *
     * 72000tickにしておかないと、
     * 最大チャージ到達時に使用状態そのものが終了してしまう。
     */
    @Override
    public int getUseDuration(ItemStack stack) {
        return 72000;
    }

    @Override
    public void releaseUsing(
            ItemStack stack,
            Level level,
            LivingEntity entity,
            int timeLeft
    ) {
        if (!(entity instanceof Player player)) {
            return;
        }

        ItemStack projectileStack = player.getProjectile(stack);

        /*
         * 矢を持っていなければ発射しない。
         */
        if (projectileStack.isEmpty()) {
            return;
        }

        /*
         * 実際に弓を引いた時間。
         */
        int charge = this.getUseDuration(stack) - timeLeft;

        /*
         * 30tickでVanilla弓の最大チャージになるようにする。
         */
        float vanillaCharge =
                (float) charge * 20.0F / ADMETAL_MAX_DRAW_DURATION;

        /*
         * Vanilla BowItemのチャージ曲線を使用。
         */
        float power =
                BowItem.getPowerForTime((int) vanillaCharge);

        /*
         * 最小チャージ未満なら発射しない。
         */
        if (power < 0.1F) {
            return;
        }

        /*
         * アダメタル弓は10倍速。
         */
        power *= SPEED_MULTIPLIER;

        boolean infinite =
                player.getAbilities().instabuild
                        || net.minecraft.world.item.enchantment.EnchantmentHelper
                        .getItemEnchantmentLevel(
                                net.minecraft.world.item.enchantment.Enchantments.INFINITY_ARROWS,
                                stack
                        ) > 0;

        /*
         * サーバー側でのみ矢を生成。
         */
        if (!level.isClientSide) {

            if (!(projectileStack.getItem() instanceof ArrowItem arrowItem)) {
                return;
            }

            var arrow =
                    arrowItem.createArrow(
                            level,
                            projectileStack,
                            player
                    );

            /*
             * 最大チャージならクリティカル。
             */
            if (power >= SPEED_MULTIPLIER) {
                arrow.setCritArrow(true);
            }

            /*
             * 発射。
             */
            arrow.shootFromRotation(
                    player,
                    player.getXRot(),
                    player.getYRot(),
                    0.0F,
                    power,
                    1.0F
            );

            /*
             * Infinity / Creativeなら
             * 回収可能な通常矢ではなくする。
             */
            if (infinite) {
                arrow.pickup =
                        net.minecraft.world.entity.projectile.AbstractArrow.Pickup.CREATIVE_ONLY;
            }

            level.addFreshEntity(arrow);
        }

        /*
         * InfinityでもCreativeでもないなら矢を1本消費。
         */
        if (!infinite) {
            projectileStack.shrink(1);
        }

        /*
         * 弓の耐久を1減らす。
         */
        stack.hurtAndBreak(
                1,
                player,
                p -> p.broadcastBreakEvent(
                        player.getUsedItemHand()
                )
        );
    }
}