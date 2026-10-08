package com.tyami.forlaism.item;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.EquipmentSlot;
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
 * SAKURAぁぁぁぁぁ。
 *
 * ネタ武器。
 * 攻撃力 1e22 / 防御力 +1e11
 */
public class SakuralalalalalalaItem extends SwordItem {

    /** 攻撃力 10^22。 */
    public static final double ATTACK_DAMAGE = 1.0e22;

    /** 防御力 +10^11。 */
    public static final double ARMOR_BONUS = 1.0e11;

private static final UUID ATK_UUID =
        UUID.fromString("5a000000-0000-0000-0000-000000000001");
private static final UUID ARMOR_UUID =
        UUID.fromString("5a000000-0000-0000-0000-000000000002");
        
    public SakuralalalalalalaItem(Properties properties) {
        super(
                Tiers.NETHERITE,
                0,
                -2.4F,
                properties.stacksTo(1).fireResistant().rarity(Rarity.EPIC)
        );
    }

    @Override
    public com.google.common.collect.Multimap<net.minecraft.world.entity.ai.attributes.Attribute,
            AttributeModifier> getDefaultAttributeModifiers(EquipmentSlot slot) {

        var original = super.getDefaultAttributeModifiers(slot);
        if (slot != EquipmentSlot.MAINHAND) return original;

        var builder = com.google.common.collect.ImmutableMultimap
                .<net.minecraft.world.entity.ai.attributes.Attribute,
                        AttributeModifier>builder();

        original.forEach(builder::put);

        builder.put(Attributes.ATTACK_DAMAGE,
                new AttributeModifier(ATK_UUID, "sakura_attack",
                        ATTACK_DAMAGE - 1.0D,
                        AttributeModifier.Operation.ADDITION));

        builder.put(Attributes.ARMOR,
                new AttributeModifier(ARMOR_UUID, "sakura_armor",
                        ARMOR_BONUS,
                        AttributeModifier.Operation.ADDITION));

        return builder.build();
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level,
                                List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.literal("§d§lSAKURAぁぁぁぁぁ")
                .withStyle(ChatFormatting.LIGHT_PURPLE));
        tooltip.add(Component.literal("§7待ち時間エグイ..まだか..まだなんか..")
                .withStyle(ChatFormatting.GRAY));
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return true;
    }
}