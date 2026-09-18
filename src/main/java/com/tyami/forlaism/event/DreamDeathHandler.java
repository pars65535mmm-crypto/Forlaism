package com.tyami.forlaism.event;

import com.tyami.forlaism.world.DreamDimension;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.UUID;

/**
 * Dreamディメンションでの死亡処理。
 *
 * 挙動:
 *   1. 死亡をキャンセル
 *   2. 最大体力を永続で -1
 *   3. HPを全快してその場で復活
 *   4. 最大体力が 1 になったら 0, -100000, 0 へ強制転送（詰み）
 */
@Mod.EventBusSubscriber(modid = "forlaism")
public final class DreamDeathHandler {

    /** 最大体力を減らす量。 */
    private static final double MAX_HEALTH_LOSS = 1.0D;

    /** 最大体力の下限。 */
    private static final double MIN_MAX_HEALTH = 1.0D;

    /** 詰み座標。 */
    private static final double ABYSS_X = 0.5D;
    private static final double ABYSS_Y = -100000.0D;
    private static final double ABYSS_Z = 0.5D;

    /** 永続デバフのModifier UUID (プレイヤーごとに固定でもOK)。 */
    private static final UUID DREAM_DECAY_UUID =
            UUID.fromString("d7e4a4f0-1111-4a2b-9c3d-000000000001");

    private static final String DREAM_DECAY_NAME = "Dream Health Decay";

    private DreamDeathHandler() {
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onDeath(LivingDeathEvent event) {

        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }

        // Dreamディメンション以外は通常処理
        if (!player.level().dimension().equals(DreamDimension.DREAM_LEVEL)) {
            return;
        }

        // 死亡をキャンセル
        event.setCanceled(true);

        // 最大体力を1減らす（永続）
        reduceMaxHealth(player);

        // HP全快
        player.setHealth(player.getMaxHealth());

        // デス画面/演出リセット
        player.revive();
        player.clearFire();
        player.setDeltaMovement(Vec3.ZERO);

        // 最大体力が1以下になったら詰み座標へ
        if (player.getMaxHealth() <= MIN_MAX_HEALTH) {
            sendToAbyss(player);
        } else {
            player.displayClientMessage(
                    Component.literal(
                            "§5夢に喰われた… §c最大体力 -1 §7(残り: "
                                    + (int) player.getMaxHealth() + ")"
                    ),
                    true
            );
        }
    }

    /**
     * 最大体力を永続で1減らす。
     */
    private static void reduceMaxHealth(ServerPlayer player) {

        AttributeInstance maxHealth = player.getAttribute(Attributes.MAX_HEALTH);

        if (maxHealth == null) {
            return;
        }

        // 既存のModifierを取得
        AttributeModifier existing = maxHealth.getModifier(DREAM_DECAY_UUID);

        double currentAmount = existing != null ? existing.getAmount() : 0.0D;

        // 新しいModifier値（負の値で減らす）
        double newAmount = currentAmount - MAX_HEALTH_LOSS;

        // 既存を除去
        if (existing != null) {
            maxHealth.removeModifier(DREAM_DECAY_UUID);
        }

        // 減らしすぎ防止（最大体力が1を下回らないように）
        double baseMaxHealth = maxHealth.getBaseValue();
        double finalMaxHealth = baseMaxHealth + newAmount;

        if (finalMaxHealth < MIN_MAX_HEALTH) {
            newAmount = MIN_MAX_HEALTH - baseMaxHealth;
        }

        // 永続Modifierを追加
        maxHealth.addPermanentModifier(
                new AttributeModifier(
                        DREAM_DECAY_UUID,
                        DREAM_DECAY_NAME,
                        newAmount,
                        AttributeModifier.Operation.ADDITION
                )
        );

        // 現在HPが新しい最大値を超えていたら合わせる
        if (player.getHealth() > player.getMaxHealth()) {
            player.setHealth(player.getMaxHealth());
        }
    }

    /**
     * 最大体力1の詰み状態。
     *
     * Dreamの 0, -100000, 0 へ転送し、リスポーン地点もそこに固定する。
     */
    private static void sendToAbyss(ServerPlayer player) {

        ServerLevel dream = player.server.getLevel(DreamDimension.DREAM_LEVEL);

        if (dream == null) {
            return;
        }

        // 転送
        player.teleportTo(
                dream,
                ABYSS_X,
                ABYSS_Y,
                ABYSS_Z,
                player.getYRot(),
                player.getXRot()
        );

        // リスポーン地点を奈落に固定（詰み確定）
        BlockPos abyssPos = BlockPos.containing(ABYSS_X, ABYSS_Y, ABYSS_Z);
        player.setRespawnPosition(
                DreamDimension.DREAM_LEVEL,
                abyssPos,
                player.getYRot(),
                true,
                false
        );

        player.displayClientMessage(
                Component.literal("§4§l夢の淵へようこそ。 §7もう戻れない…"),
                true
        );
    }


    /**
     * Mixin から呼ばれる公開ヘルパー。
     */
    public static void handleDreamDeath(ServerPlayer player, DamageSource source) {

        reduceMaxHealth(player);

        player.revive();
        player.clearFire();
        player.setHealth(player.getMaxHealth());
        player.setDeltaMovement(Vec3.ZERO);

        if (player.getMaxHealth() <= MIN_MAX_HEALTH) {
            sendToAbyss(player);
        } else {
            player.displayClientMessage(
                    Component.literal(
                            "§5夢に喰われた… §c最大体力 -1 §7(残り: "
                                    + (int) player.getMaxHealth() + ")"
                    ),
                    true
            );
        }
    }
}