package com.tyami.forlaism.item;

import com.tyami.forlaism.entity.MeteorStarEntity;
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

/**
 * インサジネーター。
 *
 * - 攻撃力 11 / 攻撃速度 1.6 / 耐久 2048
 * - 敵を倒すと「魂」が溜まる（最大HPの10%、上限100）
 * - 魂100でテクスチャ変化（insathenelrter_full）
 * - 魂100の状態でShift+右クリックを1秒長押し → 流星発動
 *   - 周囲20mに5発の流星が着弾（5秒後）
 *   - 発動者以外に30ダメージ + ノックバック
 *   - 発動者のHPを10回復
 *   - 魂は0にリセット
 */
public class InsathenelrterItem extends SwordItem {

    /** 魂の最大値。 */
    public static final int SOUL_MAX = 100;

    /** 魂のNBTキー。 */
    public static final String TAG_SOUL = "Soul";

    /** 発動に必要なShift長押しtick。1秒 = 20tick。 */
    public static final int CHARGE_TICKS = 20;

    /** 流星の数。 */
    public static final int METEOR_COUNT = 5;

    /** 流星の着弾範囲。 */
    public static final double METEOR_RADIUS = 20.0D;

    /** 流星の落下時間（tick）。5秒 = 100tick。 */
    public static final int METEOR_FALL_TICKS = 100;

    /** 流星1発のダメージ。 */
    public static final float METEOR_DAMAGE = 30.0F;

    /** 発動者の回復量。 */
    public static final float HEAL_AMOUNT = 10.0F;

    public InsathenelrterItem(Properties properties) {
        super(
                Tiers.NETHERITE,
                10,      // 攻撃力 11 (ベース1 + 10)
                -2.4F,   // 攻撃速度 1.6相当
                properties
                        .stacksTo(1)
                        .durability(2048)
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

    public static boolean isFull(ItemStack stack) {
        return getSoul(stack) >= SOUL_MAX;
    }

    // =========================================================
    // 敵を倒した時に魂を溜める
    // =========================================================


    @Override
    public boolean hurtEnemy(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        if (!attacker.level().isClientSide) {

            // この攻撃で死ぬか判定（攻撃力11換算）
            boolean willDie = target.getHealth() - 11.0F <= 0.0F;

            if (willDie) {
                // 最大HPの10%を魂に加算
                int gain = Math.max(1, (int) (target.getMaxHealth() * 0.10F));
                int current = getSoul(stack);
                int next = Math.min(SOUL_MAX, current + gain);

                setSoul(stack, next);
            }
        }

        return super.hurtEnemy(stack, target, attacker);
    }




    // =========================================================
    // 右クリック：Shift長押しチャージ開始
    // =========================================================

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        // 魂が満タン & Shift押下時のみチャージ開始
        if (!isFull(stack) || !player.isShiftKeyDown()) {
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
        // 弓っぽく構える（チャージ感）
        return UseAnim.BOW;
    }

    // =========================================================
    // チャージ中
    // =========================================================

    @Override
    public void onUseTick(Level level, LivingEntity user, ItemStack stack, int remainingUseTicks) {
        if (!(user instanceof Player player)) return;

        // 使用中に魂が減った or Shift離したら中断
        if (!isFull(stack) || !player.isShiftKeyDown()) {
            player.stopUsingItem();
            return;
        }

        int used = getUseDuration(stack) - remainingUseTicks;

        // 発動者の頭上に魂が集まる演出
        if (level instanceof ServerLevel sl) {
            if (used % 2 == 0) {
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
        }

        // 1秒（20tick）経過で自動発動
        if (used >= CHARGE_TICKS) {
            fireMeteors(player, stack, level);
            player.stopUsingItem();
        }
    }

    @Override
    public void releaseUsing(ItemStack stack, Level level, LivingEntity user, int timeLeft) {
        // 離した時点でまだチャージ完了してなければ不発（useTickで発動済み）
    }

    // =========================================================
    // 流星発動
    // =========================================================

    private void fireMeteors(Player player, ItemStack stack, Level level) {

        if (!(level instanceof ServerLevel sl)) return;

        // 魂リセット
        setSoul(stack, 0);

        // 耐久 -1
        stack.hurtAndBreak(
                1,
                player,
                p -> p.broadcastBreakEvent(player.getUsedItemHand())
        );

        // 発動者周囲20mに流星を5発召喚
        java.util.Random random = new java.util.Random();

        for (int i = 0; i < METEOR_COUNT; i++) {

            double angle = random.nextDouble() * Math.PI * 2;
            double radius = random.nextDouble() * METEOR_RADIUS;

            double tx = player.getX() + Math.cos(angle) * radius;
            double tz = player.getZ() + Math.sin(angle) * radius;
            double ty = player.getY() + 40.0D; // 上空40mから

            MeteorStarEntity star = new MeteorStarEntity(sl, player);
            star.setPos(tx, ty, tz);
            star.setTargetY(player.getY());
            star.setFallTicks(METEOR_FALL_TICKS);
            star.setDamage(METEOR_DAMAGE);
            star.setHealAmount(HEAL_AMOUNT);

            sl.addFreshEntity(star);
        }

        // 発動演出
        sl.playSound(
                null,
                player.getX(), player.getY(), player.getZ(),
                SoundEvents.TRIDENT_RIPTIDE_3,
                SoundSource.PLAYERS,
                2.0F, 1.0F
        );

        sl.playSound(
                null,
                player.getX(), player.getY(), player.getZ(),
                SoundEvents.ENDER_DRAGON_GROWL,
                SoundSource.PLAYERS,
                1.5F, 0.7F
        );

        sl.sendParticles(
                net.minecraft.core.particles.ParticleTypes.FLASH,
                player.getX(), player.getY() + 1.0, player.getZ(),
                3, 0, 0, 0, 0
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
        tooltip.add(Component.literal("§d魂を喰らいし剣。")
                .withStyle(ChatFormatting.LIGHT_PURPLE));
        tooltip.add(Component.literal("§7敵を倒すと魂が宿る。")
                .withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.literal("§7魂が満ちたとき、流星が降る。")
                .withStyle(ChatFormatting.GRAY));
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return isFull(stack);
    }
}