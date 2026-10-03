package com.tyami.forlaism.item;

import com.tyami.forlaism.ForalisItem;
import com.tyami.forlaism.damage.RinneDamageSource;
import com.tyami.watelib.item.IAnimatedTextItem;
import com.tyami.watelib.text.AnimatedText;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.Tiers;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import javax.annotation.Nullable;
import java.util.List;

/**
 * ディザスター。
 *
 * - 攻撃力 20
 * - 攻撃速度 2
 * - 耐久値 90,400,000
 * - クリティカル時のみ 輪廻ダメージ 5 を追加
 *
 * クラフト不可。村チェストに 0.3% で出現。
 */
public class DisasterItem extends SwordItem implements IAnimatedTextItem {

    /** 追加の輪廻ダメージ量。 */
    public static final float RINNE_BONUS = 5.0F;

    /** 耐久値。 */
    public static final int DURABILITY = 90_400_000;

    public DisasterItem(Properties properties) {
        super(
                Tiers.NETHERITE,
                19,      // 攻撃力 20 (ベース1 + 19)
                2.0F,   // 攻撃速度 2.0相当 (ベース4.0 - 2.0)
                properties
                        .stacksTo(1)
                        .durability(DURABILITY)
                        .fireResistant()
                        .rarity(Rarity.EPIC)
        );
    }

    // =========================================================
    // 攻撃: クリティカル時のみ輪廻ダメージ追加
    // =========================================================

    @Override
    public boolean hurtEnemy(ItemStack stack, LivingEntity target, LivingEntity attacker) {

        // クリティカル判定
        // バニラのクリティカル条件:
        //   - 落下中
        //   - ダッシュしていない
        //   - 盲目でない
        //   - 乗り物に乗っていない
        //   - 水泳中でない
        //   - クールダウン完了
        boolean isCritical =
                attacker.fallDistance > 0.0F
                        && !attacker.onGround()
                        && !attacker.onClimbable()
                        && !attacker.isInWater()
                        && !attacker.isPassenger()
                        && !attacker.hasEffect(net.minecraft.world.effect.MobEffects.BLINDNESS)
                        && !((Player) attacker).isSprinting()
                        && attacker instanceof Player p
                        && p.getAttackStrengthScale(0.5F) > 0.9F;

        // クリティカルなら輪廻ダメージ追加
        if (isCritical && !attacker.level().isClientSide
                && attacker.level() instanceof ServerLevel serverLevel) {

            target.invulnerableTime = 0;
            target.hurtTime = 0;

            target.hurt(
                    RinneDamageSource.of(serverLevel, attacker),
                    RINNE_BONUS
            );
        }

        return super.hurtEnemy(stack, target, attacker);
    }

    // =========================================================
    // 表示名
    // =========================================================

    @Override
    public AnimatedText createAnimatedName(ItemStack stack) {
        return AnimatedText.of("ディザスター")
                .gradient(0xFF330000, 0xFFFF4400, 0xFFFFDD00, 0xFFFF4400)
                .gradientSpeed(0.8F)
                .gradientPhase(0.7F);
    }



    @Override
    public boolean isFoil(ItemStack stack) {
        return true;
    }

    // =========================================================
    // 攻撃力/攻撃速度Modifier
    // =========================================================

    private static final java.util.UUID ATTACK_SPEED_UUID =
            java.util.UUID.fromString("d157a57e-1111-2222-3333-444444444401");

    @Override
    public com.google.common.collect.Multimap<net.minecraft.world.entity.ai.attributes.Attribute,
            net.minecraft.world.entity.ai.attributes.AttributeModifier>
    getDefaultAttributeModifiers(net.minecraft.world.entity.EquipmentSlot slot) {

        var original = super.getDefaultAttributeModifiers(slot);
        if (slot != net.minecraft.world.entity.EquipmentSlot.MAINHAND) return original;

        var builder = com.google.common.collect.ImmutableMultimap
                .<net.minecraft.world.entity.ai.attributes.Attribute,
                        net.minecraft.world.entity.ai.attributes.AttributeModifier>builder();

        original.forEach(builder::put);

        // 攻撃速度を2.0に調整 (ADDITION で補正)
        builder.put(
                Attributes.ATTACK_SPEED,
                new net.minecraft.world.entity.ai.attributes.AttributeModifier(
                        ATTACK_SPEED_UUID,
                        "disaster_attack_speed",
                        -2.0D,
                        net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation.ADDITION
                )
        );

        return builder.build();
    }
}