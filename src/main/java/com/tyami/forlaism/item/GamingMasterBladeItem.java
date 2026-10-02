package com.tyami.forlaism.item;

import com.tyami.forlaism.client.gmb.GMBAbility;
import com.tyami.forlaism.client.gmb.GMBRenderer;
import com.tyami.watelib.item.IAnimatedTextItem;
import com.tyami.watelib.text.AnimatedText;

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
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.Tiers;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;

import javax.annotation.Nullable;
import java.util.List;
import java.util.UUID;

/**
 * GMB (Gaming Master Blade)
 *
 * Tier 7 隠しアイテム。
 *
 * 設計方針:
 *   - クラス本体は「見た目とベース性能」だけを持つ
 *   - 能力は GMBAbility に集約し、あとで registerFeature() で拡張可能
 *   - シェーダーは GMBShader / GMBRenderer で GMB 専用の虹色グリッチ
 *
 * 拡張方法:
 *   GMBAbility.registerFeature(...) を呼ぶだけ。
 */
public class GamingMasterBladeItem extends SwordItem implements IAnimatedTextItem {

    // =========================================================
    // 性能定数（あとで GMBAbility 側からも参照される）
    // =========================================================

    public static final float ATTACK_DAMAGE = 9999.0F;
    public static final double ATTACK_SPEED = 999.0D;

    private static final UUID ATK_UUID =
            UUID.fromString("6a11e700-1111-2222-3333-444444444401");
    private static final UUID SPD_UUID =
            UUID.fromString("6a11e700-1111-2222-3333-444444444402");

    public GamingMasterBladeItem(Properties properties) {
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
    public Multimap<Attribute, AttributeModifier> getDefaultAttributeModifiers(EquipmentSlot slot) {
        Multimap<Attribute, AttributeModifier> original =
                super.getDefaultAttributeModifiers(slot);

        if (slot != EquipmentSlot.MAINHAND) {
            return original;
        }

        ImmutableMultimap.Builder<Attribute, AttributeModifier> builder =
                ImmutableMultimap.builder();
        original.forEach(builder::put);

        builder.put(Attributes.ATTACK_DAMAGE,
                new AttributeModifier(ATK_UUID, "gmb_attack",
                        ATTACK_DAMAGE, AttributeModifier.Operation.ADDITION));

        builder.put(Attributes.ATTACK_SPEED,
                new AttributeModifier(SPD_UUID, "gmb_speed",
                        ATTACK_SPEED, AttributeModifier.Operation.ADDITION));

        return builder.build();
    }

    // =========================================================
    // 攻撃時 → GMBAbility に委譲（拡張可能）
    // =========================================================

    @Override
    public boolean hurtEnemy(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        if (attacker.level().isClientSide) return true;

        GMBAbility.onHit(stack, target, attacker);
        return true;
    }

    // =========================================================
    // 使用時（右クリック）→ GMBAbility に委譲
    // =========================================================

    @Override
    public net.minecraft.world.InteractionResultHolder<ItemStack> use(
            Level level, Player player, net.minecraft.world.InteractionHand hand
    ) {
        ItemStack stack = player.getItemInHand(hand);

        if (!level.isClientSide) {
            GMBAbility.onUse(stack, player);
        }

        return net.minecraft.world.InteractionResultHolder.sidedSuccess(
                stack, level.isClientSide
        );
    }

    // =========================================================
    // 耐久無効
    // =========================================================

    @Override
    public boolean isDamageable(ItemStack stack) {
        return false;
    }

    // =========================================================
    // 表示名（虹色グリッチ）
    // =========================================================

    @Override
    public AnimatedText createAnimatedName(ItemStack stack) {
        return AnimatedText.of("GMB")
                .wave(2.5f, 0.15f, 0.45f)
                .sway(1.8f, 0.10f, 0.30f)
                .gradient(
                        0xFFFF00FF,
                        0xFFFF0000,
                        0xFFFFFF00,
                        0xFF00FF00,
                        0xFF00FFFF,
                        0xFF0000FF,
                        0xFFFF00FF
                )
                .gradientSpeed(1.2f)
                .gradientPhase(0.3f)
                .glitch(3.0f, 0.30f, 0xFFFF00FF)
                .glow(0xA0FF00FF, 2);
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
        tooltip.add(Component.literal(""));
        tooltip.add(Component.literal("§d§l【Tier 7 / Gaming Master Blade】")
                .withStyle(ChatFormatting.LIGHT_PURPLE));
        tooltip.add(Component.literal("§c攻撃力: §f" + (int) ATTACK_DAMAGE)
                .withStyle(ChatFormatting.RED));
        tooltip.add(Component.literal("§c攻撃速度: §f" + (int) ATTACK_SPEED)
                .withStyle(ChatFormatting.RED));

        // =========================================================
        // GMBAbility から拡張機能の一覧を取得
        // =========================================================
        for (String line : GMBAbility.describeFeatures(stack)) {
            tooltip.add(Component.literal(line)
                    .withStyle(ChatFormatting.LIGHT_PURPLE));
        }

        tooltip.add(Component.literal("§8全てのゲームを制覇せし者の剣。")
                .withStyle(ChatFormatting.DARK_GRAY));

        if (net.minecraft.client.gui.screens.Screen.hasShiftDown()) {
            tooltip.add(Component.literal(""));
            tooltip.add(Component.literal("§6【GMB】").withStyle(ChatFormatting.GOLD));
            tooltip.add(Component.literal("§e・\"Master\" の名を冠するに相応しき一振り。")
                    .withStyle(ChatFormatting.YELLOW));
            tooltip.add(Component.literal("§e・右クリックで能力を発動。")
                    .withStyle(ChatFormatting.YELLOW));
            tooltip.add(Component.literal("§8…GG. ノーコンティニュー。")
                    .withStyle(ChatFormatting.DARK_GRAY));
        } else {
            tooltip.add(Component.literal("§8[Shift] で詳細を表示")
                    .withStyle(ChatFormatting.DARK_GRAY));
        }
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return true;
    }

    // =========================================================
    // クライアント側レンダリングフック（ItemRendererMixin から呼ばれる）
    // =========================================================

    /**
     * ワールド上のアイテム描画時に GMB 専用シェーダーで描画する。
     * ItemRendererMixin / ItemEntityRendererMixin から呼ばれる想定。
     */
    public static void renderGMBEffect(
            ItemStack stack,
            com.mojang.blaze3d.vertex.PoseStack poseStack,
            net.minecraft.client.renderer.MultiBufferSource buffer,
            boolean billboard
    ) {
        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () ->
                GMBRenderer.render(stack, poseStack, buffer, billboard)
        );
    }
}