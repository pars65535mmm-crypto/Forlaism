package com.tyami.forlaism.item;

import com.tyami.forlaism.ForalisItem;
import com.tyami.forlaism.entity.EnergyKnifeEntity;
import com.tyami.watelib.item.IAnimatedTextItem;
import com.tyami.watelib.text.AnimatedText;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;
import java.util.List;

/**
 * エネルギーダガー。
 *
 * - 右クリック長押しでクールタイム無しの連射
 * - 5発/秒（0.2秒間隔）
 * - 投げナイフは輪廻ダメージ3
 * - 手元にエネルギーが集まって再生成される見た目
 */
public class EnergyDaggerItem extends ForalisItem implements IAnimatedTextItem {

    /** 発射間隔（tick）。0.2秒 = 4tick。 */
    public static final int FIRE_INTERVAL = 4;

    /** 投擲物の速度。 */
    public static final float VELOCITY = 3.0F;

    public EnergyDaggerItem(Properties properties) {
        super(properties
                .stacksTo(1)
                .fireResistant()
                .rarity(Rarity.EPIC)
        );
    }

    // =========================================================
    // 右クリック
    // =========================================================

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        // 長押し開始
        player.startUsingItem(hand);

        // 初回即発射
        if (!level.isClientSide) {
            fire(player, stack);
        }

        return InteractionResultHolder.consume(stack);
    }

    @Override
    public int getUseDuration(ItemStack stack) {
        // 実質無限に構えられる
        return 72000;
    }

    @Override
    public UseAnim getUseAnimation(ItemStack stack) {
        // 弓っぽく構える
        return UseAnim.BOW;
    }

    /**
     * 使用中、毎tick呼ばれる。
     */
    @Override
    public void onUseTick(Level level, LivingEntity user, ItemStack stack, int remainingUseTicks) {
        if (!(user instanceof Player player)) return;
        if (level.isClientSide) return;

        int used = getUseDuration(stack) - remainingUseTicks;

        // 発射間隔チェック
        if (used % FIRE_INTERVAL == 0 && used > 0) {
            fire(player, stack);
        }
    }

    @Override
    public void releaseUsing(ItemStack stack, Level level, LivingEntity user, int timeLeft) {
        // 離したら終了（何もしない）
    }

    // =========================================================
    // 発射処理
    // =========================================================

    private static void fire(Player player, ItemStack stack) {

        Level level = player.level();
        if (!(level instanceof ServerLevel sl)) return;

        EnergyKnifeEntity knife = new EnergyKnifeEntity(sl, player);

        // 手元の位置から発射
        Vec3 eye = player.getEyePosition();
        Vec3 look = player.getLookAngle();

        // 少し前に出す（手元感）
        Vec3 spawn = eye.add(look.scale(0.6))
                .add(0, -0.15, 0);

        knife.setPos(spawn.x, spawn.y, spawn.z);
        knife.shoot(look);

        sl.addFreshEntity(knife);

        // 発射演出
        sl.playSound(
                null,
                player.getX(), player.getY(), player.getZ(),
                SoundEvents.TRIDENT_THROW,
                SoundSource.PLAYERS,
                0.6F,
                1.8F
        );

        // 手元にエネルギーが集まる演出
        sl.sendParticles(
                ParticleTypes.ELECTRIC_SPARK,
                spawn.x, spawn.y, spawn.z,
                8,
                0.15, 0.15, 0.15,
                0.1
        );
        sl.sendParticles(
                ParticleTypes.END_ROD,
                spawn.x, spawn.y, spawn.z,
                4,
                0.1, 0.1, 0.1,
                0.02
        );
        sl.sendParticles(
                ParticleTypes.FLASH,
                spawn.x, spawn.y, spawn.z,
                1,
                0, 0, 0, 0
        );
    }

    // =========================================================
    // 表示名（AnimatedText）
    // =========================================================

    @Override
    public AnimatedText createAnimatedName(ItemStack stack) {
        return AnimatedText.of("エネルギーダガー")
                // ゆらゆらと揺れる
                .wave(2.0F, 0.30F, 0.50F)
                // エネルギーっぽくシアン〜白〜マゼンタで脈打つ
                .gradient(0xFF00FFFF, 0xFFFFFFFF, 0xFFFF00FF, 0xFFFFFFFF, 0xFF00FFFF)
                // 色を高速で流す
                .gradientSpeed(1.8F)
                .gradientPhase(0.6F)
                // 時々グリッチして「エネルギーが不安定」感
                .glitch(1.5F, 0.12F, 0xFF00FFFF)
                // 白いオーラ
                .glow(0x8000FFFF, 2)
                // 外周をほんのり
                .outline(0x80000000, 1);
    }

    // =========================================================
    // Tooltip
    // =========================================================


    @Override
    public boolean isFoil(ItemStack stack) {
        return true;
    }
}