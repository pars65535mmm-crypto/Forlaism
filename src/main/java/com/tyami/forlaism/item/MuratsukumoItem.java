package com.tyami.forlaism.item;

import com.tyami.watelib.item.IAnimatedTextItem;
import com.tyami.watelib.text.AnimatedText;

import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.Tiers;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;
import java.util.List;

/**
 * 双刀 ムラツクモ。
 *
 * - 両手（メイン + オフハンド）に持っているときだけ二刀流として発動
 * - 通常攻撃: 12ダメージ ×2ヒット + 毎回範囲切り（空中OK・半径5）
 * - 右クリック: 突進 + 12tick斬撃
 * - 突進中: 毎tick 半径3の敵に5ダメージ（無敵時間無視）
 * - 壁にぶつかるとクールダウン0（連続突進可）
 */
public class MuratsukumoItem extends SwordItem implements IAnimatedTextItem {

    /* =========================================================
     * 基本性能
     * ========================================================= */

    public static final float NORMAL_DAMAGE = 12.0F;
    public static final double SWEEP_RADIUS = 5.0D;
    public static final float SWEEP_DAMAGE = 8.0F;

    public static final float DASH_DAMAGE = 5.0F;
    public static final double DASH_RADIUS = 3.0D;

    public static final double DASH_POWER = 2.2D;
    public static final double DASH_UP = 0.5D;

    /** 突進持続tick。 */
    public static final int DASH_DURATION = 12;

    /** クールダウン（15 tick = 0.75秒）。 */
    public static final int COOLDOWN = 15;

    public MuratsukumoItem(Properties properties) {
        super(
                Tiers.NETHERITE,
                11,
                -2.4F,
                properties.stacksTo(1).fireResistant().rarity(Rarity.EPIC)
        );
    }

    /* =========================================================
     * 二刀流判定
     * ========================================================= */

    /**
     * メインハンドとオフハンドの両方にムラツクモを持っているか。
     */
    public static boolean isDualWielding(Player player) {
        return player.getMainHandItem().is(
                        com.tyami.forlaism.registry.Items.MURATSUKUMO.get())
                && player.getOffhandItem().is(
                        com.tyami.forlaism.registry.Items.MURATSUKUMO.get());
    }

    /* =========================================================
     * 表示名
     * ========================================================= */

    @Override
    public AnimatedText createAnimatedName(ItemStack stack) {
        return AnimatedText.of("双刀 ムラツクモ")
                .wave(2.5F, 0.30F, 0.50F)
                .gradient(0xFF220044, 0xFF8844FF, 0xFFFFFFFF, 0xFF44AAFF)
                .gradientSpeed(0.7F)
                .gradientPhase(0.9F);
    }

    /* =========================================================
     * 通常攻撃
     * ========================================================= */

    @Override
    public boolean hurtEnemy(ItemStack stack, LivingEntity target, LivingEntity attacker) {

        // 二刀流じゃないなら普通の剣
        if (!(attacker instanceof Player player)) {
            return super.hurtEnemy(stack, target, attacker);
        }

        if (!isDualWielding(player)) {
            return super.hurtEnemy(stack, target, attacker);
        }

        target.invulnerableTime = 0;
        target.hurtTime = 0;

        if (!player.level().isClientSide) {

            // 二刀流: 2回ヒット
            target.hurt(player.damageSources().playerAttack(player), NORMAL_DAMAGE);
            target.invulnerableTime = 0;
            target.hurt(player.damageSources().playerAttack(player), NORMAL_DAMAGE);

            // 毎回、範囲切り発動
            doSweepAttack(player);

            // メイン・オフ両方の斬撃パーティクル
            spawnDualSlashParticles(player);
        }

        return true;
    }

    private void doSweepAttack(Player player) {
        AABB area = player.getBoundingBox().inflate(SWEEP_RADIUS);

        List<LivingEntity> targets = player.level().getEntitiesOfClass(
                LivingEntity.class,
                area,
                e -> e.isAlive() && e != player && !e.isSpectator()
        );

        for (LivingEntity t : targets) {
            t.invulnerableTime = 0;
            t.hurtTime = 0;
            t.hurt(player.damageSources().playerAttack(player), SWEEP_DAMAGE);
        }
    }

    /* =========================================================
     * 右クリック: 突進
     * ========================================================= */

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {

        ItemStack stack = player.getItemInHand(hand);

        // 二刀流じゃないなら何もしない
        if (!isDualWielding(player)) {
            return InteractionResultHolder.pass(stack);
        }

        if (player.getCooldowns().isOnCooldown(this)) {
            return InteractionResultHolder.fail(stack);
        }

        if (!level.isClientSide) {

            // 視線方向へ吹っ飛ぶ
            Vec3 look = player.getLookAngle();
            player.setDeltaMovement(
                    look.x * DASH_POWER,
                    look.y * DASH_POWER + DASH_UP,
                    look.z * DASH_POWER
            );
            player.hurtMarked = true;
            player.fallDistance = 0.0F;

            // 突進状態付与
            player.getPersistentData().putInt(
                    MuratsukumoDashTracker.TAG_DASH_TICKS,
                    DASH_DURATION
            );

            // クールダウン
            player.getCooldowns().addCooldown(this, COOLDOWN);

            // 演出
            if (level instanceof ServerLevel sl) {
                sl.sendParticles(
                        ParticleTypes.SWEEP_ATTACK,
                        player.getX(), player.getY() + 1.0, player.getZ(),
                        30, 1.5, 1.0, 1.5, 0.2
                );
                sl.playSound(
                        null,
                        player.getX(), player.getY(), player.getZ(),
                        SoundEvents.TRIDENT_RIPTIDE_3,
                        SoundSource.PLAYERS,
                        1.2F, 1.4F
                );
            }
        }

        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }

    /* =========================================================
     * 突進中の毎tick処理
     * ========================================================= */

    public static void onDashTick(Player player) {

        if (player.level().isClientSide) return;

        Vec3 v = player.getDeltaMovement();

        // ---- 壁判定 ----
        double horizontalSpeed = Math.sqrt(v.x * v.x + v.z * v.z);
        boolean hitWall =
                player.horizontalCollision
                        || (horizontalSpeed < 0.05D && player.tickCount > 2);

        if (hitWall) {
            // 突進終了
            player.getPersistentData().remove(MuratsukumoDashTracker.TAG_DASH_TICKS);

            // クールダウン0
            player.getCooldowns().removeCooldown(
                    com.tyami.forlaism.registry.Items.MURATSUKUMO.get()
            );

            if (player.level() instanceof ServerLevel sl) {
                sl.sendParticles(
                        ParticleTypes.CRIT,
                        player.getX(), player.getY() + 1.0, player.getZ(),
                        20, 0.5, 0.5, 0.5, 0.3
                );
                sl.playSound(
                        null,
                        player.getX(), player.getY(), player.getZ(),
                        SoundEvents.SHIELD_BLOCK,
                        SoundSource.PLAYERS,
                        1.0F, 1.5F
                );
            }
            return;
        }

        // ---- 前方へ追加加速 ----
        Vec3 look = player.getLookAngle();
        player.setDeltaMovement(
                v.x + look.x * 0.15D,
                v.y + 0.02D,
                v.z + look.z * 0.15D
        );
        player.hurtMarked = true;
        player.fallDistance = 0.0F;

        // ---- 周囲にダメージ ----
        AABB area = player.getBoundingBox().inflate(DASH_RADIUS);

        List<LivingEntity> targets = player.level().getEntitiesOfClass(
                LivingEntity.class,
                area,
                e -> e.isAlive() && e != player && !e.isSpectator()
        );

        for (LivingEntity t : targets) {
            t.invulnerableTime = 0;
            t.hurtTime = 0;
            t.hurt(player.damageSources().playerAttack(player), DASH_DAMAGE);
        }

        // ---- 振り回し演出 ----
        if (player.level() instanceof ServerLevel sl) {
            double angle = (player.tickCount * 0.8D) % (Math.PI * 2);
            for (int i = 0; i < 3; i++) {
                double a = angle + (i * Math.PI * 2 / 3);
                double rx = Math.cos(a) * 1.8D;
                double rz = Math.sin(a) * 1.8D;
                sl.sendParticles(
                        ParticleTypes.SWEEP_ATTACK,
                        player.getX() + rx,
                        player.getY() + 1.0,
                        player.getZ() + rz,
                        1, 0, 0, 0, 0
                );
            }
        }
    }

    /* =========================================================
     * パーティクル
     * ========================================================= */

    /**
     * メイン・オフ両方から斬撃アークを出す。
     */
    private void spawnDualSlashParticles(Player player) {
        if (!(player.level() instanceof ServerLevel sl)) return;

        spawnSlashArc(sl, player, 0.0);
        spawnSlashArc(sl, player, Math.PI);
    }

    private static void spawnSlashArc(ServerLevel sl, Player player, double offsetAngle) {

        Vec3 look = player.getLookAngle();
        double baseAngle = Math.atan2(look.z, look.x) + offsetAngle;

        for (int i = 0; i < 8; i++) {
            double a = baseAngle + (i / 8.0) * Math.PI;
            double r = 2.0 + player.getRandom().nextDouble() * 1.5;
            sl.sendParticles(
                    ParticleTypes.SWEEP_ATTACK,
                    player.getX() + Math.cos(a) * r,
                    player.getY() + 0.8 + player.getRandom().nextDouble() * 0.8,
                    player.getZ() + Math.sin(a) * r,
                    1, 0, 0, 0, 0
            );
        }
    }

    /* =========================================================
     * Tooltip
     * ========================================================= */

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level,
                                List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.literal("§5双刀 ムラツクモ").withStyle(ChatFormatting.DARK_PURPLE));
        tooltip.add(Component.literal("§7両手に持ったとき、二刀流として目覚める。").withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.literal("§c通常攻撃: §f12ダメージ ×2 + 範囲切り").withStyle(ChatFormatting.RED));
        tooltip.add(Component.literal("§c範囲切り: §f半径" + (int) SWEEP_RADIUS + "に" + (int) SWEEP_DAMAGE + "ダメージ").withStyle(ChatFormatting.RED));
        tooltip.add(Component.literal("§b右クリック: §f突進 + 12tick斬撃").withStyle(ChatFormatting.AQUA));
        tooltip.add(Component.literal("§b突進中: §f毎tick 半径" + (int) DASH_RADIUS + "に" + (int) DASH_DAMAGE + "ダメージ").withStyle(ChatFormatting.AQUA));
        tooltip.add(Component.literal("§e壁にぶつかるとクールダウン0！").withStyle(ChatFormatting.YELLOW));
        tooltip.add(Component.literal("§8二刀は、止まらぬ。").withStyle(ChatFormatting.DARK_GRAY));
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return true;
    }

    /* =========================================================
     * 攻撃速度
     * ========================================================= */

    private static final java.util.UUID ATTACK_SPEED_UUID =
            java.util.UUID.fromString("c1d2e3f4-a5b6-7890-cdef-123456789abc");

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

        builder.put(
                net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_SPEED,
                new net.minecraft.world.entity.ai.attributes.AttributeModifier(
                        ATTACK_SPEED_UUID,
                        "muratsukumo_attack_speed",
                        10.0D,
                        net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation.ADDITION
                )
        );

        return builder.build();
    }
}