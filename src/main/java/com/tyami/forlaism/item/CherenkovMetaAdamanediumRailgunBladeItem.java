package com.tyami.forlaism.item;

import com.tyami.forlaism.ForalisItem;
import com.tyami.forlaism.client.magiceffect.IMagicEffectItem;
import com.tyami.forlaism.client.magiceffect.MagicEffectStyle;
import com.tyami.forlaism.damage.RinneDamageSource;
import com.tyami.forlaism.world.VoidFieldManager;
import com.tyami.watelib.item.IAnimatedTextItem;
import com.tyami.watelib.text.AnimatedText;

import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.Multimap;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.Tiers;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;
import java.util.List;
import java.util.UUID;

/**
 * チェレンコフ・メタアダマンエディウム・レールガン・ブレード。
 *
 * 通称: レールガンブレード
 *
 * - 攻撃力100 / 攻撃速度100
 * - 輪廻ダメージ（無敵時間貫通・最大HP削り・回復阻害・不死身貫通）
 * - 右クリック長押しでチャージ
 * - 10秒以上で放てる
 * - 離すとレーザー発射（射程300ブロック）
 * - 着弾地点に虚空（3秒間 / 45m / 200輪廻ダメージ毎tick）
 */
public class CherenkovMetaAdamanediumRailgunBladeItem extends SwordItem
        implements IAnimatedTextItem, IMagicEffectItem {

    // =========================================================
    // 性能定数
    // =========================================================

    public static final float ATTACK_DAMAGE = 100.0F;
    public static final double ATTACK_SPEED = 100.0D;

    /** チャージ必要tick。10秒 = 200tick。 */
    public static final int CHARGE_REQUIRED_TICKS = 200;

    /** 最大チャージ（保険）。 */
    public static final int MAX_CHARGE_TICKS = 400;

    /** レーザー射程。 */
    public static final double LASER_RANGE = 300.0D;

    /** 虚空の持続tick。3秒 = 60tick。 */
    public static final int VOID_DURATION_TICKS = 60;

    /** 虚空の範囲。 */
    public static final double VOID_RADIUS = 45.0D;

        /** 虚空の毎tickダメージ（強化版）。 */
    public static final float VOID_DAMAGE_PER_TICK = 9_222_222.0F;

    /** 攻撃力Modifier UUID。 */
    private static final UUID ATK_UUID =
            UUID.fromString("c0c0c0c0-1111-2222-3333-444444444401");
    private static final UUID SPD_UUID =
            UUID.fromString("c0c0c0c0-1111-2222-3333-444444444402");

    /** チェレンコフ青〜白のエフェクト。 */
    public static final MagicEffectStyle EFFECT_STYLE = MagicEffectStyle.builder()
            .color(0x80A0E8FF)
            .intensity(2.2f)
            .scale(1.3f)
            .speed(1.8f)
            .ringCount(1)
            .particleCount(8)
            .flares(true)
            .coreGlow(true)
            .build();

    public CherenkovMetaAdamanediumRailgunBladeItem(Properties properties) {
        super(
                Tiers.NETHERITE,
                0,             // 実ダメージは AttributeModifier 側
                -2.4F,
                properties.stacksTo(1).fireResistant().rarity(Rarity.EPIC)
        );
    }

    // =========================================================
    // 属性（攻撃力100 / 攻撃速度100）
    // =========================================================

    @Override
    public Multimap<Attribute, AttributeModifier> getDefaultAttributeModifiers(EquipmentSlot slot) {
        Multimap<Attribute, AttributeModifier> original = super.getDefaultAttributeModifiers(slot);

        if (slot != EquipmentSlot.MAINHAND) {
            return original;
        }

        ImmutableMultimap.Builder<Attribute, AttributeModifier> builder = ImmutableMultimap.builder();
        original.forEach(builder::put);

        builder.put(Attributes.ATTACK_DAMAGE,
                new AttributeModifier(ATK_UUID, "railgun_attack",
                        ATTACK_DAMAGE, AttributeModifier.Operation.ADDITION));

        builder.put(Attributes.ATTACK_SPEED,
                new AttributeModifier(SPD_UUID, "railgun_speed",
                        ATTACK_SPEED, AttributeModifier.Operation.ADDITION));

        return builder.build();
    }

    // =========================================================
    // 通常攻撃は輪廻ダメージ
    // =========================================================

    @Override
    public boolean hurtEnemy(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        if (attacker.level() instanceof ServerLevel sl) {
            target.invulnerableTime = 0;
            target.hurtTime = 0;

            target.hurt(
                    RinneDamageSource.of(sl, attacker),
                    ATTACK_DAMAGE
            );
        }
        return true;
    }

    // =========================================================
    // チャージ
    // =========================================================

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        player.startUsingItem(hand);

        if (!level.isClientSide) {
            level.playSound(null, player.getX(), player.getY(), player.getZ(),
                    SoundEvents.BEACON_ACTIVATE, SoundSource.PLAYERS, 1.0F, 2.0F);
        }

        return InteractionResultHolder.consume(stack);
    }

    @Override
    public int getUseDuration(ItemStack stack) {
        return MAX_CHARGE_TICKS;
    }

    @Override
    public UseAnim getUseAnimation(ItemStack stack) {
        // 弓っぽい構え
        return UseAnim.BOW;
    }

    @Override
    public void onUseTick(Level level, LivingEntity user, ItemStack stack, int remainingUseTicks) {
        if (!(user instanceof Player player)) return;

        int used = getUseDuration(stack) - remainingUseTicks;

        // チャージ中、パーティクル（軽く）
        if (level.isClientSide && used % 4 == 0) {
            var pos = player.getEyePosition()
                    .add(player.getLookAngle().scale(1.5));
            level.addParticle(
                    net.minecraft.core.particles.ParticleTypes.ELECTRIC_SPARK,
                    pos.x + (player.getRandom().nextDouble() - 0.5) * 0.6,
                    pos.y + (player.getRandom().nextDouble() - 0.5) * 0.6,
                    pos.z + (player.getRandom().nextDouble() - 0.5) * 0.6,
                    0, 0, 0
            );
        }
    }

    @Override
    public void releaseUsing(ItemStack stack, Level level, LivingEntity user, int timeLeft) {
        if (!(user instanceof Player player)) return;

        int used = getUseDuration(stack) - timeLeft;

        // 10秒未満なら不発
        if (used < CHARGE_REQUIRED_TICKS) {
            if (!level.isClientSide) {
                player.displayClientMessage(
                        Component.literal("§cチャージ不足… §7(" + (used / 20) + "s / 10s)"),
                        true
                );
            }
            return;
        }

if (level instanceof ServerLevel sl && player instanceof net.minecraft.server.level.ServerPlayer sp) {
    fireLaser(sl, sp, stack);
}
    }

    // =========================================================
    // レーザー発射
    // =========================================================

    private static void fireLaser(ServerLevel level, net.minecraft.server.level.ServerPlayer player, ItemStack stack) {

        Vec3 start = player.getEyePosition();
        Vec3 dir = player.getLookAngle().normalize();

        // レイトレース（300ブロック）
        BlockHitResult blockHit = level.clip(
                new net.minecraft.world.level.ClipContext(
                        start,
                        start.add(dir.scale(LASER_RANGE)),
                        net.minecraft.world.level.ClipContext.Block.COLLIDER,
                        net.minecraft.world.level.ClipContext.Fluid.NONE,
                        player
                )
        );

        Vec3 end = (blockHit.getType() == HitResult.Type.MISS)
                ? start.add(dir.scale(LASER_RANGE))
                : blockHit.getLocation();

        // =========================================================
        // ヒットしたMobに輪廻ダメージ（レーザー直撃分）
        // =========================================================
        var hitBox = new net.minecraft.world.phys.AABB(start, end).inflate(1.5);
        var entities = level.getEntitiesOfClass(
                LivingEntity.class, hitBox,
                e -> e != player && e.isAlive() && !e.isSpectator()
        );

        Vec3 finalEnd = end;
        for (LivingEntity target : entities) {
            // 線分とターゲットの距離チェック（ざっくり）
            Vec3 toTarget = target.position().subtract(start);
            double proj = toTarget.dot(dir);
            if (proj < 0 || proj > LASER_RANGE) continue;

            Vec3 closest = start.add(dir.scale(proj));
            if (closest.distanceTo(target.position()) > 3.0) continue;

            target.invulnerableTime = 0;
            target.hurt(RinneDamageSource.of(level, player), ATTACK_DAMAGE * 5);
        }

        // 着弾地点に虚空生成
        BlockPos voidCenter = BlockPos.containing(finalEnd);
        VoidFieldManager.spawn(
                level,
                voidCenter,
                VOID_DURATION_TICKS,
                VOID_RADIUS,
                VOID_DAMAGE_PER_TICK,
                player.getUUID()
        );

        // 演出
        level.playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.WARDEN_SONIC_BOOM, SoundSource.PLAYERS, 2.0F, 1.5F);

        // クライアントへパケット（レーザー + 虚空）
        var packet = new com.tyami.forlaism.network.RailgunLaserPacket(
                start, finalEnd,
                voidCenter, VOID_DURATION_TICKS, VOID_RADIUS
        );

        com.tyami.forlaism.network.FactotumPacketHandler.CHANNEL.sendTo(
                packet,
                player.connection.connection,
                net.minecraftforge.network.NetworkDirection.PLAY_TO_CLIENT
        );
    }

    // =========================================================
    // 表示名 / エフェクト / Tooltip
    // =========================================================

    @Override
    public AnimatedText createAnimatedName(ItemStack stack) {
        return AnimatedText.of("チェレンコフ・メタアダマンエディウム・レールガン・ブレード")
                .wave(2.5F, 0.35F, 0.50F)
                .gradient(0xFF0040FF, 0xFF40C0FF, 0xFFFFFFFF, 0xFFFFAA00)
                .gradientSpeed(1.6F)
                .gradientPhase(0.8F);
    }

    @Override
    public MagicEffectStyle getMagicEffect(ItemStack stack) {
        return EFFECT_STYLE;
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level,
                                List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.literal("§b§lレールガンブレード。").withStyle(ChatFormatting.AQUA));
        tooltip.add(Component.literal("§c攻撃力: §f100").withStyle(ChatFormatting.RED));
        tooltip.add(Component.literal("§c攻撃速度: §f100").withStyle(ChatFormatting.RED));
        tooltip.add(Component.literal("§d付与: §f輪廻ダメージ").withStyle(ChatFormatting.LIGHT_PURPLE));
        tooltip.add(Component.literal("§7・無敵時間貫通 / 最大HP削り / 回復阻害 / 不死身貫通")
                .withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.literal("§b右クリック長押し §f10秒チャージ → 離して発射")
                .withStyle(ChatFormatting.AQUA));
        tooltip.add(Component.literal("§b射程: §f300ブロック").withStyle(ChatFormatting.AQUA));
        tooltip.add(Component.literal("§5着弾地点に虚空 §f(3秒 / 45m / 200輪廻damage/t)")
                .withStyle(ChatFormatting.DARK_PURPLE));
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return true;
    }
}