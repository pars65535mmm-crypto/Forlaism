package com.tyami.forlaism.event;

import com.tyami.forlaism.registry.Items;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.MobType;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.phys.AABB;

import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import top.theillusivec4.curios.api.CuriosApi;

import java.util.List;
import java.util.UUID;

/**
 * 天体リング（ムーン・サン・トワイライト）の共通処理。
 *
 * - ムーン : 夜間のみ発動
 * - サン   : 昼間のみ発動
 * - トワイライト : 常時発動
 *
 * 属性ModifierはUUIDで管理し、
 * リングを外した時に確実に除去する。
 *
 * ジャンプ力はバニラに属性が無いため、
 * MobEffects.JUMP (跳躍上昇) で代用する。
 */
@Mod.EventBusSubscriber(modid = "forlaism")
public final class CelestialRingHandler {

    // =========================================================
    // 属性Modifier UUID
    // =========================================================

    private static final UUID MOON_SPEED_UUID =
            UUID.fromString("aaaa0001-0000-0000-0000-000000000001");
    private static final UUID MOON_ARMOR_UUID =
            UUID.fromString("aaaa0001-0000-0000-0000-000000000003");
    private static final UUID MOON_HP_UUID =
            UUID.fromString("aaaa0001-0000-0000-0000-000000000004");
    private static final UUID MOON_ATTACK_UUID =
            UUID.fromString("aaaa0001-0000-0000-0000-000000000005");

    private static final UUID SUN_ARMOR_UUID =
            UUID.fromString("aaaa0002-0000-0000-0000-000000000001");
    private static final UUID SUN_HP_UUID =
            UUID.fromString("aaaa0002-0000-0000-0000-000000000002");

    private static final UUID TWILIGHT_SPEED_UUID =
            UUID.fromString("aaaa0003-0000-0000-0000-000000000001");
    private static final UUID TWILIGHT_ARMOR_UUID =
            UUID.fromString("aaaa0003-0000-0000-0000-000000000003");
    private static final UUID TWILIGHT_HP_UUID =
            UUID.fromString("aaaa0003-0000-0000-0000-000000000004");

    // =========================================================
    // 効果量
    // =========================================================

    /** 速度倍率。0.5 = ×1.5 (MULTIPLY_TOTAL) */
    private static final double SPEED_MULT = 0.5D;

    /** 防御力（加算） */
    private static final double MOON_ARMOR_ADD = 20.0D;
    private static final double SUN_ARMOR_ADD = 40.0D;
    private static final double TWILIGHT_ARMOR_ADD = 40.0D;

    /** HP（加算）。バニラHP20 を基準に。 */
    private static final double MOON_HP_ADD = 40.0D;       // 20 → 60  (×3)
    private static final double SUN_HP_ADD = 80.0D;        // 20 → 100 (×5)
    private static final double TWILIGHT_HP_ADD = 80.0D;   // 20 → 100 (×5)

    /** 攻撃力（加算）。 */
    private static final double MOON_ATTACK_ADD = 4.0D;    // 1 + 4 = 5

    /** 敵燃焼範囲。 */
    private static final double BURN_RADIUS = 6.0D;

    /** 敵燃焼秒数。 */
    private static final int BURN_SECONDS = 3;

    private CelestialRingHandler() {
    }

    // =========================================================
    // メイン処理
    // =========================================================

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {

        if (event.phase != TickEvent.Phase.END) return;

        Player player = event.player;
        if (player.level().isClientSide) return;
        if (!(player instanceof ServerPlayer sp)) return;

        // =====================================================
        // リング装備チェック
        // =====================================================
        boolean hasMoon = hasRing(player, Items.MOON_RING.get());
        boolean hasSun = hasRing(player, Items.SUN_RING.get());
        boolean hasTwilight = hasRing(player, Items.TWILIGHT_RING.get());

        // =====================================================
        // 時間帯判定
        // =====================================================
        long time = player.level().getDayTime() % 24000L;
        boolean isNight = time >= 13000L && time < 23000L;

        // =====================================================
        // 発動フラグ
        // =====================================================
        boolean moonActive = hasMoon && isNight;
        boolean sunActive = hasSun && !isNight;
        boolean twilightActive = hasTwilight;

        // =====================================================
        // 属性の適用／除去
        // =====================================================

        // --- ムーン ---
        applyModifier(
                player.getAttribute(Attributes.MOVEMENT_SPEED),
                MOON_SPEED_UUID, "moon_speed",
                moonActive ? SPEED_MULT : null,
                AttributeModifier.Operation.MULTIPLY_TOTAL
        );
        applyModifier(
                player.getAttribute(Attributes.ARMOR),
                MOON_ARMOR_UUID, "moon_armor",
                moonActive ? MOON_ARMOR_ADD : null,
                AttributeModifier.Operation.ADDITION
        );
        applyModifier(
                player.getAttribute(Attributes.MAX_HEALTH),
                MOON_HP_UUID, "moon_hp",
                moonActive ? MOON_HP_ADD : null,
                AttributeModifier.Operation.ADDITION
        );
        applyModifier(
                player.getAttribute(Attributes.ATTACK_DAMAGE),
                MOON_ATTACK_UUID, "moon_attack",
                moonActive ? MOON_ATTACK_ADD : null,
                AttributeModifier.Operation.ADDITION
        );

        // --- サン ---
        applyModifier(
                player.getAttribute(Attributes.ARMOR),
                SUN_ARMOR_UUID, "sun_armor",
                sunActive ? SUN_ARMOR_ADD : null,
                AttributeModifier.Operation.ADDITION
        );
        applyModifier(
                player.getAttribute(Attributes.MAX_HEALTH),
                SUN_HP_UUID, "sun_hp",
                sunActive ? SUN_HP_ADD : null,
                AttributeModifier.Operation.ADDITION
        );

        // --- トワイライト ---
        applyModifier(
                player.getAttribute(Attributes.MOVEMENT_SPEED),
                TWILIGHT_SPEED_UUID, "twilight_speed",
                twilightActive ? SPEED_MULT : null,
                AttributeModifier.Operation.MULTIPLY_TOTAL
        );
        applyModifier(
                player.getAttribute(Attributes.ARMOR),
                TWILIGHT_ARMOR_UUID, "twilight_armor",
                twilightActive ? TWILIGHT_ARMOR_ADD : null,
                AttributeModifier.Operation.ADDITION
        );
        applyModifier(
                player.getAttribute(Attributes.MAX_HEALTH),
                TWILIGHT_HP_UUID, "twilight_hp",
                twilightActive ? TWILIGHT_HP_ADD : null,
                AttributeModifier.Operation.ADDITION
        );

        // =====================================================
        // ポーション効果（暗視）
        // =====================================================
        if (moonActive || sunActive || twilightActive) {
            applyEffect(player, MobEffects.NIGHT_VISION, 400, 0);
        }

        // =====================================================
        // ジャンプ力（Jump Boost で代用）
        // =====================================================
        if (moonActive || twilightActive) {
            // amplifier 1 = ジャンプ力 +50%
            applyEffect(player, MobEffects.JUMP, 400, 1);
        }

        // =====================================================
        // 発光（サン・トワイライト）
        // =====================================================
        if (sunActive || twilightActive) {
            player.setGlowingTag(true);
        } else {
            player.setGlowingTag(false);
        }

        // =====================================================
        // 火炎耐性・水中呼吸（トワイライト）
        // =====================================================
        if (twilightActive) {
            applyEffect(player, MobEffects.FIRE_RESISTANCE, 400, 0);
            applyEffect(player, MobEffects.WATER_BREATHING, 400, 0);
        }

        // =====================================================
        // 落下無効（サン・トワイライト）
        // =====================================================
        if (sunActive || twilightActive) {
            player.fallDistance = 0.0F;
        }

        // =====================================================
        // 回復力（HP自動回復）
        // =====================================================
        if (moonActive || sunActive || twilightActive) {
            float healPerTick = (sunActive || twilightActive) ? 0.05F : 0.03F;

            if (player.getHealth() < player.getMaxHealth()) {
                float newHP = Math.min(
                        player.getMaxHealth(),
                        player.getHealth() + healPerTick
                );
                player.setHealth(newHP);
            }
        }

        // =====================================================
        // 周囲の敵対者を燃やす（サン・トワイライト）
        // =====================================================
        if ((sunActive || twilightActive)
                && player.level() instanceof ServerLevel sl) {

            AABB area = player.getBoundingBox().inflate(BURN_RADIUS);
            List<LivingEntity> targets = sl.getEntitiesOfClass(
                    LivingEntity.class, area,
                    e -> e != player
                            && e.isAlive()
                            && !e.isSpectator()
                            && isHostile(e)
            );

            for (LivingEntity target : targets) {
                target.setSecondsOnFire(BURN_SECONDS);
            }
        }

        // =====================================================
        // パーティクル演出
        // =====================================================
        if (player.level() instanceof ServerLevel sl) {

            if (moonActive) {
                spawnMoonParticles(sl, player);
            }
            if (sunActive) {
                spawnSunParticles(sl, player);
            }
            if (twilightActive) {
                spawnTwilightParticles(sl, player);
            }
        }
    }

    // =========================================================
    // 属性ヘルパー
    // =========================================================

    /**
     * 属性Modifierを適用または除去する。
     *
     * @param attr  属性インスタンス
     * @param uuid  Modifier UUID
     * @param name  Modifier 名
     * @param value 適用値。null なら除去
     * @param op    演算種別
     */
    private static void applyModifier(
            AttributeInstance attr,
            UUID uuid,
            String name,
            Double value,
            AttributeModifier.Operation op
    ) {
        if (attr == null) return;

        AttributeModifier existing = attr.getModifier(uuid);

        // ---- 値が null → 除去 ----
        if (value == null) {
            if (existing != null) {
                attr.removeModifier(uuid);
            }
            return;
        }

        // ---- 既存と同値 → 何もしない ----
        if (existing != null && existing.getAmount() == value) {
            return;
        }

        // ---- 既存を除去して新規追加 ----
        if (existing != null) {
            attr.removeModifier(uuid);
        }

        attr.addTransientModifier(
                new AttributeModifier(uuid, name, value, op)
        );
    }

    // =========================================================
    // 効果ヘルパー
    // =========================================================

    /**
     * ポーション効果を付与する。
     *
     * 残り時間が少ない場合のみ再付与し、
     * 毎tick上書きしないようにする。
     */
    private static void applyEffect(
            Player player,
            MobEffect effect,
            int duration,
            int amplifier
    ) {
        MobEffectInstance current = player.getEffect(effect);

        // 効果なし or 残り時間少ない → 再付与
        if (current == null || current.getDuration() < 100) {
            player.addEffect(new MobEffectInstance(
                    effect,
                    duration,
                    amplifier,
                    false,
                    false,
                    false
            ));
            return;
        }

        // 既存の方が amplifier が低い → 上書き
        if (current.getAmplifier() < amplifier) {
            player.addEffect(new MobEffectInstance(
                    effect,
                    duration,
                    amplifier,
                    false,
                    false,
                    false
            ));
        }
    }

    // =========================================================
    // リング判定
    // =========================================================

    private static boolean hasRing(Player player, Item item) {
        return CuriosApi.getCuriosInventory(player)
                .map(handler -> handler.findFirstCurio(
                        stack -> stack.is(item)
                ).isPresent())
                .orElse(false);
    }

    // =========================================================
    // 敵対判定
    // =========================================================

    /**
     * 敵対Mobかどうか判定する。
     *
     * - MONSTER カテゴリ
     * - または MobType.UNDEAD
     */
    private static boolean isHostile(LivingEntity entity) {
        if (!(entity instanceof Mob mob)) {
            return false;
        }

        // カテゴリが MONSTER
        if (mob.getType().getCategory() == MobCategory.MONSTER) {
            return true;
        }

        // アンデッド
        if (mob.getMobType() == MobType.UNDEAD) {
            return true;
        }

        return false;
    }

    // =========================================================
    // パーティクル
    // =========================================================

    private static void spawnMoonParticles(ServerLevel sl, Player player) {
        if (player.tickCount % 4 == 0) {
            sl.sendParticles(
                    ParticleTypes.END_ROD,
                    player.getX(), player.getY() + 1.0, player.getZ(),
                    2,
                    0.4, 0.4, 0.4,
                    0.01
            );
        }
    }

    private static void spawnSunParticles(ServerLevel sl, Player player) {
        if (player.tickCount % 4 == 0) {
            sl.sendParticles(
                    ParticleTypes.FLAME,
                    player.getX(), player.getY() + 1.0, player.getZ(),
                    2,
                    0.4, 0.4, 0.4,
                    0.01
            );
        }
    }

    private static void spawnTwilightParticles(ServerLevel sl, Player player) {
        if (player.tickCount % 3 == 0) {
            sl.sendParticles(
                    ParticleTypes.END_ROD,
                    player.getX(), player.getY() + 1.0, player.getZ(),
                    1,
                    0.4, 0.4, 0.4,
                    0.01
            );
            sl.sendParticles(
                    ParticleTypes.FLAME,
                    player.getX(), player.getY() + 1.0, player.getZ(),
                    1,
                    0.4, 0.4, 0.4,
                    0.01
            );
        }
    }
}