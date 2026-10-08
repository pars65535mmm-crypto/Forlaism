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
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.Level;

import javax.annotation.Nullable;
import java.util.List;
import java.util.UUID;

/**
 * 拳。
 *
 * ─ スペック ────────────────────────────────────
 *   見た目  : なし（透明・素手のまま振れる）
 *   攻撃力  : 1 (素手と同じ)
 *   攻撃速度: Integer.MAX_VALUE (21億)
 *   無敵時間: 無視（Mixin側で強制リセット）
 *
 * 「拳は見えない。だが確かにそこにある。」
 */
public class FistItem extends Item {

    /** 攻撃速度 21億。 */
    public static final double ATTACK_SPEED = Integer.MAX_VALUE;

    private static final UUID ATTACK_SPEED_UUID =
            UUID.fromString("f157f157-0001-0002-0003-000000000001");

    public FistItem(Properties properties) {
        super(properties
                .stacksTo(1)
                .rarity(Rarity.EPIC)
        );
    }

    // =========================================================
    // 属性（攻撃速度 21億）
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

        builder.put(
                Attributes.ATTACK_SPEED,
                new AttributeModifier(
                        ATTACK_SPEED_UUID,
                        "fist_attack_speed",
                        ATTACK_SPEED,
                        AttributeModifier.Operation.ADDITION
                )
        );

        return builder.build();
    }

    // =========================================================
    // 殴った時: ここでは何もしない（Mixinが本体）
    // =========================================================

    /** 1クリックあたりのヒット回数。 */
    public static final int HITS_PER_CLICK = 10;

    @Override
    public boolean hurtEnemy(ItemStack stack, LivingEntity target, LivingEntity attacker) {

        if (attacker.level().isClientSide) {
            return true;
        }

        net.minecraft.world.entity.player.Player player =
                attacker instanceof net.minecraft.world.entity.player.Player p ? p : null;

        // 1クリックで 10 回ヒット
        for (int i = 0; i < HITS_PER_CLICK; i++) {

            // 毎回無敵時間をリセット
            target.invulnerableTime = 0;
            target.hurtTime = 0;
            target.hurtDuration = 0;

            // ダメージ
            target.hurt(
                    attacker.damageSources().playerAttack(player),
                    1.0F
            );

            // 死んだら抜ける
            if (target.isDeadOrDying()) {
                break;
            }
        }

        return true;
    }

    // =========================================================
    // 素手モーション
    // =========================================================

    /**
     * 使うアニメーション。
     * 「素手を振る」ように見せたいので NONE を指定。
     */
    @Override
    public UseAnim getUseAnimation(ItemStack stack) {
        return UseAnim.NONE;
    }



    @Override
    public boolean isDamageable(ItemStack stack) {
        return false;
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return false;
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
        tooltip.add(Component.literal("§7これは見えません。")
                .withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.literal("§7拳です。")
                .withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.empty());
        tooltip.add(Component.literal("§c攻撃速度: §f∞")
                .withStyle(ChatFormatting.RED));
        tooltip.add(Component.literal("§6無敵時間を無視する")
                .withStyle(ChatFormatting.GOLD));
        tooltip.add(Component.empty());
        tooltip.add(Component.literal("§8連打すればするだけ強い。")
                .withStyle(ChatFormatting.DARK_GRAY));
    }
}