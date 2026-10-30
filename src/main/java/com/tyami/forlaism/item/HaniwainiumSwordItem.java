package com.tyami.forlaism.item;

import com.tyami.watelib.item.IAnimatedTextItem;
import com.tyami.watelib.text.AnimatedText;

import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.Multimap;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Random;
import java.util.UUID;

/**
 * ハニワイニウムの剣。
 *
 * 攻撃力: 32
 * 攻撃速度: ダイヤ相当 (4.0 - 2.4 = 1.6)
 * 耐久値: 999,999
 *
 * 能力:
 *   土葬 : 右クリック / CT 10秒 / 周囲10マスの敵を土葬 / 3%即死
 *   地震 : Shift + 右クリック / CT 20秒 / 画面揺れ + 5秒毎秒10ダメ + 鈍足5(3秒) / 範囲30
 */
public class HaniwainiumSwordItem extends SwordItem
        implements IAnimatedTextItem {

    /** 攻撃力 32。 */
    public static final float ATTACK_DAMAGE = 32.0F;

    /** 攻撃速度 ダイヤ相当。 */
    public static final double ATTACK_SPEED = 1.6D;

    /** 耐久値。 */
    public static final int DURABILITY = 999_999;

    // =========================================================
    // 土葬
    // =========================================================

    public static final double BURIAL_RADIUS = 10.0D;
    public static final int BURIAL_COOLDOWN_TICKS = 200; // 10秒
    public static final float BURIAL_INSTANT_KILL_CHANCE = 0.03F; // 3%

    // =========================================================
    // 地震
    // =========================================================

    public static final double QUAKE_RADIUS = 30.0D;
    public static final int QUAKE_COOLDOWN_TICKS = 400; // 20秒
    public static final int QUAKE_DURATION_TICKS = 100; // 5秒
    public static final float QUAKE_DAMAGE_PER_SECOND = 10.0F;
    public static final int QUAKE_SLOW_DURATION_TICKS = 60; // 3秒
    public static final int QUAKE_SLOW_AMPLIFIER = 4; // 鈍足5 (amp 4)

    private static final Random RANDOM = new Random();

    private static final UUID ATK_UUID =
            UUID.fromString("a1b2c3d4-1111-2222-3333-444444444401");
    private static final UUID SPD_UUID =
            UUID.fromString("a1b2c3d4-1111-2222-3333-444444444402");

    public HaniwainiumSwordItem(Properties properties) {
        super(
                HaniwainiumTier.INSTANCE,
                0,
                -2.4F,
                properties.stacksTo(1).durability(DURABILITY).fireResistant().rarity(Rarity.EPIC)
        );
    }

    // =========================================================
    // 属性
    // =========================================================

    @Override
    public Multimap<Attribute, AttributeModifier> getDefaultAttributeModifiers(EquipmentSlot slot) {
        Multimap<Attribute, AttributeModifier> original = super.getDefaultAttributeModifiers(slot);
        if (slot != EquipmentSlot.MAINHAND) return original;

        ImmutableMultimap.Builder<Attribute, AttributeModifier> builder = ImmutableMultimap.builder();
        original.forEach(builder::put);

        builder.put(Attributes.ATTACK_DAMAGE,
                new AttributeModifier(ATK_UUID, "haniwainium_attack",
                        ATTACK_DAMAGE - 1.0D,
                        AttributeModifier.Operation.ADDITION));

        builder.put(Attributes.ATTACK_SPEED,
                new AttributeModifier(SPD_UUID, "haniwainium_speed",
                        ATTACK_SPEED - 4.0D,
                        AttributeModifier.Operation.ADDITION));

        return builder.build();
    }

    // =========================================================
    // 表示名
    // =========================================================

    @Override
    public AnimatedText createAnimatedName(ItemStack stack) {
        return AnimatedText.of("ハニワイニウムの剣")
                .wave(2.5F, 0.30F, 0.50F)
                .gradient(0xFF8B4513, 0xFFD2B48C, 0xFFFFFFFF, 0xFFD2B48C, 0xFF8B4513)
                .gradientSpeed(0.7F)
                .gradientPhase(0.6F);
    }

    // =========================================================
    // 右クリック（土葬 / Shift で地震）
    // =========================================================

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        if (level.isClientSide) {
            return InteractionResultHolder.sidedSuccess(stack, true);
        }

        if (!(level instanceof ServerLevel sl) || !(player instanceof ServerPlayer sp)) {
            return InteractionResultHolder.pass(stack);
        }

        // Shift + 右クリック → 地震
        if (player.isShiftKeyDown()) {
            if (player.getCooldowns().isOnCooldown(this)) {
                sp.displayClientMessage(
                        Component.literal("§7地震はまだクールダウン中だ…"), true);
                return InteractionResultHolder.fail(stack);
            }

            HaniwainiumBurialHandler.castQuake(sl, sp);
            player.getCooldowns().addCooldown(this, QUAKE_COOLDOWN_TICKS);

            return InteractionResultHolder.sidedSuccess(stack, false);
        }

        // 通常右クリック → 土葬
        if (player.getCooldowns().isOnCooldown(this)) {
            sp.displayClientMessage(
                    Component.literal("§7土葬はまだクールダウン中だ…"), true);
            return InteractionResultHolder.fail(stack);
        }

        HaniwainiumBurialHandler.castBurial(sl, sp);
        player.getCooldowns().addCooldown(this, BURIAL_COOLDOWN_TICKS);

        return InteractionResultHolder.sidedSuccess(stack, false);
    }

    // =========================================================
    // Tooltip
    // =========================================================

    @Override
    public void appendHoverText(
            ItemStack stack,
            @Nullable Level level,
            List<Component> tooltip,
            TooltipFlag flag
    ) {
        tooltip.add(Component.literal("ハニワイニウムってなんなんでしょうか").withStyle(ChatFormatting.GOLD));
        tooltip.add(Component.empty());
    }


}