package com.tyami.forlaism.item;

import com.tyami.watelib.item.IAnimatedTextItem;
import com.tyami.watelib.text.AnimatedText;

import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.Multimap;

import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.Tiers;
import net.minecraft.world.level.block.state.BlockState;

import java.util.UUID;

public class SovereignScepterSwordItem
        extends SwordItem
        implements IAnimatedTextItem {

    /*
     * ============================================================
     * 君主乃笏剣 基本性能
     * ============================================================
     */

    /**
     * SwordItem側に渡す攻撃力。
     *
     * 実際の攻撃力は下のAttributeModifier側で調整する。
     */
    private static final int SOVEREIGN_ATTACK_DAMAGE = 64;

    /**
     * 実質的な追加攻撃力。
     *
     * 「ほぼ0」を狙う。
     */
    private static final double SOVEREIGN_ATTACK_DAMAGE_MODIFIER =
            1.01234562871209408478969820789679890876543213456789087654321345678908765432134567D;

    /**
     * 攻撃速度。
     *
     * Double.POSITIVE_INFINITYを直接Attributeへ入れるより、
     * Minecraftの属性計算で扱いやすい非常に大きな値を使用。
     */
    private static final double SOVEREIGN_ATTACK_SPEED =
            1_000_000D;

    /**
     * 採掘速度。
     *
     * 実質的に一瞬で掘る。
     */
    private static final float SOVEREIGN_DESTROY_SPEED =
           Integer.MAX_VALUE;

    /**
     * 可能な限り高い採掘レベル。
     */
    private static final int SOVEREIGN_TOOL_LEVEL =
            Integer.MAX_VALUE;

    /*
     * ============================================================
     * UUID
     * ============================================================
     */

    private static final UUID SOVEREIGN_ATTACK_DAMAGE_UUID =
            UUID.fromString(
                    "7f3b2f42-4c52-4d5e-9a8e-1e8b9f3c0002"
            );

    private static final UUID SOVEREIGN_ATTACK_SPEED_UUID =
            UUID.fromString(
                    "7f3b2f42-4c52-4d5e-9a8e-1e8b9f3c0001"
            );

    /*
     * ============================================================
     * Attribute
     * ============================================================
     */

    private final Multimap<Attribute, AttributeModifier>
            sovereignModifiers;

    /*
     * ============================================================
     * Constructor
     * ============================================================
     */

    public SovereignScepterSwordItem(Properties properties) {

        /*
         * SwordItemとしての基本設定。
         *
         * 攻撃力は0にして、実際のModifierは下で自前管理。
         */
        super(
                Tiers.NETHERITE,
                SOVEREIGN_ATTACK_DAMAGE,
                -2.4F,
                properties
        );

        ImmutableMultimap.Builder<Attribute, AttributeModifier> builder =
                ImmutableMultimap.builder();

        /*
         * --------------------------------------------------------
         * 攻撃力
         * --------------------------------------------------------
         *
         * ほぼ0。
         */
        builder.put(
                Attributes.ATTACK_DAMAGE,
                new AttributeModifier(
                        SOVEREIGN_ATTACK_DAMAGE_UUID,
                        "Sovereign attack damage",
                        SOVEREIGN_ATTACK_DAMAGE_MODIFIER,
                        AttributeModifier.Operation.ADDITION
                )
        );

        /*
         * --------------------------------------------------------
         * 攻撃速度
         * --------------------------------------------------------
         *
         * 事実上の超高速。
         */
        builder.put(
                Attributes.ATTACK_SPEED,
                new AttributeModifier(
                        SOVEREIGN_ATTACK_SPEED_UUID,
                        "Sovereign attack speed",
                        SOVEREIGN_ATTACK_SPEED,
                        AttributeModifier.Operation.ADDITION
                )
        );

        sovereignModifiers = builder.build();
    }

    /*
     * ============================================================
     * Attribute Modifiers
     * ============================================================
     */

    /**
     * メインハンドに持ったときの属性。
     */
    @Override
    public Multimap<Attribute, AttributeModifier>
    getDefaultAttributeModifiers(
            EquipmentSlot slot
    ) {

        if (slot == EquipmentSlot.MAINHAND) {
            return sovereignModifiers;
        }

        return super.getDefaultAttributeModifiers(slot);
    }

    /*
     * ============================================================
     * Mining
     * ============================================================
     */

    /**
     * 採掘速度。
     *
     * どのブロックに対しても極端に高速。
     */
    @Override
    public float getDestroySpeed(
            ItemStack stack,
            BlockState state
    ) {
        return SOVEREIGN_DESTROY_SPEED;
    }

    /**
     * すべてのブロックを適正ツールとして扱う。
     *
     * これによりドロップ条件を満たす方向へ持っていく。
     */
    @Override
    public boolean isCorrectToolForDrops(
            BlockState state
    ) {
        return true;
    }

    /*
     * ============================================================
     * Durability
     * ============================================================
     */

    /**
     * 耐久値を減らさない。
     */
    @Override
    public boolean isDamageable(
            ItemStack stack
    ) {
        return false;
    }

    /*
     * ============================================================
     * Display
     * ============================================================
     */

    @Override
    public AnimatedText createAnimatedName(
            ItemStack stack
    ) {

        return AnimatedText.of("君主乃笏剣（W.I.P.✧)")

                // 波
                .wave(
                        2.5F,
                        0.30F,
                        0.50F
                )

                // 黒 → 白 → 青
                .gradient(
                        0xFF000000,
                        0xFFFFFFFF,
                        0xFF0000FF
                )

                // 元0.08 → 3倍
                .gradientSpeed(0.6F)

                .gradientPhase(1.40F);
    }

    @Override
    public Rarity getRarity(
            ItemStack stack
    ) {
        return Rarity.EPIC;
    }

    /*
     * ============================================================
     * Combat
     * ============================================================
     */

    /**
     * 敵を殴ったとき。
     */
    @Override
    public boolean hurtEnemy(
            ItemStack stack,
            LivingEntity target,
            LivingEntity attacker
    ) {

        /*
         * 殴った敵を保存。
         */
        SovereignTargetData.saveTarget(
                stack,
                target
        );

        /*
         * Loot Table抽出。
         *
         * 1回ではなく5回。
         */
        if (!target.level().isClientSide
                && attacker instanceof net.minecraft.world.entity.player.Player player
                && target.level()
                        instanceof net.minecraft.server.level.ServerLevel serverLevel) {

            for (int i = 0; i < 5; i++) {

                SovereignLootExtractor.extract(
                        serverLevel,
                        target,
                        player,
                        stack
                );
            }
        }

        return true;
    }
}