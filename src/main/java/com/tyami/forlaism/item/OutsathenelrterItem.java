package com.tyami.forlaism.item;

import com.tyami.forlaism.entity.MeteorStarEntity;
import com.tyami.forlaism.registry.Items;
import com.tyami.forlaism.registry.ModEntityTypes;

import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.Tiers;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Random;

/**
 * アウトサジネーター。
 *
 * - 攻撃力 18 / 攻撃速度 1.8 / 耐久 20480
 * - 敵を倒すと魂 +1（上限25）
 * - 魂数に応じて3段階の技が発動（Shift + 右クリック1秒）
 *     魂 5  → 流星  (周囲20m に 5発)
 *     魂 10 → 落星  (自分の周囲5m に 10発)
 *     魂 25 → 堕星  (見てる対象に 30発 / 2tick間隔)
 * - 魂9で発動すると「魂5」の技が発動して魂は4になる
 */
public class OutsathenelrterItem extends SwordItem {

    /** 魂の上限。 */
    public static final int SOUL_MAX = 25;

    /** 魂のNBTキー。 */
    public static final String TAG_SOUL = "Soul";

    /** 発動に必要な Shift 長押しtick。1秒 = 20tick。 */
    public static final int CHARGE_TICKS = 20;

    /** 流星（魂5）の数。 */
    public static final int METEOR_COUNT = 5;

    /** 流星（魂5）の範囲。 */
    public static final double METEOR_RADIUS = 20.0D;

    /** 落星（魂10）の数。 */
    public static final int FALLING_STAR_COUNT = 10;

    /** 落星（魂10）のバラけ半径。 */
    public static final double FALLING_STAR_RADIUS = 5.0D;

    /** 堕星（魂25）の数。 */
    public static final int DOOM_STAR_COUNT = 30;

    /** 堕星の落下間隔（tick）。0.1秒 = 2tick。 */
    public static final int DOOM_STAR_INTERVAL = 2;

    /** 堕星の射程。 */
    public static final double DOOM_STAR_RANGE = 100.0D;

    /** 流星の落下時間（tick）。5秒。 */
    public static final int METEOR_FALL_TICKS = 100;

    /** 流星1発のダメージ。 */
    public static final float METEOR_DAMAGE = 30.0F;

    /** 発動者の回復量。 */
    public static final float HEAL_AMOUNT = 10.0F;

    /** 乱数。 */
    private static final Random RANDOM = new Random();

    public OutsathenelrterItem(Properties properties) {
        super(
                Tiers.NETHERITE,
                17,      // 攻撃力 18 (ベース1 + 17)
                -2.2F,   // 攻撃速度 1.8相当
                properties
                        .stacksTo(1)
                        .durability(20480)
                        .rarity(Rarity.EPIC)
        );
    }

    // =========================================================
    // 魂操作
    // =========================================================

    public static int getSoul(ItemStack stack) {
        CompoundTag tag = stack.getTag();
        return tag == null ? 0 : tag.getInt(TAG_SOUL);
    }

    public static void setSoul(ItemStack stack, int value) {
        CompoundTag tag = stack.getOrCreateTag();
        tag.putInt(TAG_SOUL, Math.max(0, Math.min(SOUL_MAX, value)));
    }

    // =========================================================
    // 敵を倒すと魂 +1
    // =========================================================

    @Override
    public boolean hurtEnemy(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        if (!attacker.level().isClientSide) {
            // この攻撃で死ぬか（攻撃力18換算）
            boolean willDie = target.getHealth() - 18.0F <= 0.0F;

            if (willDie) {
                int current = getSoul(stack);
                setSoul(stack, Math.min(SOUL_MAX, current + 1));
            }
        }

        return super.hurtEnemy(stack, target, attacker);
    }

    // =========================================================
    // 右クリック：Shift長押しでチャージ
    // =========================================================

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        // 魂5未満 or Shift未押下 → 何もしない
        if (getSoul(stack) < 5 || !player.isShiftKeyDown()) {
            return InteractionResultHolder.pass(stack);
        }

        player.startUsingItem(hand);

        if (!level.isClientSide) {
            level.playSound(
                    null,
                    player.getX(), player.getY(), player.getZ(),
                    SoundEvents.BEACON_ACTIVATE,
                    SoundSource.PLAYERS,
                    0.7F, 1.5F
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

    // =========================================================
    // チャージ中
    // =========================================================

    @Override
    public void onUseTick(Level level, LivingEntity user, ItemStack stack, int remainingUseTicks) {
        if (!(user instanceof Player player)) return;

        // 魂が減った or Shift離したら中断
        if (getSoul(stack) < 5 || !player.isShiftKeyDown()) {
            player.stopUsingItem();
            return;
        }

        int used = getUseDuration(stack) - remainingUseTicks;

        // 溜め演出
        if (level instanceof ServerLevel sl && used % 2 == 0) {
            Vec3 eye = player.getEyePosition();
            double angle = (used * 0.5) % (Math.PI * 2);
            double r = 1.2;

            for (int i = 0; i < 3; i++) {
                double a = angle + (i * Math.PI * 2 / 3);
                sl.sendParticles(
                        net.minecraft.core.particles.ParticleTypes.END_ROD,
                        eye.x + Math.cos(a) * r,
                        eye.y + Math.sin(used * 0.15) * 0.5,
                        eye.z + Math.sin(a) * r,
                        1, 0, 0, 0, 0
                );
            }
        }

        // 1秒経過で発動
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

        int soul = getSoul(stack);

        // ---- 魂25 → 堕星 ----
        if (soul >= 25) {
            castDoomStar(sl, player);
            setSoul(stack, soul - 25);
            return;
        }

        // ---- 魂10 → 落星 ----
        if (soul >= 10) {
            castFallingStar(sl, player);
            setSoul(stack, soul - 10);
            return;
        }

        // ---- 魂5以上 → 流星 ----
        if (soul >= 5) {
            castMeteor(sl, player);
            setSoul(stack, soul - 5);
        }
    }

    // =========================================================
    // 流星（魂5）
    // =========================================================

    private void castMeteor(ServerLevel sl, Player player) {
        for (int i = 0; i < METEOR_COUNT; i++) {
            double angle = RANDOM.nextDouble() * Math.PI * 2;
            double radius = RANDOM.nextDouble() * METEOR_RADIUS;

            double tx = player.getX() + Math.cos(angle) * radius;
            double tz = player.getZ() + Math.sin(angle) * radius;

            spawnStar(sl, player, tx, tz, player.getY());
        }

        sl.playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.TRIDENT_RIPTIDE_3, SoundSource.PLAYERS, 2.0F, 1.0F);
        sl.playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.ENDER_DRAGON_GROWL, SoundSource.PLAYERS, 1.5F, 0.7F);


    }

    // =========================================================
    // 落星（魂10）
    // =========================================================

    private void castFallingStar(ServerLevel sl, Player player) {
        for (int i = 0; i < FALLING_STAR_COUNT; i++) {
            double angle = RANDOM.nextDouble() * Math.PI * 2;
            double radius = RANDOM.nextDouble() * FALLING_STAR_RADIUS;

            double tx = player.getX() + Math.cos(angle) * radius;
            double tz = player.getZ() + Math.sin(angle) * radius;

            spawnStar(sl, player, tx, tz, player.getY());
        }

        sl.playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.WITHER_SPAWN, SoundSource.PLAYERS, 1.5F, 0.6F);
        sl.playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.END_PORTAL_SPAWN, SoundSource.PLAYERS, 1.2F, 0.8F);

        sl.sendParticles(
                net.minecraft.core.particles.ParticleTypes.FLASH,
                player.getX(), player.getY() + 1.0, player.getZ(),
                5, 0, 0, 0, 0
        );


    }

    // =========================================================
    // 堕星（魂25）
    // =========================================================

    private void castDoomStar(ServerLevel sl, Player player) {

        // ---- 視線先の座標をロック ----
        Vec3 eye = player.getEyePosition();
        Vec3 look = player.getLookAngle();
        Vec3 end = eye.add(look.scale(DOOM_STAR_RANGE));

        // ① Entity優先
        AABB box = player.getBoundingBox()
                .expandTowards(look.scale(DOOM_STAR_RANGE))
                .inflate(2.0);

        EntityHitResult entityHit = ProjectileUtil.getEntityHitResult(
                sl, player, eye, end, box,
                e -> e instanceof LivingEntity && e.isAlive() && e != player
        );

        Vec3 lockPos;

        if (entityHit != null) {
            lockPos = entityHit.getEntity().position();
        } else {
            // ② ブロック
            BlockHitResult blockHit = sl.clip(new ClipContext(
                    eye, end,
                    ClipContext.Block.COLLIDER,
                    ClipContext.Fluid.NONE,
                    player
            ));

            if (blockHit.getType() == HitResult.Type.MISS) {
                // 何も当たらなかったら視線100m先
                lockPos = end;
            } else {
                lockPos = blockHit.getLocation();
            }
        }

        final double lockX = lockPos.x;
        final double lockZ = lockPos.z;
        final double lockY = lockPos.y;

        // ---- 30発を2tick間隔でスケジュール ----
        // MasterpieceClockScheduler を流用
        for (int i = 0; i < DOOM_STAR_COUNT; i++) {
            int delay = i * DOOM_STAR_INTERVAL;

            com.tyami.forlaism.item.MasterpieceClockScheduler.schedule(
                    sl, delay,
                    () -> spawnStar(sl, player, lockX, lockZ, lockY)
            );
        }

        sl.playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.WITHER_SPAWN, SoundSource.PLAYERS, 2.0F, 0.5F);
        sl.playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.ENDER_DRAGON_DEATH, SoundSource.PLAYERS, 1.5F, 0.6F);
        sl.playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.LIGHTNING_BOLT_THUNDER, SoundSource.PLAYERS, 2.0F, 0.8F);

        sl.sendParticles(
                net.minecraft.core.particles.ParticleTypes.EXPLOSION_EMITTER,
                player.getX(), player.getY() + 1.0, player.getZ(),
                3, 0, 0, 0, 0
        );


    }

    // =========================================================
    // 流星エンティティ生成ヘルパー
    // =========================================================

    private static void spawnStar(ServerLevel sl, Player owner,
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
    // Tooltip
    // =========================================================

    @Override
    public void appendHoverText(
            ItemStack stack,
            @Nullable Level level,
            List<Component> tooltip,
            TooltipFlag flag
    ) {
        tooltip.add(Component.literal("§d魂を弔いし剣。")
                .withStyle(ChatFormatting.LIGHT_PURPLE));
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return getSoul(stack) >= SOUL_MAX;
    }
}