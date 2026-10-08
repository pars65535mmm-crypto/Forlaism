package com.tyami.forlaism.item;

import com.tyami.forlaism.entity.HaniwaNoYariProjectile;
import com.tyami.forlaism.registry.ModEntityTypes;
import com.tyami.watelib.item.IAnimatedTextItem;
import com.tyami.watelib.text.AnimatedText;

import net.minecraft.ChatFormatting;
import com.tyami.forlaism.registry.ModEntityTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.Tiers;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Random;
import java.util.UUID;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraftforge.common.ForgeMod;
import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.Multimap;
import net.minecraft.world.entity.ai.attributes.Attribute;

/**
 * ハニワノヤリ。
 *
 * - 弓みたいにチャージして槍を放つ（アイテムは残る、弾は回収不可）
 * - Shift押しながら放つと発射数3本 + 着弾地点で爆破
 * - 近接リーチ長め・でかい・0.5%即死・発火
 * - アイテム自体が発光（グロウ）する
 */
public class HaniwaNoYariItem extends SwordItem implements IAnimatedTextItem {

    /** 0.5% の即死確率。 */
    public static final float INSTANT_KILL_CHANCE = 0.005F;

    /** チャージ必要tick（弓のフルチャージ相当）。 */
    public static final int CHARGE_TICKS = 1;

    /** 発射速度。 */
    public static final float PROJECTILE_SPEED = 4.0F;

    /** 発射ダメージ（弾）。 */
    public static final float PROJECTILE_DAMAGE = 19.0F;

    /** 近接リーチ（バニラより長い）。 */
    public static final double REACH_BONUS = 9.0D;

    private static final Random RANDOM = new Random();

    public HaniwaNoYariItem(Properties properties) {
        super(
                Tiers.NETHERITE,
                8,      // 攻撃力 9
                -2.4F,  // 攻撃速度
                properties.stacksTo(1).fireResistant().rarity(Rarity.EPIC)
        );
    }

private static final UUID REACH_UUID =
        UUID.fromString("a1a1a1a1-1111-2222-3333-444444444401");

@Override
public Multimap<Attribute, AttributeModifier> getDefaultAttributeModifiers(EquipmentSlot slot) {
    Multimap<Attribute, AttributeModifier> original = super.getDefaultAttributeModifiers(slot);

    if (slot != EquipmentSlot.MAINHAND) {
        return original;
    }

    ImmutableMultimap.Builder<Attribute, AttributeModifier> builder = ImmutableMultimap.builder();
    original.forEach(builder::put);

    // エンティティ攻撃リーチ +3
    builder.put(
            ForgeMod.ENTITY_REACH.get(),
            new AttributeModifier(
                    REACH_UUID,
                    "haniwa_no_yari_reach",
                    REACH_BONUS,
                    AttributeModifier.Operation.ADDITION
            )
    );

    return builder.build();
}

    // =========================================================
    // 表示名（ハニワっぽくゲーミング）
    // =========================================================

    @Override
    public AnimatedText createAnimatedName(ItemStack stack) {
        return AnimatedText.of("ハニワノヤリ")
                .wave(2.5F, 0.30F, 0.50F)
                .gradient(0xFFFF6600, 0xFFFFFF00, 0xFFFF00FF, 0xFF00FFFF, 0xFFFF6600)
                .gradientSpeed(1.5F)
                .gradientPhase(0.7F);
    }

    // =========================================================
    // 右クリック：チャージ開始
    // =========================================================

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        player.startUsingItem(hand);
        return InteractionResultHolder.consume(stack);
    }

    @Override
    public int getUseDuration(ItemStack stack) {
        return 72000;
    }

    @Override
    public UseAnim getUseAnimation(ItemStack stack) {
        return UseAnim.BOW;
    }

    // =========================================================
    // チャージ中演出
    // =========================================================

    @Override
    public void onUseTick(Level level, LivingEntity user, ItemStack stack, int remainingUseTicks) {
        if (!(user instanceof Player player)) return;

        int used = getUseDuration(stack) - remainingUseTicks;

        if (level instanceof ServerLevel sl) {
            // チャージ完了間際にキラキラ
            if (used >= CHARGE_TICKS && used % 2 == 0) {
                Vec3 eye = player.getEyePosition();
                Vec3 look = player.getLookAngle();
                Vec3 pos = eye.add(look.scale(1.2));

                sl.sendParticles(
                        net.minecraft.core.particles.ParticleTypes.END_ROD,
                        pos.x, pos.y, pos.z,
                        2, 0.05, 0.05, 0.05, 0.0
                );
                sl.sendParticles(
                        net.minecraft.core.particles.ParticleTypes.FLAME,
                        pos.x, pos.y, pos.z,
                        1, 0.05, 0.05, 0.05, 0.0
                );
            }
        }
    }

    // =========================================================
    // チャージ放ち
    // =========================================================

    @Override
    public void releaseUsing(ItemStack stack, Level level, LivingEntity user, int timeLeft) {
        if (!(user instanceof Player player)) return;
        if (!(level instanceof ServerLevel sl)) return;

        int used = getUseDuration(stack) - timeLeft;

        // チャージ不足 → 不発
        if (used < 1) return;

        float power = Math.min(1.0F, used / (float) CHARGE_TICKS);
        boolean fullCharge = power >= 1.0F;
        boolean shift = player.isShiftKeyDown();

        Vec3 eye = player.getEyePosition();
        Vec3 look = player.getLookAngle().normalize();

        int shotCount = shift ? 3 : 1;
        float damage = PROJECTILE_DAMAGE * power;

        for (int i = 0; i < shotCount; i++) {
            Vec3 dir = look;

            // 3本時は左右に散らす
            if (shotCount == 3) {
                double spread = (i - 1) * 0.12D;
                dir = look.add(
                        Math.cos(Math.toRadians(player.getYRot())) * spread,
                        0,
                        Math.sin(Math.toRadians(player.getYRot())) * spread
                ).normalize();
            }

            HaniwaNoYariProjectile proj = new HaniwaNoYariProjectile(sl, player);
            proj.setPos(eye.x + look.x * 1.0, eye.y + look.y * 1.0 - 0.1, eye.z + look.z * 1.0);
            proj.setDamage(damage);
            proj.setExplosive(shift);          // Shift なら着弾で爆破
            proj.shoot(dir.x, dir.y, dir.z, PROJECTILE_SPEED, 0.0F);
            sl.addFreshEntity(proj);
        }

        // 発射音
        sl.playSound(null,
                player.getX(), player.getY(), player.getZ(),
                SoundEvents.TRIDENT_THROW,
                SoundSource.PLAYERS,
                1.2F, 0.8F
        );
        if (shift) {
            sl.playSound(null,
                    player.getX(), player.getY(), player.getZ(),
                    SoundEvents.FIREWORK_ROCKET_LAUNCH,
                    SoundSource.PLAYERS,
                    1.0F, 1.5F
            );
        }

        // クールダウン
        player.getCooldowns().addCooldown(this, fullCharge ? 1 : 1);
    }

    // =========================================================
    // 近接：0.5%即死 + 発火
    // =========================================================

    @Override
    public boolean hurtEnemy(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        if (attacker.level().isClientSide) return super.hurtEnemy(stack, target, attacker);

        // 発火
        target.setSecondsOnFire(8);

        // 0.5% 即死
        if (RANDOM.nextFloat() < INSTANT_KILL_CHANCE) {
            target.invulnerableTime = 0;
            target.setHealth(0.0F);
            if (!target.isDeadOrDying()) {
                target.die(
                        target.damageSources().playerAttack(
                                attacker instanceof Player p ? p : null
                        )
                );
            }

            // 演出：即死エフェクト
            if (attacker.level() instanceof ServerLevel sl) {
                sl.sendParticles(
                        net.minecraft.core.particles.ParticleTypes.EXPLOSION_EMITTER,
                        target.getX(), target.getY() + 1.0, target.getZ(),
                        1, 0, 0, 0, 0
                );
                sl.playSound(null,
                        target.getX(), target.getY(), target.getZ(),
                        SoundEvents.WITHER_DEATH,
                        SoundSource.PLAYERS, 1.5F, 0.5F
                );
            }
        }

        return super.hurtEnemy(stack, target, attacker);
    }

    // =========================================================
    // アイテム自体を発光（グロウ）させる
    // =========================================================

    /**
     * エンチャントの輝き（enchantment glint）を常時ONにする。
     * これが「アイテム自体を発光させる」一番簡単な方法。
     */
    @Override
    public boolean isFoil(ItemStack stack) {
        return true;
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
        tooltip.add(Component.literal("§6意味はわからないが[ザエンドオブザウィッチ]と書いてある..").withStyle(ChatFormatting.GOLD));
    }

    // =========================================================
    // リーチ延長（Mixinが拾う）
    // =========================================================

    public static boolean isHaniwaNoYari(ItemStack stack) {
        return stack.getItem() instanceof HaniwaNoYariItem;
    }

    public static double getReachBonus() {
        return REACH_BONUS;
    }
}