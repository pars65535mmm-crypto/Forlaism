package com.tyami.forlaism.event;

import com.tyami.forlaism.item.SakuraginiumArmorMaterial;
import com.tyami.forlaism.item.SenpaiFireAxeItem;
import com.tyami.forlaism.registry.Items;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.Comparator;
import java.util.List;
import java.util.UUID;

/**
 * サクラギニウム バーサーモード ハンドラー。
 *
 * 発動条件:
 *   ・サクラギニウム防具フルセット
 *   ・メインハンド & オフハンド が 先輩の消防斧
 *
 * 効果中:
 *   ・攻撃力 ×3  (MULTIPLY_TOTAL で +200%)
 *   ・移動速度 ×1.5 (MULTIPLY_TOTAL で +50%)
 *   ・HP自然回復 ×3 (毎20tickごとに heal(1.0F) を3回 → 実質3倍)
 *   ・常に最寄りの敵へ視線ロック
 *   ・衝撃吸収 Lv3 (フルセット時の常時効果)
 *
 * 解除時:
 *   ・鈍足 Lv3 を 10秒 (200tick)
 *   ・「バーサーキレ」演出
 */
@Mod.EventBusSubscriber(modid = "forlaism")
public final class SakuraginiumBerserkerHandler {

    // =========================================================
    // 属性Modifier UUID
    // =========================================================

    private static final UUID BERSERK_ATTACK_UUID =
            UUID.fromString("aa000001-1111-2222-3333-444444444401");
    private static final UUID BERSERK_SPEED_UUID =
            UUID.fromString("aa000002-1111-2222-3333-444444444402");

    /** 攻撃力 +200% (×3)。 */
    private static final double ATTACK_MULT = 2.0D;

    /** 移動速度 +50% (×1.5)。 */
    private static final double SPEED_MULT = 0.5D;

    /** HP回復倍率。 */
    private static final int HEAL_MULTIPLIER = 3;

    /** 回復チェック間隔（tick）。 */
    private static final int HEAL_INTERVAL = 20;

    /** 解除時鈍足Lv3を10秒 (200tick)。 */
    private static final int EXHAUST_SLOW_DURATION = 200;

    /** バーサー状態フラグを保存するNBTキー。 */
    private static final String TAG_BERSERK = "ForlaismSakuraginiumBerserk";

    /** 最後にバーサーでなかった時点のクールダウン。 */
    private static final String TAG_BERSERK_COOLDOWN = "ForlaismBerserkCooldown";

    private SakuraginiumBerserkerHandler() {
    }

    // =========================================================
    // 装備チェック
    // =========================================================

    private static boolean hasFullArmor(Player player) {
        return isSlot(player, EquipmentSlot.HEAD, Items.SAKURAGINIUM_HELMET.get())
                && isSlot(player, EquipmentSlot.CHEST, Items.SAKURAGINIUM_CHESTPLATE.get())
                && isSlot(player, EquipmentSlot.LEGS, Items.SAKURAGINIUM_LEGGINGS.get())
                && isSlot(player, EquipmentSlot.FEET, Items.SAKURAGINIUM_BOOTS.get());
    }

    private static boolean hasDualFireAxe(Player player) {
        ItemStack main = player.getMainHandItem();
        ItemStack off = player.getOffhandItem();
        return main.getItem() instanceof SenpaiFireAxeItem
                && off.getItem() instanceof SenpaiFireAxeItem;
    }

    private static boolean isSlot(Player player, EquipmentSlot slot,
                                  net.minecraft.world.item.Item item) {
        ItemStack s = player.getItemBySlot(slot);
        return !s.isEmpty() && s.is(item);
    }

    // =========================================================
    // メイン処理
    // =========================================================

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {

        if (event.phase != TickEvent.Phase.END) return;

        Player player = event.player;
        if (player.level().isClientSide) return;

        boolean fullArmor = hasFullArmor(player);
        boolean dualAxe = hasDualFireAxe(player);

        // ---------------------------------------------------------
        // フルセット時の常時衝撃吸収 Lv3
        // ---------------------------------------------------------
        if (fullArmor) {
            player.addEffect(new MobEffectInstance(
                    MobEffects.ABSORPTION,
                    40,      // 短命で毎tick更新
                    2,       // amplifier 2 = Lv3
                    false, false, false
            ));
        }

        // ---------------------------------------------------------
        // バーサー発動判定
        // ---------------------------------------------------------
        boolean shouldBerserk = fullArmor && dualAxe;
        boolean wasBerserk = player.getPersistentData().getBoolean(TAG_BERSERK);

        if (shouldBerserk) {
            if (!wasBerserk) {
                // 発動！
                activate(player);
                player.getPersistentData().putBoolean(TAG_BERSERK, true);
            }
            tickBerserk(player);
        } else {
            if (wasBerserk) {
                // 解除！
                deactivate(player);
                player.getPersistentData().putBoolean(TAG_BERSERK, false);
            }
        }
    }

    // =========================================================
    // 発動
    // =========================================================

    private static void activate(Player player) {

        if (!(player.level() instanceof ServerLevel sl)) return;

        sl.playSound(null,
                player.getX(), player.getY(), player.getZ(),
                SoundEvents.WARDEN_ROAR,
                SoundSource.PLAYERS,
                2.0F, 1.5F);

        sl.playSound(null,
                player.getX(), player.getY(), player.getZ(),
                SoundEvents.BLAZE_SHOOT,
                SoundSource.PLAYERS,
                1.5F, 0.5F);

        sl.sendParticles(
                ParticleTypes.SOUL_FIRE_FLAME,
                player.getX(), player.getY() + 1.0, player.getZ(),
                60, 0.8, 1.5, 0.8, 0.15
        );

        player.displayClientMessage(
                net.minecraft.network.chat.Component.literal(
                        "§4§lバーサーカーモード 発動！"),
                true
        );
    }

    // =========================================================
    // バーサー中の毎tick処理
    // =========================================================

    private static void tickBerserk(Player player) {

        if (!(player.level() instanceof ServerLevel sl)) return;

        // ---------------------------------------------------------
        // 攻撃力 ×3
        // ---------------------------------------------------------
        applyModifier(
                player.getAttribute(Attributes.ATTACK_DAMAGE),
                BERSERK_ATTACK_UUID,
                "berserk_attack",
                ATTACK_MULT,
                AttributeModifier.Operation.MULTIPLY_TOTAL
        );

        // ---------------------------------------------------------
        // 移動速度 ×1.5
        // ---------------------------------------------------------
        applyModifier(
                player.getAttribute(Attributes.MOVEMENT_SPEED),
                BERSERK_SPEED_UUID,
                "berserk_speed",
                SPEED_MULT,
                AttributeModifier.Operation.MULTIPLY_TOTAL
        );

        // ---------------------------------------------------------
        // HP回復 ×3
        // ---------------------------------------------------------
        if (player.tickCount % HEAL_INTERVAL == 0) {
            if (player.getHealth() < player.getMaxHealth()) {
                player.heal(1.0F * HEAL_MULTIPLIER);
            }
        }

        // ---------------------------------------------------------
        // 常に敵へ視線ロック
        // ---------------------------------------------------------
        LivingEntity nearest = findNearestEnemy(player, 32.0D);
        if (nearest != null) {
            double dx = nearest.getX() - player.getX();
            double dy = nearest.getEyeY() - player.getEyeY();
            double dz = nearest.getZ() - player.getZ();
            double horiz = Math.sqrt(dx * dx + dz * dz);

            float yaw = (float) (Math.atan2(dz, dx) * (180.0 / Math.PI)) - 90.0F;
            float pitch = (float) -(Math.atan2(dy, horiz) * (180.0 / Math.PI));

            player.setYRot(yaw);
            player.setYHeadRot(yaw);
            player.setXRot(pitch);
        }

        // ---------------------------------------------------------
        // オーラ演出
        // ---------------------------------------------------------
        if (player.tickCount % 4 == 0) {
            sl.sendParticles(
                    ParticleTypes.CHERRY_LEAVES,
                    player.getX(), player.getY() + 1.0, player.getZ(),
                    3, 0.4, 0.6, 0.4, 0.02
            );
            sl.sendParticles(
                    ParticleTypes.FLAME,
                    player.getX(), player.getY() + 1.0, player.getZ(),
                    2, 0.3, 0.5, 0.3, 0.02
            );
        }
    }

    // =========================================================
    // 解除
    // =========================================================

    private static void deactivate(Player player) {

        if (!(player.level() instanceof ServerLevel sl)) return;

        // 属性Modifier除去
        removeModifier(player.getAttribute(Attributes.ATTACK_DAMAGE), BERSERK_ATTACK_UUID);
        removeModifier(player.getAttribute(Attributes.MOVEMENT_SPEED), BERSERK_SPEED_UUID);

        // 鈍足 Lv3 を 10秒
        player.addEffect(new MobEffectInstance(
                MobEffects.MOVEMENT_SLOWDOWN,
                EXHAUST_SLOW_DURATION,
                2,     // amplifier 2 = Lv3
                false, true, true
        ));

        sl.playSound(null,
                player.getX(), player.getY(), player.getZ(),
                SoundEvents.PLAYER_HURT,
                SoundSource.PLAYERS,
                1.5F, 0.5F);

        sl.sendParticles(
                ParticleTypes.SMOKE,
                player.getX(), player.getY() + 1.0, player.getZ(),
                20, 0.5, 0.8, 0.5, 0.05
        );

        player.displayClientMessage(
                net.minecraft.network.chat.Component.literal(
                        "§7バーサーカーモード 解除… §c脱力感に襲われた"),
                true
        );
    }

    // =========================================================
    // ヘルパー
    // =========================================================

    private static void applyModifier(
            AttributeInstance attr,
            UUID uuid,
            String name,
            double amount,
            AttributeModifier.Operation op
    ) {
        if (attr == null) return;

        AttributeModifier existing = attr.getModifier(uuid);
        if (existing != null && existing.getAmount() == amount) return;

        if (existing != null) attr.removeModifier(uuid);
        attr.addTransientModifier(new AttributeModifier(uuid, name, amount, op));
    }

    private static void removeModifier(AttributeInstance attr, UUID uuid) {
        if (attr == null) return;
        if (attr.getModifier(uuid) != null) attr.removeModifier(uuid);
    }

    private static LivingEntity findNearestEnemy(Player player, double range) {
        AABB area = player.getBoundingBox().inflate(range);
        List<LivingEntity> candidates = player.level().getEntitiesOfClass(
                LivingEntity.class,
                area,
                e -> e.isAlive()
                        && !e.isSpectator()
                        && e != player
                        && e instanceof Mob
        );
        return candidates.stream()
                .min(Comparator.comparingDouble(e -> e.distanceToSqr(player)))
                .orElse(null);
    }
}