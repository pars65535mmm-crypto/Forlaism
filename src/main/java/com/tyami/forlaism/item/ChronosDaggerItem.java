package com.tyami.forlaism.item;

import com.tyami.forlaism.world.ChronosSlowManager;
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
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.Tiers;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.Level;

import javax.annotation.Nullable;
import java.util.List;

/**
 * クロノスダガー。
 *
 * 攻撃力 38 / 攻撃速度 21
 * 能力「時動停示」：右クリック1.2秒チャージ → 半径45mの敵を1/10減速（10秒）
 * クールタイム 11秒
 */
public class ChronosDaggerItem extends SwordItem implements IAnimatedTextItem {

    /** 攻撃速度（21）。 */
    public static final double ATTACK_SPEED = 21.0D;

    /** 攻撃力（38）。 */
    public static final float ATTACK_DAMAGE = 38.0F;

    /** チャージ必要tick。1.2秒 = 24 tick。 */
    public static final int CHARGE_TICKS = 24;

    /** 効果半径。45m。 */
    public static final double EFFECT_RADIUS = 45.0D;

    /** クールタイム。11秒 = 220 tick。 */
    public static final int COOLDOWN_TICKS = 220;

    private static final java.util.UUID ATK_UUID =
            java.util.UUID.fromString("c0c00000-0000-0000-0000-0000000000a1");
    private static final java.util.UUID SPD_UUID =
            java.util.UUID.fromString("c0c00000-0000-0000-0000-0000000000a2");

    public ChronosDaggerItem(Properties properties) {
        super(
                Tiers.NETHERITE,
                0,
                -2.4F,
                properties.stacksTo(1).fireResistant().rarity(Rarity.EPIC)
        );
    }

    // =========================================================
    // 属性
    // =========================================================

    @Override
    public com.google.common.collect.Multimap<net.minecraft.world.entity.ai.attributes.Attribute,
            net.minecraft.world.entity.ai.attributes.AttributeModifier>
    getDefaultAttributeModifiers(EquipmentSlot slot) {

        var original = super.getDefaultAttributeModifiers(slot);
        if (slot != EquipmentSlot.MAINHAND) return original;

        var builder = com.google.common.collect.ImmutableMultimap
                .<net.minecraft.world.entity.ai.attributes.Attribute,
                        net.minecraft.world.entity.ai.attributes.AttributeModifier>builder();

        original.forEach(builder::put);

        builder.put(
                net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_DAMAGE,
                new net.minecraft.world.entity.ai.attributes.AttributeModifier(
                        ATK_UUID,
                        "chronos_dagger_attack",
                        ATTACK_DAMAGE - 1.0D,
                        net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation.ADDITION
                )
        );

        builder.put(
                net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_SPEED,
                new net.minecraft.world.entity.ai.attributes.AttributeModifier(
                        SPD_UUID,
                        "chronos_dagger_speed",
                        ATTACK_SPEED - 4.0D,
                        net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation.ADDITION
                )
        );

        return builder.build();
    }

    // =========================================================
    // 右クリック：チャージ開始
    // =========================================================

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        if (player.getCooldowns().isOnCooldown(this)) {
            return InteractionResultHolder.fail(stack);
        }

        player.startUsingItem(hand);

        if (!level.isClientSide) {
            level.playSound(null, player.getX(), player.getY(), player.getZ(),
                    SoundEvents.BEACON_ACTIVATE, SoundSource.PLAYERS, 0.6F, 1.8F);
        }

        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);

    }

    @Override
    public int getUseDuration(ItemStack stack) {
        return 72000;
    }

    @Override
    public UseAnim getUseAnimation(ItemStack stack) {
        return UseAnim.BLOCK;
    }


    @Override
    public boolean canPerformAction(ItemStack stack, net.minecraftforge.common.ToolAction toolAction) {
        // Forgeのツールアクションで「盾のブロック（防御）」を許可します
        return net.minecraftforge.common.ToolActions.DEFAULT_SHIELD_ACTIONS.contains(toolAction);
    }

    // =========================================================
    // チャージ中：1.2秒で発動
    // =========================================================

    @Override
    public void onUseTick(Level level, LivingEntity user, ItemStack stack, int remainingUseTicks) {
        if (!(user instanceof ServerPlayer player)) return;

        int used = getUseDuration(stack) - remainingUseTicks;

        // チャージ演出
        if (level instanceof ServerLevel sl && used % 3 == 0) {
            double t = used / (double) CHARGE_TICKS;
            double r = 0.8 * (1.0 - t);

            for (int i = 0; i < 8; i++) {
                double angle = (i / 8.0) * Math.PI * 2 + used * 0.3;
                double px = player.getX() + Math.cos(angle) * r;
                double pz = player.getZ() + Math.sin(angle) * r;
                sl.sendParticles(
                        net.minecraft.core.particles.ParticleTypes.END_ROD,
                        px,
                        player.getY() + 1.0,
                        pz,
                        1, 0, 0, 0, 0
                );
            }
        }

        // 発動
        if (used >= CHARGE_TICKS) {
            if (level instanceof ServerLevel sl) {
                fire(sl, player, stack);
            }
            player.stopUsingItem();
        }
    }

    @Override
    public void releaseUsing(ItemStack stack, Level level, LivingEntity user, int timeLeft) {
        // 途中で離したら不発（クールタイムも付けない）
    }

    // =========================================================
    // 発動処理
    // =========================================================

    private static void fire(ServerLevel sl, ServerPlayer player, ItemStack stack) {

        // 周囲45mの敵を減速
        ChronosSlowManager.applyArea(
                sl,
                player.getX(), player.getY(), player.getZ(),
                EFFECT_RADIUS
        );

        // クールタイム
        player.getCooldowns().addCooldown(stack.getItem(), COOLDOWN_TICKS);


        player.addEffect(new net.minecraft.world.effect.MobEffectInstance(
                net.minecraft.world.effect.MobEffects.MOVEMENT_SPEED, 
                200, 
                1, 
                false, 
                false
        ));

        // 発動演出：中心から時空の波紋
        double cx = player.getX();
        double cy = player.getY() + 1.0;
        double cz = player.getZ();



        sl.sendParticles(
                net.minecraft.core.particles.ParticleTypes.FIREWORK,
                cx, cy, cz,
                20,          // 粒子の数
                0.2, 0.5, 0.2, // スポーンさせる範囲（X, Y, Zのブレ幅）
                0.15         // 飛び散る速度（スピード）
        );
        sl.sendParticles(
                net.minecraft.core.particles.ParticleTypes.PORTAL,
                cx, cy, cz,
                30,
                0.3, 0.6, 0.3,
                0.5
        );

        for (int ring = 1; ring <= 8; ring++) {
            double radius = ring * (EFFECT_RADIUS / 8.0);
            int points = 32 + ring * 4;
            for (int i = 0; i < points; i++) {
                double angle = (i / (double) points) * Math.PI * 2;
                double px = cx + Math.cos(angle) * radius;
                double pz = cz + Math.sin(angle) * radius;
                sl.sendParticles(
                        net.minecraft.core.particles.ParticleTypes.END_ROD,
                        px, cy + 0.1, pz,
                        1, 0, 0, 0, 0
                );
                if (ring % 2 == 0 && i % 3 == 0) {
                    sl.sendParticles(
                            net.minecraft.core.particles.ParticleTypes.SOUL_FIRE_FLAME,
                            px, cy + 0.1, pz,
                            1, 0, 0, 0, 0
                    );
                }
            }
        }

        // 音：時が止まる演出
        sl.playSound(null, cx, cy, cz,
                SoundEvents.WARDEN_SONIC_BOOM, SoundSource.PLAYERS, 2.0F, 0.5F);
        sl.playSound(null, cx, cy, cz,
                SoundEvents.BEACON_POWER_SELECT, SoundSource.PLAYERS, 1.5F, 0.7F);
        sl.playSound(null, cx, cy, cz,
                SoundEvents.END_PORTAL_SPAWN, SoundSource.PLAYERS, 1.0F, 1.8F);

    }

private static final AnimatedText CHRONOS_NAME =
            AnimatedText.of("クロノスダガー")
                    .gradient(0xFF0033AA, 0xFF66CCFF, 0xFFFFFFFF, 0xFF88DDFF, 0xFF0033AA)
                    .gradientSpeed(0.35F)
                    .gradientPhase(0.20F)
                    .sparkle(0xFFFFFFFF, 6.0F, 0.9F)
                    .outline(0x80000000, 1)
                    .glow(0x6066CCFF, 2);

    @Override
    public AnimatedText createAnimatedName(ItemStack stack) {
        return CHRONOS_NAME;
    }

    // =========================================================
    // Tooltip（時間・時空モチーフ）
    // =========================================================

    @Override
    public AnimatedText createAnimatedTooltip(ItemStack stack, int lineIndex) {

        // line 1：フレーバーテキスト
        // 「あの日までは…」の切なさを、色ノイズでざわつかせる
        if (lineIndex == 1) {
            return AnimatedText.of("希望を持っていれば救われるって、あの日までは....そう思ってたんだ。")
                    .typewriter(1, 10)
                    .withColor(0xFFAAAAAA)
                    .colorNoise(8, 0.5F, 0.3F);
        }

        // それ以外はバニラ描画に任せる
        return null;
    }


}