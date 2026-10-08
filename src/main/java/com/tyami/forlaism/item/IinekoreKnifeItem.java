package com.tyami.forlaism.item;

import com.tyami.forlaism.entity.IinekoreKnifeEntity;
import com.tyami.forlaism.registry.ModEntityTypes;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.Tiers;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;
import java.util.List;

/**
 * 投げナイフ。
 *
 * - 攻撃力 6 / 攻撃速度 4
 * - 右クリックで投げる（アイテムは減る）
 * - 敵ヒット→その場にドロップ
 * - ブロックヒット→刺さったまま、近づくと回収（矢方式）
 */
public class IinekoreKnifeItem extends SwordItem {

    /** 攻撃力。 */
    public static final float ATTACK_DAMAGE = 6.0F;

    /** 攻撃速度。 */
    public static final double ATTACK_SPEED = 4.0D;

    /** 投擲速度。 */
    public static final float THROW_SPEED = 2.0F;

    /** 投擲ダメージ。 */
    public static final float THROW_DAMAGE = 6.0F;

    public IinekoreKnifeItem(Properties properties) {
        super(
                Tiers.IRON,
                (int) ATTACK_DAMAGE - 1, // SwordItem は +1 されるので
                -2.0F,                    // 攻撃速度補正 (4.0 - 2.0 = 2.0系…実際は別途調整)
                properties.stacksTo(16)
        );
    }

    // =========================================================
    // 右クリック：投げる
    // =========================================================

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        if (!level.isClientSide && level instanceof ServerLevel sl) {

            IinekoreKnifeEntity knife = new IinekoreKnifeEntity(sl, player);
            knife.setItem(stack.copy());
            knife.setDamage(THROW_DAMAGE);

            Vec3 eye = player.getEyePosition();
            Vec3 look = player.getLookAngle().normalize();

            knife.setPos(
                    eye.x + look.x * 0.6,
                    eye.y + look.y * 0.6 - 0.1,
                    eye.z + look.z * 0.6
            );

            knife.shoot(look.x, look.y, look.z, THROW_SPEED, 0.0F);
            sl.addFreshEntity(knife);

            sl.playSound(null,
                    player.getX(), player.getY(), player.getZ(),
                    SoundEvents.TRIDENT_THROW,
                    SoundSource.PLAYERS,
                    0.8F, 1.5F
            );

            // アイテム消費
            if (!player.getAbilities().instabuild) {
                stack.shrink(1);
            }
        }

        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }

    // =========================================================
    // 攻撃力・攻撃速度
    // =========================================================

    private static final java.util.UUID ATK_UUID =
            java.util.UUID.fromString("a11b1c1d-2222-3333-4444-555555555501");
    private static final java.util.UUID SPD_UUID =
            java.util.UUID.fromString("a11b1c1d-2222-3333-4444-555555555502");

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

        // 攻撃力 6
        builder.put(
                net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_DAMAGE,
                new net.minecraft.world.entity.ai.attributes.AttributeModifier(
                        ATK_UUID,
                        "iinekore_knife_attack",
                        ATTACK_DAMAGE - 1.0D,
                        net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation.ADDITION
                )
        );

        // 攻撃速度 4.0
        builder.put(
                net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_SPEED,
                new net.minecraft.world.entity.ai.attributes.AttributeModifier(
                        SPD_UUID,
                        "iinekore_knife_speed",
                        ATTACK_SPEED - 4.0D,
                        net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation.ADDITION
                )
        );

        return builder.build();
    }

    // =========================================================
    // Tooltip
    // =========================================================

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level,
                                List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.literal("§7右クリックで投げる。")
                .withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.literal("§7刺さったナイフは近づくと回収できる。")
                .withStyle(ChatFormatting.GRAY));
    }
}