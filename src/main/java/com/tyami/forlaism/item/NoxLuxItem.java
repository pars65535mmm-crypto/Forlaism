package com.tyami.forlaism.item;

import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.Multimap;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.Tiers;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import javax.annotation.Nullable;
import java.util.List;
import java.util.UUID;

/**
 * ノクスルクス。
 *
 * Lore: 夜に沈む剣
 *
 * ─ スペック ────────────────────────────────────
 *   攻撃力  : 15
 *   攻撃速度: 3
 *
 * ─ 能力: 〈月夜〉────────────────────────────────
 *   Shift押した瞬間に見ていたEntityを記録
 *   → 0.5秒間Shiftを押し続ける
 *   → 記録したEntityの背後にTP
 *   → 視線は記録したEntityに向く
 *   → 1秒以内に攻撃すると追撃〈月夜〉発動
 *
 *   〈月夜〉: 月光ダメージ 3 × 3回 + 0.3秒硬直
 *   月光: 防御貫通
 */
public class NoxLuxItem extends SwordItem {

    /** 攻撃力。 */
    public static final float ATTACK_DAMAGE = 15.0F;

    /** 攻撃速度。 */
    public static final double ATTACK_SPEED = 3.0D;

    /** 追撃〈月夜〉のダメージ量。 */
    public static final float MOONLIGHT_DAMAGE = 3.0F;

    /** 追撃〈月夜〉のヒット回数。 */
    public static final int MOONLIGHT_HITS = 3;

    /** 追撃〈月夜〉の硬直時間（tick）。0.3秒 = 6tick。 */
    public static final int MOONLIGHT_STUN_TICKS = 6;

    /** Shift長押しの必要時間（tick）。0.5秒 = 10tick。 */
    public static final int TP_CHARGE_TICKS = 10;

    /** TP後の追撃有効時間（tick）。1秒 = 20tick。 */
    public static final int PURSUIT_WINDOW_TICKS = 20;

    private static final UUID ATTACK_DAMAGE_UUID =
            UUID.fromString("a1b2c3d4-0001-0002-0003-000000000001");
    private static final UUID ATTACK_SPEED_UUID =
            UUID.fromString("a0b1c2d3-0001-0002-0003-000000000002");

    public NoxLuxItem(Properties properties) {
        super(
                Tiers.NETHERITE,
                0,   // 実ダメージは AttributeModifier で
                -2.4F,
                properties.stacksTo(1).fireResistant().rarity(Rarity.EPIC)
        );
    }

    // =========================================================
    // 属性上書き
    // =========================================================

    @Override
    public Multimap<Attribute, AttributeModifier> getDefaultAttributeModifiers(EquipmentSlot slot) {

        Multimap<Attribute, AttributeModifier> original =
                super.getDefaultAttributeModifiers(slot);

        if (slot != EquipmentSlot.MAINHAND) {
            return original;
        }

        ImmutableMultimap.Builder<Attribute, AttributeModifier> builder =
                ImmutableMultimap.builder();

        original.forEach(builder::put);

        // 攻撃力 15（ベース1 + 14）
        builder.put(
                Attributes.ATTACK_DAMAGE,
                new AttributeModifier(
                        ATTACK_DAMAGE_UUID,
                        "noxlux_attack",
                        ATTACK_DAMAGE - 1.0D,
                        AttributeModifier.Operation.ADDITION
                )
        );

        // 攻撃速度 3（ベース4.0 - 1.0）
        builder.put(
                Attributes.ATTACK_SPEED,
                new AttributeModifier(
                        ATTACK_SPEED_UUID,
                        "noxlux_speed",
                        ATTACK_SPEED - 2.0D,
                        AttributeModifier.Operation.ADDITION
                )
        );

        return builder.build();
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
        tooltip.add(Component.literal("§5夜に沈む剣")
                .withStyle(ChatFormatting.DARK_PURPLE));
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return true;
    }
}