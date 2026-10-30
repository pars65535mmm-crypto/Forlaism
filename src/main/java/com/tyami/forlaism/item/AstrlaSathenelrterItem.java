package com.tyami.forlaism.item;

import com.tyami.forlaism.entity.MeteorStarEntity;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import com.tyami.forlaism.entity.FlyingStarEntity;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.Tiers;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Random;
import java.util.UUID;

/**
 * アストラサジネーター。
 *
 * - 攻撃力 30 / 攻撃速度 3 / 耐久 1,024,255
 * - 敵を切る → 流星  (その敵の周囲に1発)
 * - 敵を倒す → 落星  (その敵の周囲に10発)
 * - Shift 1秒 + 右クリック          → 堕星  (周囲50mの敵に30発ずつ)
 * - Shift 1秒 + 空中 + 右クリック    → 輝星  (周囲20mの敵を拘束+流星)
 * - 左手 + Shift 1秒 + 右クリック    → 門星  (背後に7つ、向き方向へ射出)
 * - 左手 + Shift 1秒 + 右クリック + 空中 → 星々 (周囲50mの全Entityに3種 + 背後/上空に21発)
 */
public class AstrlaSathenelrterItem extends SwordItem {

    /** 攻撃力。 */
    public static final float ATTACK_DAMAGE = 30.0F;

    /** 攻撃速度。 */
    public static final double ATTACK_SPEED = 3.0D;

    /** 耐久値。 */
    public static final int DURABILITY = 1_024_255;

    /** 発動に必要な Shift 長押しtick。1秒 = 20。 */
    public static final int CHARGE_TICKS = 20;

    /** 流星/落星/堕星の落下時間。 */
    public static final int METEOR_FALL_TICKS = 100;

    /** 流星1発のダメージ。 */
    public static final float METEOR_DAMAGE = 30.0F;

    /** 発動者の回復量。 */
    public static final float HEAL_AMOUNT = 10.0F;

    /** 星の数（流星/落星/堕星/輝星/門星/星々）。 */
    public static final int HIT_METEOR_COUNT = 1;
    public static final int KILL_STAR_COUNT = 10;
    public static final int DOOM_STAR_COUNT = 30;
    public static final int HALO_STAR_COUNT = 10;
    public static final int GATE_STAR_COUNT = 7;
    public static final int STARS_STAR_COUNT = 21;

    /** 堕星の範囲。 */
    public static final double DOOM_RADIUS = 50.0D;

    /** 輝星の範囲。 */
    public static final double HALO_RADIUS = 20.0D;

    /** 星々の範囲。 */
    public static final double STARS_RADIUS = 50.0D;

    /** 星の射出速度（門星/星々）。 */
    public static final double STAR_LAUNCH_SPEED = 2.0D;

    /** 星の射出寿命（tick）。 */
    public static final int STAR_LAUNCH_LIFE = 40;

    private static final Random RANDOM = new Random();

    public AstrlaSathenelrterItem(Properties properties) {
        super(
                Tiers.NETHERITE,
                29,      // 攻撃力 30 (ベース1 + 29)
                -2.4F,
                properties
                        .stacksTo(1)
                        .durability(DURABILITY)
                        .fireResistant()
                        .rarity(Rarity.EPIC)
        );
    }

    // =========================================================
    // 攻撃時: 流星
    // =========================================================

    @Override
    public boolean hurtEnemy(ItemStack stack, LivingEntity target, LivingEntity attacker) {

        if (!attacker.level().isClientSide
                && attacker.level() instanceof ServerLevel sl
                && attacker instanceof Player player) {

            // 流星: その敵の位置に1発
            for (int i = 0; i < HIT_METEOR_COUNT; i++) {
                spawnStarAt(sl, player, target.getX(), target.getZ(), target.getY());
            }

            // 撃破判定（攻撃力30換算）
            boolean willDie = target.getHealth() - ATTACK_DAMAGE <= 0.0F;
            if (willDie) {
                // 落星: その敵の位置に10発
                for (int i = 0; i < KILL_STAR_COUNT; i++) {
                    double angle = RANDOM.nextDouble() * Math.PI * 2;
                    double r = RANDOM.nextDouble() * 4.0;
                    double tx = target.getX() + Math.cos(angle) * r;
                    double tz = target.getZ() + Math.sin(angle) * r;
                    spawnStarAt(sl, player, tx, tz, target.getY());
                }
            }
        }

        return super.hurtEnemy(stack, target, attacker);
    }

    // =========================================================
    // Shift 長押し → 右クリック
    // =========================================================

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        if (!player.isShiftKeyDown()) {
            return InteractionResultHolder.pass(stack);
        }

        player.startUsingItem(hand);

        if (!level.isClientSide) {
            level.playSound(
                    null,
                    player.getX(), player.getY(), player.getZ(),
                    SoundEvents.BEACON_ACTIVATE,
                    SoundSource.PLAYERS,
                    0.8F, 1.5F
            );
        }

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

    @Override
    public void onUseTick(Level level, LivingEntity user, ItemStack stack, int remainingUseTicks) {
        if (!(user instanceof Player player)) return;

        if (!player.isShiftKeyDown()) {
            player.stopUsingItem();
            return;
        }

        int used = getUseDuration(stack) - remainingUseTicks;

        // 溜め演出
        if (level instanceof ServerLevel sl && used % 2 == 0) {
            Vec3 eye = player.getEyePosition();
            double angle = (used * 0.5) % (Math.PI * 2);
            double r = 1.5;

            for (int i = 0; i < 4; i++) {
                double a = angle + (i * Math.PI * 2 / 4);
                sl.sendParticles(
                        net.minecraft.core.particles.ParticleTypes.END_ROD,
                        eye.x + Math.cos(a) * r,
                        eye.y + Math.sin(used * 0.15) * 0.5,
                        eye.z + Math.sin(a) * r,
                        1, 0, 0, 0, 0
                );
            }
        }

        if (used >= CHARGE_TICKS) {
            fire(player, stack, level);
            player.stopUsingItem();
        }
    }

    // =========================================================
    // 発動分岐
    // =========================================================

    private void fire(Player player, ItemStack stack, Level level) {
        if (!(level instanceof ServerLevel sl)) return;

        boolean offhand = player.getOffhandItem().is(this);
        boolean airborne = !player.onGround();

        if (offhand && airborne) {
            // 星々
            castStars(sl, player);
        } else if (offhand) {
            // 門星
            castGate(sl, player);
        } else if (airborne) {
            // 輝星
            castHalo(sl, player);
        } else {
            // 堕星
            castDoom(sl, player);
        }
    }

    // =========================================================
    // 堕星: 周囲50mの敵に30発ずつ
    // =========================================================

    private void castDoom(ServerLevel sl, Player player) {

        AABB area = player.getBoundingBox().inflate(DOOM_RADIUS);
        List<LivingEntity> targets = sl.getEntitiesOfClass(
                LivingEntity.class, area,
                e -> e.isAlive() && !e.isSpectator() && e != player
        );

        for (LivingEntity target : targets) {
            for (int i = 0; i < DOOM_STAR_COUNT; i++) {
                double angle = RANDOM.nextDouble() * Math.PI * 2;
                double r = RANDOM.nextDouble() * 6.0;
                double tx = target.getX() + Math.cos(angle) * r;
                double tz = target.getZ() + Math.sin(angle) * r;
                spawnStarAt(sl, player, tx, tz, target.getY());
            }
        }

        sl.playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.WITHER_SPAWN, SoundSource.PLAYERS, 2.0F, 0.5F);
        sl.playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.ENDER_DRAGON_GROWL, SoundSource.PLAYERS, 1.5F, 0.6F);

        sl.sendParticles(
                net.minecraft.core.particles.ParticleTypes.EXPLOSION_EMITTER,
                player.getX(), player.getY() + 1.0, player.getZ(),
                3, 0, 0, 0, 0
        );
    }

    // =========================================================
    // 輝星: 周囲20mの敵を拘束 + 流星
    // =========================================================

    private void castHalo(ServerLevel sl, Player player) {

        AABB area = player.getBoundingBox().inflate(HALO_RADIUS);
        List<LivingEntity> targets = sl.getEntitiesOfClass(
                LivingEntity.class, area,
                e -> e.isAlive() && !e.isSpectator() && e != player
        );

        for (LivingEntity target : targets) {
            // 上空に20m飛ばす
            double newY = target.getY() + 20.0D;
            target.teleportTo(target.getX(), newY, target.getZ());

            // 拘束フラグ
            long now = sl.getGameTime();
            long releaseAt = now + 160;  // 8秒
            target.getPersistentData().putBoolean(
                    RapruslsathenelrterItem.TAG_BOUND, true);
            target.getPersistentData().putLong(
                    RapruslsathenelrterItem.TAG_BOUND_UNTIL, releaseAt);

            com.tyami.forlaism.event.RaprusBindingHandler.registerBound(target);

            if (target instanceof net.minecraft.world.entity.Mob mob) {
                mob.setNoAi(true);
                mob.setDeltaMovement(0, 0, 0);
                mob.setTarget(null);
            }

            // 拘束演出
            sl.sendParticles(
                    net.minecraft.core.particles.ParticleTypes.END_ROD,
                    target.getX(), target.getY() + 1.0, target.getZ(),
                    30, 0.5, 0.5, 0.5, 0.1
            );

            // 流星を10発
            for (int i = 0; i < HALO_STAR_COUNT; i++) {
                spawnStarAt(sl, player, target.getX(), target.getZ(), target.getY());
            }
        }

        sl.playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.END_PORTAL_SPAWN, SoundSource.PLAYERS, 2.0F, 0.7F);
        sl.playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.BEACON_ACTIVATE, SoundSource.PLAYERS, 1.5F, 0.8F);

        sl.sendParticles(
                net.minecraft.core.particles.ParticleTypes.FLASH,
                player.getX(), player.getY() + 1.0, player.getZ(),
                5, 0, 0, 0, 0
        );
    }

    // =========================================================
    // 門星: 背後に7つ、向き方向へ射出
    // =========================================================

    private void castGate(ServerLevel sl, Player player) {

        Vec3 look = player.getLookAngle().normalize();
        Vec3 origin = player.position();

        // 背後に7つ並べる（扇状）
        for (int i = 0; i < GATE_STAR_COUNT; i++) {
            double spread = (i - (GATE_STAR_COUNT - 1) / 2.0) * 0.5;
            Vec3 right = new Vec3(-look.z, 0, look.x).normalize();
            Vec3 spawn = origin.subtract(look.scale(3.0)).add(right.scale(spread));
            spawn = spawn.add(0, 1.0, 0);

            spawnLaunchedStar(sl, player, spawn, look);
        }

        sl.playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.TRIDENT_RIPTIDE_3, SoundSource.PLAYERS, 2.0F, 1.2F);
        sl.playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.FIREWORK_ROCKET_LAUNCH, SoundSource.PLAYERS, 1.5F, 1.0F);

        sl.sendParticles(
                net.minecraft.core.particles.ParticleTypes.FLASH,
                player.getX(), player.getY() + 1.0, player.getZ(),
                3, 0, 0, 0, 0
        );
    }

    // =========================================================
    // 星々: 周囲50mの全Entityに3種 + 背後/上空に21発
    // =========================================================

    private void castStars(ServerLevel sl, Player player) {

        AABB area = player.getBoundingBox().inflate(STARS_RADIUS);
        List<LivingEntity> targets = sl.getEntitiesOfClass(
                LivingEntity.class, area,
                e -> e.isAlive() && !e.isSpectator() && e != player
        );

        // 各Entityに流星/堕星/落星を1発ずつ
        for (LivingEntity target : targets) {
            // 流星
            spawnStarAt(sl, player, target.getX(), target.getZ(), target.getY());

            // 堕星（少しずらして）
            for (int i = 0; i < 1; i++) {
                double angle = RANDOM.nextDouble() * Math.PI * 2;
                double r = RANDOM.nextDouble() * 3.0;
                spawnStarAt(sl, player,
                        target.getX() + Math.cos(angle) * r,
                        target.getZ() + Math.sin(angle) * r,
                        target.getY());
            }

            // 落星
            for (int i = 0; i < 1; i++) {
                double angle = RANDOM.nextDouble() * Math.PI * 2;
                double r = RANDOM.nextDouble() * 5.0;
                spawnStarAt(sl, player,
                        target.getX() + Math.cos(angle) * r,
                        target.getZ() + Math.sin(angle) * r,
                        target.getY());
            }
        }

        // 21発: 背後7 + 上空7 + 前7
        Vec3 look = player.getLookAngle().normalize();
        Vec3 origin = player.position();
        Vec3 right = new Vec3(-look.z, 0, look.x).normalize();

        for (int i = 0; i < 7; i++) {
            // 背後
            double spread = (i - 3) * 0.6;
            Vec3 spawn = origin.subtract(look.scale(3.0)).add(right.scale(spread));
            spawn = spawn.add(0, 1.0, 0);
            spawnLaunchedStar(sl, player, spawn, look);

            // 上空
            Vec3 up = origin.add(0, 3.0 + i * 0.5, 0).add(right.scale(spread));
            spawnLaunchedStar(sl, player, up, look);

            // 前
            Vec3 front = origin.add(look.scale(3.0)).add(right.scale(spread));
            front = front.add(0, 1.0, 0);
            spawnLaunchedStar(sl, player, front, look);
        }

        sl.playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.WITHER_SPAWN, SoundSource.PLAYERS, 2.5F, 0.4F);
        sl.playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.ENDER_DRAGON_DEATH, SoundSource.PLAYERS, 2.0F, 0.5F);
        sl.playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.LIGHTNING_BOLT_THUNDER, SoundSource.PLAYERS, 2.0F, 0.8F);

        sl.sendParticles(
                net.minecraft.core.particles.ParticleTypes.EXPLOSION_EMITTER,
                player.getX(), player.getY() + 1.0, player.getZ(),
                5, 0, 0, 0, 0
        );
        sl.sendParticles(
                net.minecraft.core.particles.ParticleTypes.SCULK_SOUL,
                player.getX(), player.getY() + 1.0, player.getZ(),
                150, 3.0, 1.5, 3.0, 0.2
        );
    }

    // =========================================================
    // 流星エンティティ生成
    // =========================================================

    private static void spawnStarAt(ServerLevel sl, Player owner,
                                    double tx, double tz, double targetY) {

        MeteorStarEntity star = new MeteorStarEntity(sl, owner);
        star.setPos(tx, targetY + 40.0D, tz);
        star.setTargetY(targetY);
        star.setFallTicks(METEOR_FALL_TICKS);
        star.setDamage(METEOR_DAMAGE);
        star.setHealAmount(HEAL_AMOUNT);

        sl.addFreshEntity(star);
    }

    // =========================================================
    // 射出型の星（門星/星々用）
    // =========================================================

    private static void spawnLaunchedStar(ServerLevel sl, Player owner,
                                          Vec3 spawnPos, Vec3 direction) {

        FlyingStarEntity star = new FlyingStarEntity(sl, owner);
        star.setPos(spawnPos.x, spawnPos.y, spawnPos.z);
        star.shoot(direction, STAR_LAUNCH_SPEED);
        star.setLife(STAR_LAUNCH_LIFE);

        sl.addFreshEntity(star);

        sl.sendParticles(
                net.minecraft.core.particles.ParticleTypes.FLASH,
                spawnPos.x, spawnPos.y, spawnPos.z,
                1, 0, 0, 0, 0
        );
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
        tooltip.add(Component.literal("§d§l星々を連ねる剣")
                .withStyle(ChatFormatting.LIGHT_PURPLE));

    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return true;
    }
}