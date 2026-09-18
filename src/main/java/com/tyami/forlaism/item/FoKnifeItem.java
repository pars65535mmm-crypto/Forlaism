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
 * フォラリスの滲んだ包丁。
 *
 * - 攻撃力 3
 * - 攻撃速度 4.4
 * - 耐久値 800
 * - スニークしながら殴ると 0.3% で相手のHPの1%の追加ダメージ
 */
public class FoKnifeItem extends SwordItem {

    private static final Random RANDOM = new Random();
    private static final float BONUS_CHANCE = 0.003F; // 0.3%

    public FoKnifeItem(Properties properties) {
        super(
                Tiers.IRON,
                2,      // 攻撃力 3 (ベース2 + 1)
                -3.4F,  // 攻撃速度 4.4相当
                properties.durability(800)
        );
    }

    @Override
    public boolean hurtEnemy(ItemStack stack, LivingEntity target, LivingEntity attacker) {

        if (attacker instanceof Player player && player.isShiftKeyDown()) {
            if (RANDOM.nextFloat() < BONUS_CHANCE) {
                float bonus = target.getMaxHealth() * 0.01F;
                target.invulnerableTime = 0;
                target.hurt(
                        target.damageSources().playerAttack(player),
                        bonus
                );
            }
        }

        return super.hurtEnemy(stack, target, attacker);
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level,
                                List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.literal("§5フォラリスが滲み出している…"));
        tooltip.add(Component.literal("§7スニーク攻撃: 0.3%で相手のHPの1%追加ダメージ")
                .withStyle(net.minecraft.ChatFormatting.GRAY));
    }
}