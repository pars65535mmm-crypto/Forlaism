package com.tyami.forlaism.item;

import com.tyami.forlaism.world.FakedreamDimension;
import com.tyami.watelib.client.AnimatedItemRenderer;
import com.tyami.watelib.effect.GradientDirection;
import com.tyami.watelib.effect.Waveform;
import com.tyami.watelib.item.IAnimatedItemVisual;
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
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import top.theillusivec4.curios.api.SlotContext;
import top.theillusivec4.curios.api.type.capability.ICurioItem;

import javax.annotation.Nullable;
import java.util.List;

/**
 * マスターピースクロック。
 *
 * Curios の clock スロットに装備可能。
 * Cキーでバインド画面を開く（キーコンフィグ可能）。
 * Shift + 右クリックで FakeDream へ転送。
 */
public class MasterpieceClockItem extends Item
        implements IAnimatedTextItem, IAnimatedItemVisual, ICurioItem {

    /** FakeDream 転送先。 */
    public static final double FAKEDREAM_X = 0.5D;
    public static final double FAKEDREAM_Y = 123.0D;
    public static final double FAKEDREAM_Z = 0.5D;

    /** 演出時間（tick）。2秒 = 40tick。 */
    public static final int EFFECT_DURATION = 40;

    /** 射出までの遅延（演出開始から）。 */
    public static final int LAUNCH_DELAY = 40;

    public MasterpieceClockItem(Properties properties) {
        super(properties.stacksTo(1).fireResistant().rarity(Rarity.EPIC));
    }

    // =========================================================
    // アイテム名
    // =========================================================

    @Override
    public AnimatedText createAnimatedName(ItemStack stack) {
        return AnimatedText.of("Masterpiece Clock")
                .wave(Waveform.SINE, 1.5F, 0.45F, 0.60F)
                .sway(1.5F, 0.35F, 0.45F)
                .gradient(
                        0xFF888888,
                        0xFFE0E0E0,
                        0xFFFFFFFF,
                        0xFFFFD700,
                        0xFFFFFFFF,
                        0xFFE0E0E0,
                        0xFF888888
                )
                .gradientSpeed(0.35F)
                .gradientPhase(0.50F)
                .typewriter(3, 5)
                .rainbowCycle(0.45F, 0.30F, 0.35F, 0.95F)
                .sparkle(0xFFFFFFFF, 2.5F, 0.75F)
                .glow(0xA0FFFFFF, 1);
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
        tooltip.add(Component.empty());
        tooltip.add(Component.empty());
        tooltip.add(Component.empty());
    }

    @Override
    public AnimatedText createAnimatedTooltip(ItemStack stack, int lineIndex) {
        if (lineIndex == 1) {
            return AnimatedText.of("常に決意を持ち、自由を手に入れ、可能性を信じ続け、")
                    .wave(Waveform.SINE, 1.5F, 0.35F, 0.40F)
                    .sway(0.8F, 0.25F, 0.30F)
                    .gradient(GradientDirection.LEFT_TO_RIGHT,
                            0xFFE0E0E0, 0xFFFFFFFF, 0xFFFFD700)
                    .gradientSpeed(0.30F)
                    .gradientPhase(0.45F)
                    .rainbowCycle(0.30F, 0.35F, 0.40F, 0.90F)
                    .sparkle(0xFFFFEECC, 1.5F, 0.65F)
                    .outline(0x80000000, 1)
                    .glow(0x80FFFFFF, 1);
        }
        if (lineIndex == 2) {
            return AnimatedText.of("夢幻の力を使い、無限を超え、想像し、挑戦をやめず、")
                    .wave(Waveform.SINE, 1.5F, 0.35F, 0.40F)
                    .sway(0.8F, 0.25F, 0.30F)
                    .gradient(GradientDirection.LEFT_TO_RIGHT,
                            0xFFFFFFFF, 0xFFFFD700, 0xFFFFFFFF)
                    .gradientSpeed(0.30F)
                    .gradientPhase(0.55F)
                    .rainbowCycle(0.30F, 0.30F, 0.45F, 0.95F)
                    .sparkle(0xFFFFEECC, 1.5F, 0.65F)
                    .outline(0x80000000, 1)
                    .glow(0x80FFFFFF, 1);
        }
        if (lineIndex == 3) {
            return AnimatedText.of("微睡を持った者のみが手に入れることのできる銀時計")
                    .wave(Waveform.SMOOTHSTEP, 2.0F, 0.45F, 0.45F)
                    .sway(1.2F, 0.30F, 0.35F)
                    .gradient(GradientDirection.CENTER_OUT,
                            0xFFFFD700, 0xFFFFFFFF, 0xFFE0E0E0)
                    .gradientSpeed(0.25F)
                    .gradientPhase(0.60F)
                    .rainbowCycle(0.50F, 0.40F, 0.35F, 1.0F)
                    .sparkle(0xFFFFD700, 1.5F, 0.80F)
                    .outline(0x80000000, 1)
                    .glow(0x90FFD700, 1);
        }
        return null;
    }

    @Override
    public AnimatedItemRenderer.Visual createVisual(ItemStack stack) {
        return new AnimatedItemRenderer.Visual(
                2.0F,
                0.003F,
                0.008F,
                0x80FFFFFF,
                1,
                0.9F
        );
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return true;
    }

    // =========================================================
    // Curios: ICurioItem
    // =========================================================

    @Override
    public boolean canEquip(SlotContext slotContext, ItemStack stack) {
        return "clock".equals(slotContext.identifier());
    }

    @Override
    public boolean canUnequip(SlotContext slotContext, ItemStack stack) {
        return true;
    }

    @Override
    public void onEquip(SlotContext slotContext, ItemStack prevStack, ItemStack stack) {
        if (slotContext.entity() instanceof ServerPlayer sp) {
            sp.displayClientMessage(
                    Component.literal("§f§l時計が、時を刻み始めた…")
                            .withStyle(ChatFormatting.WHITE),
                    true
            );
        }
    }

    @Override
    public void onUnequip(SlotContext slotContext, ItemStack newStack, ItemStack stack) {
        if (slotContext.entity() instanceof ServerPlayer sp) {
            sp.displayClientMessage(
                    Component.literal("§7時計が、止まった…")
                            .withStyle(ChatFormatting.GRAY),
                    true
            );
        }
    }

    // =========================================================
    // 右クリック: Shift で FakeDream 転送
    // =========================================================

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        // Shift 押してない → 何もしない
        if (!player.isShiftKeyDown()) {
            return InteractionResultHolder.pass(stack);
        }

        if (level.isClientSide) {
            return InteractionResultHolder.sidedSuccess(stack, true);
        }

        if (!(player instanceof ServerPlayer serverPlayer)) {
            return InteractionResultHolder.fail(stack);
        }

        if (!(level instanceof ServerLevel serverLevel)) {
            return InteractionResultHolder.fail(stack);
        }

        ServerLevel fakedream = serverPlayer.server.getLevel(FakedreamDimension.FAKEDREAM_LEVEL);
        if (fakedream == null) {
            serverPlayer.displayClientMessage(
                    Component.literal("§c夢幻の世界が存在しない…")
                            .withStyle(ChatFormatting.DARK_RED),
                    true
            );
            return InteractionResultHolder.fail(stack);
        }

        // 演出開始
        playTransferEffect(serverLevel, serverPlayer, fakedream);

        // クールダウン
        serverPlayer.getCooldowns().addCooldown(this, EFFECT_DURATION + 40);

        return InteractionResultHolder.sidedSuccess(stack, false);
    }

    // =========================================================
    // 演出: 桜吹雪 2秒 → 光の柱 → 射出 → 転送
    // =========================================================

    private void playTransferEffect(ServerLevel level, ServerPlayer player, ServerLevel targetDim) {

        final double x = player.getX();
        final double y = player.getY();
        final double z = player.getZ();

        // =========================================================
        // 桜吹雪の音（即時）
        // =========================================================
        level.playSound(null, x, y, z,
                SoundEvents.CHERRY_LEAVES_PLACE,
                SoundSource.PLAYERS, 1.5F, 0.8F);
        level.playSound(null, x, y, z,
                SoundEvents.AMETHYST_BLOCK_CHIME,
                SoundSource.PLAYERS, 1.2F, 1.5F);
        level.playSound(null, x, y, z,
                SoundEvents.BEACON_ACTIVATE,
                SoundSource.PLAYERS, 2.0F, 0.5F);

        // =========================================================
        // 桜吹雪: 1tickずつスケジュール（確実に出す！）
        // =========================================================
        for (int tick = 0; tick < EFFECT_DURATION; tick++) {
            final int t = tick;
            scheduleDelayed(level, tick, () -> {
                if (!player.isAlive()) return;

                // 2tickごとにまとめて出す
                if (t % 2 != 0) return;

                // =================================================
                // CHERRY_LEAVES: 周囲に舞う
                // =================================================
                for (int i = 0; i < 8; i++) {
                    double ox = (level.random.nextDouble() - 0.5) * 5.0;
                    double oy = level.random.nextDouble() * 4.0;
                    double oz = (level.random.nextDouble() - 0.5) * 5.0;

                    level.sendParticles(
                            ParticleTypes.CHERRY_LEAVES,
                            player.getX() + ox,
                            player.getY() + oy,
                            player.getZ() + oz,
                            1,      // ← count=1（ここ重要）
                            0.0, 0.0, 0.0,  // 広がりゼロ
                            0.02    // 速度
                    );
                }

                // =================================================
                // キラキラも少し（演出強化）
                // =================================================
                for (int i = 0; i < 3; i++) {
                    double ox = (level.random.nextDouble() - 0.5) * 3.0;
                    double oy = level.random.nextDouble() * 3.0;
                    double oz = (level.random.nextDouble() - 0.5) * 3.0;

                    level.sendParticles(
                            ParticleTypes.END_ROD,
                            player.getX() + ox,
                            player.getY() + oy,
                            player.getZ() + oz,
                            1,
                            0.0, 0.0, 0.0,
                            0.01
                    );
                }
            });
        }

        // =========================================================
        // 光の柱 + 射出 + 転送（2秒後）
        // =========================================================
        scheduleDelayed(level, LAUNCH_DELAY, () -> {
            if (!player.isAlive()) return;
            launchAndTransfer(level, player, targetDim);
        });
    }

    /**
     * 光の柱 + 射出 + 転送。
     */
    private void launchAndTransfer(ServerLevel level, ServerPlayer player, ServerLevel targetDim) {

        double x = player.getX();
        double y = player.getY();
        double z = player.getZ();

        // =========================================================
        // 1. 光の柱（プレイヤーを覆って有り余る）
        // =========================================================
        int radius = 6;
        int height = 40;

        // 中心の光の柱
        for (double dy = 0; dy < height; dy += 0.5) {
            level.sendParticles(
                    ParticleTypes.END_ROD,
                    x, y + dy, z,
                    3, 0.3, 0.1, 0.3, 0.0
            );
            level.sendParticles(
                    ParticleTypes.FLASH,
                    x, y + dy, z,
                    1, 0, 0, 0, 0
            );
            level.sendParticles(
                    ParticleTypes.FIREWORK,
                    x, y + dy, z,
                    2, 0.2, 0.2, 0.2, 0.02
            );
            level.sendParticles(
                    ParticleTypes.SOUL_FIRE_FLAME,
                    x, y + dy, z,
                    1, 0.3, 0.3, 0.3, 0.01
            );
        }

        // 全方位の光の柱（太く）
        for (double r = 0; r < radius; r += 0.8) {
            int points = (int) (8 + r * 4);
            for (int i = 0; i < points; i++) {
                double angle = (i / (double) points) * Math.PI * 2;
                double px = x + Math.cos(angle) * r;
                double pz = z + Math.sin(angle) * r;

                for (double dy = 0; dy < height * 0.6; dy += 1.0) {
                    level.sendParticles(
                            ParticleTypes.END_ROD,
                            px, y + dy, pz,
                            1, 0.05, 0.1, 0.05, 0.0
                    );
                }
            }
        }

        // ソウルフレイムの渦
        for (double dy = 0; dy < height * 0.5; dy += 0.5) {
            double angle = dy * 0.5;
            double r = 2.0 - dy * 0.05;
            if (r < 0.2) r = 0.2;
            level.sendParticles(
                    ParticleTypes.SOUL_FIRE_FLAME,
                    x + Math.cos(angle) * r,
                    y + dy,
                    z + Math.sin(angle) * r,
                    1, 0, 0, 0, 0.02
            );
        }

        // 大量のフラッシュ
        for (int i = 0; i < 30; i++) {
            double ox = (level.random.nextDouble() - 0.5) * radius * 1.5;
            double oy = level.random.nextDouble() * height * 0.8;
            double oz = (level.random.nextDouble() - 0.5) * radius * 1.5;
            level.sendParticles(
                    ParticleTypes.FLASH,
                    x + ox, y + oy, z + oz,
                    1, 0, 0, 0, 0
            );
        }

        // =========================================================
        // 2. 音：ばああああん
        // =========================================================
        level.playSound(null, x, y, z,
                SoundEvents.GENERIC_EXPLODE,
                SoundSource.PLAYERS, 3.0F, 0.5F);
        level.playSound(null, x, y, z,
                SoundEvents.END_PORTAL_SPAWN,
                SoundSource.PLAYERS, 2.5F, 0.7F);
        level.playSound(null, x, y, z,
                SoundEvents.WITHER_SPAWN,
                SoundSource.PLAYERS, 2.0F, 0.5F);
        level.playSound(null, x, y, z,
                SoundEvents.LIGHTNING_BOLT_THUNDER,
                SoundSource.PLAYERS, 2.5F, 0.8F);

        // =========================================================
        // 3. 上空へ高速射出
        // =========================================================
        player.setDeltaMovement(0, 3.5D, 0);
        player.hurtMarked = true;
        player.fallDistance = 0.0F;

        // =========================================================
        // 4. 転送（20tick後）
        // =========================================================
        scheduleDelayed(level, 20, () -> {
            if (!player.isAlive()) return;

            player.teleportTo(
                    targetDim,
                    FAKEDREAM_X, FAKEDREAM_Y, FAKEDREAM_Z,
                    player.getYRot(),
                    player.getXRot()
            );

            // 転送先の着地演出
            targetDim.sendParticles(
                    ParticleTypes.FLASH,
                    FAKEDREAM_X, FAKEDREAM_Y, FAKEDREAM_Z,
                    5, 0.5, 0.5, 0.5, 0
            );
            targetDim.sendParticles(
                    ParticleTypes.END_ROD,
                    FAKEDREAM_X, FAKEDREAM_Y + 2, FAKEDREAM_Z,
                    50, 1.5, 3.0, 1.5, 0.1
            );
            targetDim.sendParticles(
                    ParticleTypes.CHERRY_LEAVES,
                    FAKEDREAM_X, FAKEDREAM_Y + 1, FAKEDREAM_Z,
                    40, 2.0, 2.0, 2.0, 0.05
            );
            targetDim.playSound(null,
                    FAKEDREAM_X, FAKEDREAM_Y, FAKEDREAM_Z,
                    SoundEvents.END_PORTAL_SPAWN,
                    SoundSource.PLAYERS, 2.0F, 1.5F);

            player.displayClientMessage(
                    Component.literal("§d§l夢幻の世界へ…")
                            .withStyle(ChatFormatting.LIGHT_PURPLE),
                    true
            );
        });
    }

    // =========================================================
    // 簡易遅延実行
    // =========================================================

    private static void scheduleDelayed(ServerLevel level, int delayTicks, Runnable task) {
        MasterpieceClockScheduler.schedule(level, delayTicks, task);
    }
}