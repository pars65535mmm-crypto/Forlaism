package com.tyami.forlaism.world;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraftforge.common.ForgeMod;

import java.util.UUID;

/**
 * 重力反転の管理。
 *
 * 反転中は全LivingEntityのGRAVITY属性を負の値に上書きする。
 * 再使用で解除。
 */
public final class GravityInversionManager {

    private static final UUID GRAVITY_UUID =
            UUID.fromString("c10c4000-0000-0000-0000-000000000001");
    private static final String GRAVITY_NAME = "masterpiece_clock_gravity_inversion";

    /** 反転中の重力値（バニラは0.08）。 */
    private static final double INVERTED_GRAVITY = -0.08D;

    private static boolean active = false;

    private GravityInversionManager() {
    }

    public static boolean isActive() {
        return active;
    }

    public static void toggle() {
        active = !active;
    }

    public static void setActive(boolean value) {
        active = value;
    }

    /**
     * 対象LivingEntityに重力反転Modifierを適用/除去。
     * 毎tick呼ぶ想定。
     */
    public static void applyTo(LivingEntity entity) {
        AttributeInstance gravity = entity.getAttribute(ForgeMod.ENTITY_GRAVITY.get());
        if (gravity == null) return;

        AttributeModifier existing = gravity.getModifier(GRAVITY_UUID);

        if (!active) {
            if (existing != null) {
                gravity.removeModifier(GRAVITY_UUID);
            }
            return;
        }

        // 既存のバニラ値(0.08)を打ち消して -0.08 にする
        // → ADDITION で -0.16 を加える = 0.08 + (-0.16) = -0.08
        double target = INVERTED_GRAVITY - gravity.getBaseValue();

        if (existing == null) {
            gravity.addTransientModifier(new AttributeModifier(
                    GRAVITY_UUID, GRAVITY_NAME,
                    target,
                    AttributeModifier.Operation.ADDITION
            ));
        } else if (existing.getAmount() != target) {
            gravity.removeModifier(GRAVITY_UUID);
            gravity.addTransientModifier(new AttributeModifier(
                    GRAVITY_UUID, GRAVITY_NAME,
                    target,
                    AttributeModifier.Operation.ADDITION
            ));
        }
    }
}