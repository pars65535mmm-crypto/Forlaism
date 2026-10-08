package com.tyami.forlaism.item;

import com.tyami.forlaism.damage.FrostbiteExecution;
import com.tyami.watelib.item.IAnimatedTextItem;
import com.tyami.watelib.text.AnimatedText;

import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.Tier;
import net.minecraft.world.item.Tiers;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import com.tyami.forlaism.entity.FraslatiaThrowEntity;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;

import javax.annotation.Nullable;
import java.util.List;
import java.util.UUID;

/**
 * フラスラティア / Fraslatia。
 *
 * 北欧神話の「ニヴルヘイムの死神」を模した鎌。
 *
 * - 攻撃力 50 / 攻撃速度 1
 * - 命中時に 冷凍 / 鈍足 / 凍傷 / 凍死 を付与
 */
public class FraslatiaItem extends SwordItem implements IAnimatedTextItem {

    /** 攻撃力。 */
    public static final float ATTACK_DAMAGE = 50.0F;

    /** 攻撃速度。 */
    public static final double ATTACK_SPEED = 1.0D;

    /** 攻撃力Modifier UUID。 */
    private static final UUID ATK_UUID =
            UUID.fromString("f7a571ce-1111-2222-3333-444444444401");
    private static final UUID SPD_UUID =
            UUID.fromString("f7a571ce-1111-2222-3333-444444444402");

    /** 冷凍時間（tick）。3秒。 */
    private static final int FREEZE_DURATION = 60;

    /** 鈍足時間（tick）。8秒。 */
    private static final int SLOW_DURATION = 160;

    /** 凍傷時間（tick）。10秒。 */
    private static final int FROSTBITE_DURATION = 200;

    public FraslatiaItem(Properties properties) {
        super(
                Tiers.NETHERITE,
                0,      // 実ダメージはAttributeModifierで
                -2.4F,
                properties.stacksTo(1).fireResistant().rarity(Rarity.EPIC)
        );
    }

    // =========================================================
    // 属性上書き
    // =========================================================

    @Override
    public com.google.common.collect.Multimap<net.minecraft.world.entity.ai.attributes.Attribute,
            AttributeModifier> getDefaultAttributeModifiers(net.minecraft.world.entity.EquipmentSlot slot) {

        var original = super.getDefaultAttributeModifiers(slot);
        if (slot != net.minecraft.world.entity.EquipmentSlot.MAINHAND) return original;

        var builder = com.google.common.collect.ImmutableMultimap
                .<net.minecraft.world.entity.ai.attributes.Attribute,
                        AttributeModifier>builder();
        original.forEach(builder::put);

        // 攻撃力: ベース 1 + 49 = 50
        builder.put(Attributes.ATTACK_DAMAGE,
                new AttributeModifier(ATK_UUID, "fraslatia_attack",
                        ATTACK_DAMAGE - 1, AttributeModifier.Operation.ADDITION));

        // 攻撃速度: 4.0 - 3.0 = 1.0
        builder.put(Attributes.ATTACK_SPEED,
                new AttributeModifier(SPD_UUID, "fraslatia_speed",
                        ATTACK_SPEED - 0.0D, AttributeModifier.Operation.ADDITION));

        return builder.build();
    }

    // =========================================================
    // 命中時
    // =========================================================

    @Override
    public boolean hurtEnemy(ItemStack stack, LivingEntity target, LivingEntity attacker) {

        if (!attacker.level().isClientSide && attacker.level() instanceof ServerLevel sl) {

            target.invulnerableTime = 0;
            target.hurtTime = 0;

            // =========================================================
            // 1. 冷凍（移動速度-90% + Slowness IV）
            // =========================================================
            target.addEffect(new MobEffectInstance(
                    MobEffects.MOVEMENT_SLOWDOWN,
                    FREEZE_DURATION, 3, false, true, true
            ));
            target.addEffect(new MobEffectInstance(
                    MobEffects.DIG_SLOWDOWN,
                    FREEZE_DURATION, 3, false, true, true
            ));

            // =========================================================
            // 2. 鈍足（Slowness II、8秒）
            // =========================================================
            target.addEffect(new MobEffectInstance(
                    MobEffects.MOVEMENT_SLOWDOWN,
                    SLOW_DURATION, 1, false, true, true
            ));

            // =========================================================
            // 3. 凍傷
            // =========================================================
            FrostbiteExecution.apply(target, FROSTBITE_DURATION);

            // =========================================================
            // 4. 命中演出（氷の斬撃）
            // =========================================================

            // 雪の結晶が舞う
            sl.sendParticles(
                    ParticleTypes.SNOWFLAKE,
                    target.getX(), target.getY() + target.getBbHeight() * 0.5, target.getZ(),
                    20, 0.5, 0.5, 0.5, 0.05
            );

            // 霜のオーラ
            sl.sendParticles(
                    ParticleTypes.WHITE_ASH,
                    target.getX(), target.getY() + target.getBbHeight() * 0.5, target.getZ(),
                    12, 0.4, 0.4, 0.4, 0.02
            );

            // 氷の破片
            sl.sendParticles(
                    ParticleTypes.ITEM_SNOWBALL,
                    target.getX(), target.getY() + 1.0, target.getZ(),
                    8, 0.3, 0.3, 0.3, 0.1
            );

            // 斬撃音
            sl.playSound(null,
                    target.getX(), target.getY(), target.getZ(),
                    SoundEvents.GLASS_BREAK,
                    SoundSource.PLAYERS, 0.8F, 1.8F
            );
            sl.playSound(null,
                    target.getX(), target.getY(), target.getZ(),
                    SoundEvents.PLAYER_HURT_FREEZE,
                    SoundSource.PLAYERS, 1.0F, 0.8F
            );

            // =========================================================
            // 5. 攻撃者を少し前進させる（重い一撃の演出）
            // =========================================================
            if (attacker instanceof Player player) {
                // ノックバック: 相手を少し後ろに
                target.knockback(
                        0.5D,
                        player.getX() - target.getX(),
                        player.getZ() - target.getZ()
                );
            }
        }

        return super.hurtEnemy(stack, target, attacker);
    }

    // =========================================================
    // 表示名
    // =========================================================

    @Override
    public AnimatedText createAnimatedName(ItemStack stack) {
        return AnimatedText.of("フラスラティア")
                .wave(2.5F, 0.20F, 0.45F)
                .gradient(
                        0xFF002244,   // 深い青
                        0xFF44AAFF,   // 氷青
                        0xFFFFFFFF,   // 白
                        0xFFAAEEFF,   // 淡い水色
                        0xFF002244    // 深い青
                )
                .gradientSpeed(0.8F)
                .gradientPhase(0.7F);
    }

    // =========================================================
    // Tooltip
    // =========================================================

    @Override
    public void appendHoverText(
            ItemStack stack, @Nullable Level level,
            List<Component> tooltip, TooltipFlag flag
    ) {
        tooltip.add(Component.literal("§b§lニヴルヘイムの死神の鎌。")
                .withStyle(ChatFormatting.AQUA));
        tooltip.add(Component.literal("攻撃力: 50 / 攻撃速度: 1")
                .withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.literal("§8氷の国より来たりし、死神の一振り。")
                .withStyle(ChatFormatting.DARK_GRAY));
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return true;
    }

        // =========================================================
    // 右クリック: 鎌を投げる
    // =========================================================

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        if (!level.isClientSide && level instanceof ServerLevel sl) {

            // 投擲体生成
            FraslatiaThrowEntity thrown = new FraslatiaThrowEntity(sl, player);

            // 発射位置: 目の少し前
            Vec3 eye = player.getEyePosition();
            Vec3 look = player.getLookAngle();
            Vec3 spawnPos = eye.add(look.scale(0.8));

            thrown.setPos(spawnPos.x, spawnPos.y - 0.2, spawnPos.z);
            thrown.shoot(look);

            sl.addFreshEntity(thrown);

            // 演出
            sl.playSound(null,
                    player.getX(), player.getY(), player.getZ(),
                    SoundEvents.TRIDENT_THROW,
                    SoundSource.PLAYERS, 1.0F, 0.8F
            );

            sl.sendParticles(
                    ParticleTypes.SNOWFLAKE,
                    spawnPos.x, spawnPos.y, spawnPos.z,
                    15, 0.3, 0.3, 0.3, 0.1
            );

            // 手元から鎌を消す
            if (!player.getAbilities().instabuild) {
                stack.shrink(1);
            }


        }

        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }

    /**
     * 鎌投擲中は手元から消えるから、耐久ゲージを非表示に。
     */
    @Override
    public boolean isDamageable(ItemStack stack) {
        return true;
    }
}