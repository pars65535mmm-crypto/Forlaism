package com.tyami.forlaism.item;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SwordItem;

import java.util.Random;

/**
 * デスメタルの剣。
 *
 * HPが10%以下の敵に対して、10%の確率で21億ダメージを与える。
 */
public class DeathMetalSwordItem extends SwordItem {

    /** 21億ダメージ。 */
    public static final float INSTANT_KILL_DAMAGE = 2_100_000_000.0F;

    /** 発動閾値: 敵のHPが最大の10%以下。 */
    public static final float HP_THRESHOLD = 0.10F;

    /** 発動確率: 10%。 */
    public static final float PROC_CHANCE = 0.10F;

    private static final Random RANDOM = new Random();

    public DeathMetalSwordItem() {
        super(
                DeathMetalTier.INSTANCE,
                3,        // 攻撃力: ダイヤ剣と同じベース
                -2.4F,    // 攻撃速度: ダイヤ剣と同じ
                new net.minecraft.world.item.Item.Properties().fireResistant()
        );
    }

    @Override
    public boolean hurtEnemy(ItemStack stack, LivingEntity target, LivingEntity attacker) {

        if (!target.level().isClientSide) {
            // HP閾値チェック
            float maxHp = target.getMaxHealth();
            float currentHp = target.getHealth();

            if (maxHp > 0.0F && currentHp / maxHp <= HP_THRESHOLD) {
                if (RANDOM.nextFloat() < PROC_CHANCE) {
                    // 21億ダメージを素通しで叩き込む
                    // → 無敵時間・防具・エンチャ無視で直接HPを吹き飛ばす
                    target.invulnerableTime = 0;
                    target.hurtTime = 0;

                    float newHp = Math.max(0.0F, currentHp - INSTANT_KILL_DAMAGE);
                    target.setHealth(newHp);

                    if (newHp <= 0.0F && !target.isDeadOrDying()) {
                        target.die(
                                target.damageSources().playerAttack(
                                        attacker instanceof Player p ? p : null
                                )
                        );
                    }
                }
            }
        }

        return super.hurtEnemy(stack, target, attacker);
    }
}