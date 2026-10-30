package com.tyami.forlaism.item;

import com.tyami.forlaism.entity.MeteorStarEntity;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.Tiers;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * ラプラスサジネーター。
 *
 * - 攻撃力 21 / 攻撃速度 2.1 / 耐久 65536
 * - 敵を切ると、その敵の半径10m以内のランダムな敵に流星1発
 * - 敵を倒すと、その敵の半径10m以内のランダムな敵に流星発動
 * - Shift1秒+右クリック → 輝星
 *     - 周囲20mの敵を空へ+20m飛ばして拘束（移動不可+AI停止）
 *     - 拘束した敵の数だけ流星発動
 *     - 拘束は8秒後に解除
 * - Shift1秒+右クリック+HP5未満 → 贄星
 *     - 周囲50mの敵をその場で拘束 + Y-3mに埋める
 *     - 拘束した敵ごとに10発の流星を2tick間隔で頭上に落とす
 *     - 発動者に速度上昇IIIを10秒
 */
public class RapruslsathenelrterItem extends SwordItem {

    /** 攻撃ヒット時に反応する範囲。 */
    public static final double HIT_RADIUS = 10.0D;

    /** 撃破時に反応する範囲。 */
    public static final double KILL_RADIUS = 10.0D;

    /** 輝星の対象範囲。 */
    public static final double HALO_RADIUS = 20.0D;

    /** 贄星の対象範囲。 */
    public static final double SACRIFICE_RADIUS = 50.0D;

    /** 輝星の飛ばす高さ。 */
    public static final double HALO_LIFT = 20.0D;

    /** 輝星の拘束時間（tick）。8秒 = 160tick。 */
    public static final int HALO_BIND_TICKS = 160;

    /** 贄星の埋める深さ。 */
    public static final double SACRIFICE_DEPTH = 3.0D;

    /** 贄星の頭上落下数（敵1体あたり）。 */
    public static final int SACRIFICE_STAR_PER_TARGET = 10;

    /** 落下間隔（tick）。0.1秒 = 2tick。 */
    public static final int STAR_INTERVAL = 2;

    /** 発動に必要な Shift 長押しtick。 */
    public static final int CHARGE_TICKS = 20;

    /** 流星の落下時間（tick）。5秒。 */
    public static final int METEOR_FALL_TICKS = 100;

    /** 流星1発のダメージ。 */
    public static final float METEOR_DAMAGE = 30.0F;

    /** 発動者の回復量。 */
    public static final float HEAL_AMOUNT = 10.0F;

    /** HP閾値（贄星判定）。 */
    public static final float SACRIFICE_HP_THRESHOLD = 5.0F;

    private static final Random RANDOM = new Random();

    /** 拘束中のEntityを管理するタグ。 */
    public static final String TAG_BOUND = "ForlaismRaprusBound";
    public static final String TAG_BOUND_UNTIL = "ForlaismRaprusBoundUntil";

    public RapruslsathenelrterItem(Properties properties) {
        super(
                Tiers.NETHERITE,
                20,      // 攻撃力 21 (ベース1 + 20)
                -1.9F,   // 攻撃速度 2.1相当
                properties
                        .stacksTo(1)
                        .durability(65536)
                        .fireResistant()
                        .rarity(Rarity.EPIC)
        );
    }

    // =========================================================
    // 敵を切った時：半径10m以内のランダムな敵に流星1発
    // =========================================================

    @Override
    public boolean hurtEnemy(ItemStack stack, LivingEntity target, LivingEntity attacker) {

        if (!attacker.level().isClientSide && attacker.level() instanceof ServerLevel sl
                && attacker instanceof Player player) {

            // 半径10m以内の他の敵を取得
            List<LivingEntity> nearby = getNearbyEnemies(sl, target, player, HIT_RADIUS);

            // ランダムに1体選んで流星発動
            if (!nearby.isEmpty()) {
                LivingEntity chosen = nearby.get(RANDOM.nextInt(nearby.size()));
                spawnStar(sl, player, chosen.getX(), chosen.getZ(), chosen.getY());
            }

            // 撃破判定
            boolean willDie = target.getHealth() - 21.0F <= 0.0F;
            if (willDie) {
                // 撃破時：半径10m以内のランダムな敵に流星
                List<LivingEntity> killNearby = getNearbyEnemies(sl, target, player, KILL_RADIUS);
                if (!killNearby.isEmpty()) {
                    LivingEntity chosen = killNearby.get(RANDOM.nextInt(killNearby.size()));
                    spawnStar(sl, player, chosen.getX(), chosen.getZ(), chosen.getY());
                }
            }
        }

        return super.hurtEnemy(stack, target, attacker);
    }

    private static List<LivingEntity> getNearbyEnemies(
            ServerLevel sl, LivingEntity origin, Player owner, double radius
    ) {
        AABB area = origin.getBoundingBox().inflate(radius);

        List<LivingEntity> result = new ArrayList<>();
        for (LivingEntity e : sl.getEntitiesOfClass(
                LivingEntity.class, area,
                e -> e.isAlive() && !e.isSpectator() && e != origin && e != owner
        )) {
            result.add(e);
        }
        return result;
    }

    // =========================================================
    // 右クリック
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
            fire(player, level);
            player.stopUsingItem();
        }
    }

    // =========================================================
    // 発動分岐
    // =========================================================

    private void fire(Player player, Level level) {
        if (!(level instanceof ServerLevel sl)) return;

        // HP5未満 → 贄星
        if (player.getHealth() < SACRIFICE_HP_THRESHOLD) {
            castSacrificeStar(sl, player);
            return;
        }

        // 通常 → 輝星
        castHaloStar(sl, player);
    }

    // =========================================================
    // 輝星
    // =========================================================

    private void castHaloStar(ServerLevel sl, Player player) {

        // 周囲20mの敵を取得
        AABB area = player.getBoundingBox().inflate(HALO_RADIUS);
        List<LivingEntity> targets = sl.getEntitiesOfClass(
                LivingEntity.class, area,
                e -> e.isAlive() && !e.isSpectator() && e != player
        );

        // 敵0なら何も起きない
        if (targets.isEmpty()) {

            return;
        }

        long now = sl.getGameTime();
        long releaseAt = now + HALO_BIND_TICKS;

        for (LivingEntity target : targets) {
            // 空へ飛ばす
            double newY = target.getY() + HALO_LIFT;
            target.teleportTo(target.getX(), newY, target.getZ());

            // 拘束フラグ
            CompoundTag data = target.getPersistentData();
            data.putBoolean(TAG_BOUND, true);
            data.putLong(TAG_BOUND_UNTIL, releaseAt);

            com.tyami.forlaism.event.RaprusBindingHandler.registerBound(target);

            // AI停止
            if (target instanceof Mob mob) {
                mob.setNoAi(true);
                mob.setDeltaMovement(0, 0, 0);
                mob.setTarget(null);
            }

            // 拘束の見た目
            sl.sendParticles(
                    net.minecraft.core.particles.ParticleTypes.END_ROD,
                    target.getX(), target.getY() + 1.0, target.getZ(),
                    30, 0.5, 0.5, 0.5, 0.1
            );

            // 流星を1発（敵1体につき）
            spawnStar(sl, player, target.getX(), target.getZ(), target.getY());
        }

        // 演出
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
    // 贄星
    // =========================================================

    private void castSacrificeStar(ServerLevel sl, Player player) {

        // 周囲50mの敵を取得
        AABB area = player.getBoundingBox().inflate(SACRIFICE_RADIUS);
        List<LivingEntity> targets = sl.getEntitiesOfClass(
                LivingEntity.class, area,
                e -> e.isAlive() && !e.isSpectator() && e != player
        );

        // 敵0でも発動（自分の速度上昇はもらえる）
        long now = sl.getGameTime();
        long releaseAt = now + HALO_BIND_TICKS;

        for (LivingEntity target : targets) {

            // Y-3mに埋める
            double newY = target.getY() - SACRIFICE_DEPTH;
            target.teleportTo(target.getX(), newY, target.getZ());

            // 拘束フラグ
            CompoundTag data = target.getPersistentData();
            data.putBoolean(TAG_BOUND, true);
            data.putLong(TAG_BOUND_UNTIL, releaseAt);

            com.tyami.forlaism.event.RaprusBindingHandler.registerBound(target);

            if (target instanceof Mob mob) {
                mob.setNoAi(true);
                mob.setDeltaMovement(0, 0, 0);
                mob.setTarget(null);
            }

            // 土で蓋をする（Y-3の位置＝元の地表位置の上に土ブロック）
            BlockPos cover = BlockPos.containing(
                    target.getX(),
                    target.getY() + 1,
                    target.getZ()
            );

            // 空気なら土で埋める
            for (int dy = 0; dy <= 2; dy++) {
                BlockPos p = cover.above(dy);
                if (sl.getBlockState(p).isAir()) {
                    sl.setBlock(p, Blocks.DIRT.defaultBlockState(), 3);
                }
            }

            // 拘束演出
            sl.sendParticles(
                    net.minecraft.core.particles.ParticleTypes.SCULK_SOUL,
                    target.getX(), target.getY() + 1.0, target.getZ(),
                    20, 0.5, 0.5, 0.5, 0.05
            );

            // 敵ごとに10発を2tick間隔で頭上に落とす
            final double tx = target.getX();
            final double tz = target.getZ();
            final double ty = target.getY();

            for (int i = 0; i < SACRIFICE_STAR_PER_TARGET; i++) {
                int delay = i * STAR_INTERVAL;

                com.tyami.forlaism.item.MasterpieceClockScheduler.schedule(
                        sl, delay,
                        () -> spawnStar(sl, player, tx, tz, ty)
                );
            }
        }

        // 発動者に速度上昇IIIを10秒
        player.addEffect(new MobEffectInstance(
                MobEffects.MOVEMENT_SPEED,
                200,     // 10秒
                2,       // アンプリファイア2 = Lv3
                false, true, true
        ));

        // 演出
        sl.playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.WITHER_SPAWN, SoundSource.PLAYERS, 2.5F, 0.5F);
        sl.playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.ENDER_DRAGON_DEATH, SoundSource.PLAYERS, 2.0F, 0.5F);
        sl.playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.LIGHTNING_BOLT_THUNDER, SoundSource.PLAYERS, 2.5F, 0.6F);

        sl.sendParticles(
                net.minecraft.core.particles.ParticleTypes.EXPLOSION_EMITTER,
                player.getX(), player.getY() + 1.0, player.getZ(),
                5, 0, 0, 0, 0
        );

        sl.sendParticles(
                net.minecraft.core.particles.ParticleTypes.SCULK_SOUL,
                player.getX(), player.getY() + 1.0, player.getZ(),
                120, 3.0, 1.5, 3.0, 0.2
        );


    }

    // =========================================================
    // 流星エンティティ生成
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
        tooltip.add(Component.literal("§5ラプラスの名を冠した剣。")
                .withStyle(ChatFormatting.DARK_PURPLE));
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return true;
    }
}