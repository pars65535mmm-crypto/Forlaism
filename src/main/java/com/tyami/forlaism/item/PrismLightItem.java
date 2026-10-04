package com.tyami.forlaism.item;

import com.tyami.forlaism.ForalisItem;
import com.tyami.forlaism.entity.PrismLightOrbEntity;
import com.tyami.forlaism.network.FactotumPacketHandler;
import com.tyami.forlaism.network.PrismLockPacket;
import com.tyami.watelib.item.IAnimatedTextItem;
import com.tyami.watelib.text.AnimatedText;

import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
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
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.UUID;

/**
 * プリズムライト。
 *
 * - 右クリック長押しでチャージ
 * - チャージ段階 = 1秒ごとに1ずつ上昇（最大5）
 * - 離すと段階分の光の玉を発射
 * - 弾はロックした対象へ滑らかにホーミング
 * - 構えながら視線のentityにShiftでロックON/OFF
 * - フルチャージ（5秒）弾は着弾時に雷撃+爆裂
 */
public class PrismLightItem extends ForalisItem implements IAnimatedTextItem {

    public static final int MAX_CHARGE = 5;
    public static final int TICKS_PER_STAGE = 20;

    /** プレイヤーごとのロック対象リスト。 */
    private static final java.util.Map<UUID, List<UUID>> LOCKS = new java.util.concurrent.ConcurrentHashMap<>();

    private static final Random RANDOM = new Random();

    public PrismLightItem(Properties properties) {
        super(properties.stacksTo(1).fireResistant().rarity(Rarity.EPIC));
    }

    @Override
    public AnimatedText createAnimatedName(ItemStack stack) {
        return AnimatedText.of("プリズムライト")
                .wave(2.0F, 0.25F, 0.45F)
                .gradient(0xFFFFFFFF, 0xFFFF88FF, 0xFF88FFFF, 0xFFFFFFFF)
                .gradientSpeed(1.0F)
                .gradientPhase(0.6F);
    }

    // =========================================================
    // ロック操作API
    // =========================================================

    public static List<UUID> getLocks(Player player) {
        return LOCKS.computeIfAbsent(player.getUUID(), k -> new ArrayList<>());
    }

    public static void toggleLock(Player player, LivingEntity target) {
        List<UUID> list = getLocks(player);
        UUID id = target.getUUID();
        if (list.contains(id)) {
            list.remove(id);
            player.displayClientMessage(
                    Component.literal("§cロック解除 §7(" + list.size() + "体)"), true);
        } else {
            list.add(id);
            player.displayClientMessage(
                    Component.literal("§bロックオン §7(" + list.size() + "体)"), true);
        }
    }

    public static void clearLocks(Player player) {
        LOCKS.remove(player.getUUID());
    }

    // =========================================================
    // 右クリック
    // =========================================================

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        player.startUsingItem(hand);

        if (!level.isClientSide) {
            level.playSound(null, player.getX(), player.getY(), player.getZ(),
                    SoundEvents.BEACON_ACTIVATE, SoundSource.PLAYERS, 0.5F, 2.0F);
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
    // チャージ中: 段階演出
    // =========================================================

        @Override
    public void onUseTick(Level level, LivingEntity user, ItemStack stack, int remainingUseTicks) {
        if (!(user instanceof Player player)) return;

        int used = getUseDuration(stack) - remainingUseTicks;
        int stage = Math.min(MAX_CHARGE, used / TICKS_PER_STAGE + 1);

        // =========================================================
        // サーバー側: 段階変化の演出
        // =========================================================
        if (level instanceof ServerLevel sl) {

            Vec3 pos = player.position();
            Vec3 eye = player.getEyePosition();
            Vec3 look = player.getLookAngle();

            // ---- 段階が変わった瞬間の派手演出 ----
            if (used > 0 && used % TICKS_PER_STAGE == 0 && (used / TICKS_PER_STAGE) <= MAX_CHARGE) {

                int currentStage = used / TICKS_PER_STAGE;

                // 音（段階ごとにピッチ上昇）
                float pitch = 1.0F + currentStage * 0.25F;
                sl.playSound(null, player.getX(), player.getY(), player.getZ(),
                        SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 1.2F, pitch);
                sl.playSound(null, player.getX(), player.getY(), player.getZ(),
                        SoundEvents.BEACON_POWER_SELECT, SoundSource.PLAYERS, 0.8F, pitch);

                // 光の輪が広がる（拡大するリング）
                int points = 48;
                double radius = 1.0 + currentStage * 0.5;

                for (int i = 0; i < points; i++) {
                    double angle = (i / (double) points) * Math.PI * 2;
                    double x = pos.x + Math.cos(angle) * radius;
                    double z = pos.z + Math.sin(angle) * radius;

                    // 地面スレスレの輪
                    sl.sendParticles(
                            ParticleTypes.END_ROD,
                            x, pos.y + 0.1, z,
                            1, 0, 0.02, 0, 0.0
                    );

                    // 上に伸びる光の柱
                    if (i % 4 == 0) {
                        for (double y = 0; y < 1.5; y += 0.3) {
                            sl.sendParticles(
                                    ParticleTypes.SOUL_FIRE_FLAME,
                                    x, pos.y + y, z,
                                    1, 0, 0, 0, 0.0
                            );
                        }
                    }
                }

                // 中心から光の柱
                for (double y = 0; y < 2.5; y += 0.15) {
                    sl.sendParticles(
                            ParticleTypes.END_ROD,
                            pos.x, pos.y + y, pos.z,
                            1, 0.05, 0, 0.05, 0.0
                    );
                }

                // 段階数だけ周囲に光球を出す
                for (int i = 0; i < currentStage; i++) {
                    double angle = (i / (double) currentStage) * Math.PI * 2
                            + sl.getGameTime() * 0.1;
                    double r = 1.2;
                    double x = pos.x + Math.cos(angle) * r;
                    double z = pos.z + Math.sin(angle) * r;

                    sl.sendParticles(
                            ParticleTypes.FLASH,
                            x, pos.y + 1.0, z,
                            1, 0, 0, 0, 0
                    );
                    sl.sendParticles(
                            ParticleTypes.ELECTRIC_SPARK,
                            x, pos.y + 1.0, z,
                            5, 0.1, 0.1, 0.1, 0.05
                    );
                }

                // フルチャージ到達時
                if (currentStage == MAX_CHARGE) {
                    // 大爆発の予兆
                    sl.sendParticles(
                            ParticleTypes.EXPLOSION,
                            pos.x, pos.y + 1.0, pos.z,
                            1, 0, 0, 0, 0
                    );
                    sl.sendParticles(
                            ParticleTypes.FLASH,
                            pos.x, pos.y + 1.0, pos.z,
                            3, 0.5, 0.5, 0.5, 0
                    );
                    sl.playSound(null, pos.x, pos.y, pos.z,
                            SoundEvents.LIGHTNING_BOLT_THUNDER,
                            SoundSource.PLAYERS, 1.5F, 1.8F);
                    sl.playSound(null, pos.x, pos.y, pos.z,
                            SoundEvents.END_PORTAL_SPAWN,
                            SoundSource.PLAYERS, 1.0F, 2.0F);
                }

                // プレイヤーにメッセージ
                player.displayClientMessage(
                        Component.literal("§d§lチャージ §f" + currentStage + " §7/ §f" + MAX_CHARGE),
                        true
                );
            }

            // ---- 常時: 手元に光が集まる演出 ----
            // 視線の先に収束していく粒子
            double t = (used % 20) / 20.0;
            double convergeR = 1.5 * (1.0 - t);

            for (int i = 0; i < 4; i++) {
                double angle = (used * 0.3) + (i * Math.PI / 2);
                double px = eye.x + look.x * 1.2 + Math.cos(angle) * convergeR;
                double py = eye.y + look.y * 1.2 + Math.sin(angle) * convergeR;
                double pz = eye.z + look.z * 1.2 + Math.cos(angle + Math.PI / 2) * convergeR;

                sl.sendParticles(
                        ParticleTypes.END_ROD,
                        px, py, pz,
                        1, 0, 0, 0, 0.0
                );
            }

            // ---- ロック対象にマーカー ----
            List<UUID> locks = getLocks(player);
            for (UUID id : locks) {
                var e = sl.getEntity(id);
                if (e instanceof LivingEntity living && living.isAlive()) {

                    // 頭上に光の柱
                    double lx = living.getX();
                    double ly = living.getY() + living.getBbHeight() + 0.5;
                    double lz = living.getZ();

                    float time = sl.getGameTime() * 0.2F;
                    for (int i = 0; i < 6; i++) {
                        double angle = time + (i * Math.PI / 3);
                        double r = 0.5;
                        sl.sendParticles(
                                ParticleTypes.ELECTRIC_SPARK,
                                lx + Math.cos(angle) * r,
                                ly + Math.sin(time * 2) * 0.1,
                                lz + Math.sin(angle) * r,
                                1, 0, 0, 0, 0
                        );
                    }

                    // 中心マーカー
                    sl.sendParticles(
                            ParticleTypes.FLASH,
                            lx, ly, lz,
                            1, 0, 0, 0, 0
                    );
                }
            }

            // ---- 構えてる間、周囲が光る ----
            if (used % 5 == 0) {
                for (int i = 0; i < 8; i++) {
                    double angle = sl.getGameTime() * 0.1 + (i * Math.PI / 4);
                    double r = 1.2;
                    sl.sendParticles(
                            ParticleTypes.ENCHANT,
                            pos.x + Math.cos(angle) * r,
                            pos.y + 0.5 + Math.random() * 1.5,
                            pos.z + Math.sin(angle) * r,
                            1, 0.1, 0.1, 0.1, 0.02
                    );
                }
            }
        }

        // =========================================================
        // クライアント側: 追加の光
        // =========================================================
        if (level.isClientSide) {
            Vec3 eye = player.getEyePosition();
            Vec3 look = player.getLookAngle();

            if (used % 2 == 0) {
                level.addParticle(
                        ParticleTypes.END_ROD,
                        eye.x + look.x * 0.6,
                        eye.y + look.y * 0.6 - 0.2,
                        eye.z + look.z * 0.6,
                        0, 0, 0
                );
            }
            if (used % 3 == 0) {
                level.addParticle(
                        ParticleTypes.ENCHANT,
                        player.getX() + (Math.random() - 0.5) * 2,
                        player.getY() + Math.random() * 2,
                        player.getZ() + (Math.random() - 0.5) * 2,
                        0, 0.05, 0
                );
            }
        }
    }

    // =========================================================
    // 離した時: 発射
    // =========================================================

    @Override
    public void releaseUsing(ItemStack stack, Level level, LivingEntity user, int timeLeft) {
        if (!(user instanceof Player player)) return;
        if (level.isClientSide) return;
        if (!(level instanceof ServerLevel sl)) return;

        int used = getUseDuration(stack) - timeLeft;
        int stage = Math.min(MAX_CHARGE, used / TICKS_PER_STAGE + 1);

        // チャージが浅すぎるなら不発
        if (used < 5) return;

        boolean full = (stage >= MAX_CHARGE);

        // 発射
        List<UUID> locks = new ArrayList<>(getLocks(player));

        Vec3 eye = player.getEyePosition();
        Vec3 look = player.getLookAngle().normalize();

        for (int i = 0; i < stage; i++) {

            PrismLightOrbEntity orb = new PrismLightOrbEntity(sl, player);
            orb.setPos(eye.x + look.x * 0.8, eye.y + look.y * 0.8 - 0.2, eye.z + look.z * 0.8);
            orb.setFullCharged(full);

            // ロック対象がいるならランダムに1体を選択
            if (!locks.isEmpty()) {
                UUID chosenId = locks.get(RANDOM.nextInt(locks.size()));
                var chosenEntity = sl.getEntity(chosenId);
                if (chosenEntity instanceof LivingEntity living && living.isAlive()) {
                    orb.setTarget(living);
                }
            }

            // 発射方向: ロックあれば対象の方向、なければ視線
            Vec3 dir;
            if (orb.hasTarget()) {
                // 少し散らす
                dir = look.add(
                        (RANDOM.nextDouble() - 0.5) * 0.4,
                        (RANDOM.nextDouble() - 0.5) * 0.4,
                        (RANDOM.nextDouble() - 0.5) * 0.4
                ).normalize();
            } else {
                dir = look;
            }

            orb.shoot(dir);
            sl.addFreshEntity(orb);
        }

                // =========================================================
        // 発射演出
        // =========================================================

        Vec3 eye2 = player.getEyePosition();
        Vec3 look2 = player.getLookAngle();

        // 音
        sl.playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.TRIDENT_THROW, SoundSource.PLAYERS, 1.2F, 1.8F);
        sl.playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.FIREWORK_ROCKET_LAUNCH, SoundSource.PLAYERS, 1.0F, 2.0F);

        // 発射点のフラッシュ
        sl.sendParticles(
                ParticleTypes.FLASH,
                eye2.x, eye2.y, eye2.z,
                3, 0.2, 0.2, 0.2, 0
        );

        // 銃口から光が放射状に広がる
        for (int i = 0; i < 32; i++) {
            double angle = (i / 32.0) * Math.PI * 2;
            double vx = look2.x + Math.cos(angle) * 0.5;
            double vy = look2.y + Math.sin(angle) * 0.5;
            double vz = look2.z + Math.cos(angle + Math.PI / 2) * 0.5;

            sl.sendParticles(
                    ParticleTypes.END_ROD,
                    eye2.x, eye2.y, eye2.z,
                    1, vx, vy, vz, 0.5
            );
        }

        // 発射点から円形の衝撃波
        for (double d = 0; d < 2.0; d += 0.15) {
            for (int i = 0; i < 24; i++) {
                double angle = (i / 24.0) * Math.PI * 2;
                double px = eye2.x + Math.cos(angle) * d;
                double pz = eye2.z + Math.sin(angle) * d;
                sl.sendParticles(
                        ParticleTypes.SOUL_FIRE_FLAME,
                        px, eye2.y, pz,
                        1, 0, 0, 0, 0
                );
            }
        }

        // フルチャージ発射時は大爆発エフェクト
        if (full) {
            sl.sendParticles(
                    ParticleTypes.EXPLOSION_EMITTER,
                    eye2.x, eye2.y, eye2.z,
                    1, 0, 0, 0, 0
            );
            sl.sendParticles(
                    ParticleTypes.DRAGON_BREATH,
                    eye2.x, eye2.y, eye2.z,
                    60, 1.0, 1.0, 1.0, 0.1
            );
            sl.playSound(null, eye2.x, eye2.y, eye2.z,
                    SoundEvents.LIGHTNING_BOLT_THUNDER, SoundSource.PLAYERS, 2.0F, 1.5F);
        }

        // 発射後はロック全解除
        clearLocks(player);
    }

    // =========================================================
    // Tooltip
    // =========================================================

    @Override
    public void appendHoverText(
            ItemStack stack, @Nullable Level level,
            List<Component> tooltip, TooltipFlag flag
    ) {

        tooltip.add(Component.literal("§8虹の欠片が、万象を穿つ。")
                .withStyle(ChatFormatting.DARK_GRAY));
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return true;
    }
}