package com.tyami.forlaism.item;

import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.Tiers;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Random;

/**
 * アサシンナイフ。
 *
 * - 攻撃力 8
 * - 攻撃速度 4
 * - 耐久値 1000
 * - どんなMobでも0.1%で即死
 */
public class AssassinKnifeItem extends SwordItem {

    private static final Random RANDOM = new Random();
    private static final float INSTANT_KILL_CHANCE = 0.001F; // 0.1%

    public AssassinKnifeItem(Properties properties) {
        super(
                Tiers.DIAMOND,
                7,      // 攻撃力 8
                -3.0F,  // 攻撃速度 4.0相当
                properties.durability(1000)
        );
    }

    @Override
    public boolean hurtEnemy(ItemStack stack, LivingEntity target, LivingEntity attacker) {

        if (!target.level().isClientSide) {
            if (RANDOM.nextFloat() < INSTANT_KILL_CHANCE) {
                target.invulnerableTime = 0;
                target.setHealth(0.0F);
                if (!target.isDeadOrDying()) {
                    target.die(
                            target.damageSources().playerAttack(
                                    attacker instanceof Player p ? p : null
                            )
                    );
                }
            }
        }

        return super.hurtEnemy(stack, target, attacker);
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level,
                                List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.literal("§4暗殺者の一撃。"));
        tooltip.add(Component.literal("§70.1%で即死")
                .withStyle(net.minecraft.ChatFormatting.GRAY));
    }
}