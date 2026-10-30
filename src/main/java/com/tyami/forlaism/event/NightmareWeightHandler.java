package com.tyami.forlaism.event;

import com.tyami.forlaism.world.NightmareHelper;

import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ItemStack;

import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.UUID;

/**
 * ナイトメアモード中の装備重量システム。
 *
 * - 各防具の「防御力」を重さとして扱う
 * - 重量合計 × 0.02 だけ移動速度を低下させる
 *
 * 例:
 *   防御力合計 20 → -0.4 (40%減速)
 */
@Mod.EventBusSubscriber(modid = "forlaism")
public final class NightmareWeightHandler {

    private static final UUID WEIGHT_UUID =
            UUID.fromString("f00db0ba-1234-5678-9abc-def012345678");

    private static final String WEIGHT_NAME = "nightmare_equipment_weight";

    /** 1重量あたりの速度低下率。 */
    public static final double WEIGHT_MULTIPLIER = 0.02D;

    /** 速度低下の上限（-0.9 = 90%減速まで）。 */
    public static final double MAX_SLOWDOWN = -0.9D;

    private NightmareWeightHandler() {
    }

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;

        Player player = event.player;
        if (player.level().isClientSide) return;

        AttributeInstance speed = player.getAttribute(Attributes.MOVEMENT_SPEED);
        if (speed == null) return;

        // ナイトメアじゃなければ解除
        if (!NightmareHelper.isActive(player)) {
            if (speed.getModifier(WEIGHT_UUID) != null) {
                speed.removeModifier(WEIGHT_UUID);
            }
            return;
        }

        // 装備重量を計算
        double weight = calculateWeight(player);

        if (weight <= 0.0D) {
            if (speed.getModifier(WEIGHT_UUID) != null) {
                speed.removeModifier(WEIGHT_UUID);
            }
            return;
        }

        // ペナルティ値（マイナス）
        double modifier = Math.max(MAX_SLOWDOWN, -(weight * WEIGHT_MULTIPLIER));

        AttributeModifier existing = speed.getModifier(WEIGHT_UUID);

        if (existing != null && existing.getAmount() == modifier) {
            return;
        }

        if (existing != null) {
            speed.removeModifier(WEIGHT_UUID);
        }

        speed.addTransientModifier(new AttributeModifier(
                WEIGHT_UUID,
                WEIGHT_NAME,
                modifier,
                AttributeModifier.Operation.MULTIPLY_TOTAL
        ));
    }

    /**
     * プレイヤーの装備重量を計算する。
     *
     * 各防具の「防御力」を合計する。
     * （ヘルメット + チェスト + レギンス + ブーツ）
     */
    private static double calculateWeight(Player player) {

        double weight = 0.0D;

        for (EquipmentSlot slot : new EquipmentSlot[]{
                EquipmentSlot.HEAD,
                EquipmentSlot.CHEST,
                EquipmentSlot.LEGS,
                EquipmentSlot.FEET
        }) {
            ItemStack stack = player.getItemBySlot(slot);
            if (stack.isEmpty()) continue;

            if (stack.getItem() instanceof ArmorItem armor) {
                weight += armor.getDefense();
            } else {
                // 防具じゃないもの（カボチャとか）は軽く1とする
                weight += 1.0D;
            }
        }

        return weight;
    }
}