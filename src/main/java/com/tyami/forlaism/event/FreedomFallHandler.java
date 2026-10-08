package com.tyami.forlaism.event;

import com.tyami.forlaism.registry.Items;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.ItemStack;

import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * 「自由」の入手処理。
 *
 * 条件:
 *   - インベントリに「決意」を持っている
 *   - インベントリに「自由」を持っていない
 *   - Y座標が 1000 以上に到達する
 *
 * 到達時:
 *   - 自由を付与（決意は消費しない）
 *   - 盛大な演出
 */
@Mod.EventBusSubscriber(modid = "forlaism")
public final class FreedomFallHandler {

    /** 到達すべきY座標。 */
    public static final double REQUIRED_Y = 1000.0D;

    private FreedomFallHandler() {
    }

    // =========================================================
    // 毎tick: Y座標をチェック
    // =========================================================

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {

        if (event.phase != TickEvent.Phase.END) return;

        if (!(event.player instanceof ServerPlayer player)) return;
        if (!(player.level() instanceof ServerLevel level)) return;

        // Y=1000 未満なら何もしない
        if (player.getY() < REQUIRED_Y) return;

        // =========================================================
        // 決意を持っていて、かつ自由を持っていない時だけ発動
        // =========================================================
        if (!hasDetermination(player)) return;
        if (hasFreedom(player)) return;

        // =========================================================
        // 条件クリア！！ 自由を授ける
        // =========================================================

        ItemStack freedom = new ItemStack(Items.FREEDOM.get());
        if (!player.getInventory().add(freedom)) {
            player.drop(freedom, false);
        }

        // =========================================================
        // 演出
        // =========================================================

        level.playSound(null,
                player.getX(), player.getY(), player.getZ(),
                SoundEvents.ENDER_DRAGON_FLAP,
                SoundSource.PLAYERS,
                2.5F, 1.0F);

        level.playSound(null,
                player.getX(), player.getY(), player.getZ(),
                SoundEvents.TRIDENT_RIPTIDE_3,
                SoundSource.PLAYERS,
                2.0F, 1.5F);

        level.playSound(null,
                player.getX(), player.getY(), player.getZ(),
                SoundEvents.FIREWORK_ROCKET_LARGE_BLAST,
                SoundSource.PLAYERS,
                2.0F, 0.8F);

        level.playSound(null,
                player.getX(), player.getY(), player.getZ(),
                SoundEvents.ENDER_DRAGON_GROWL,
                SoundSource.PLAYERS,
                1.5F, 1.8F);

        // 全方位の衝撃波
        for (int ring = 1; ring <= 5; ring++) {
            double radius = ring * 2.0;
            int points = 20 + ring * 6;
            for (int i = 0; i < points; i++) {
                double angle = (i / (double) points) * Math.PI * 2;
                double px = player.getX() + Math.cos(angle) * radius;
                double pz = player.getZ() + Math.sin(angle) * radius;
                level.sendParticles(
                        ParticleTypes.END_ROD,
                        px, player.getY(), pz,
                        1, 0, 0.05, 0, 0.02
                );
            }
        }

        // 光の柱
        for (double y = 0; y < 8.0; y += 0.3) {
            level.sendParticles(
                    ParticleTypes.FIREWORK,
                    player.getX(), player.getY() - y, player.getZ(),
                    3, 1.0, 0, 1.0, 0.05
            );
        }

        // 上空へ舞い上がる羽根
        level.sendParticles(
                ParticleTypes.END_ROD,
                player.getX(), player.getY() + 1.0, player.getZ(),
                120, 2.5, 3.0, 2.5, 0.20
        );

        // 全体フラッシュ
        level.sendParticles(
                ParticleTypes.FLASH,
                player.getX(), player.getY() + 1.0, player.getZ(),
                3, 0, 0, 0, 0
        );
    }

    // =========================================================
    // ユーティリティ
    // =========================================================

    /**
     * プレイヤーが「決意」を持っているか。
     */
    private static boolean hasDetermination(ServerPlayer player) {

        var inventory = player.getInventory();

        for (int i = 0; i < inventory.getContainerSize(); i++) {
            ItemStack stack = inventory.getItem(i);
            if (stack.is(Items.DETERMINATION.get())) {
                return true;
            }
        }

        return false;
    }

    /**
     * プレイヤーが「自由」を持っているか。
     */
    private static boolean hasFreedom(ServerPlayer player) {

        var inventory = player.getInventory();

        for (int i = 0; i < inventory.getContainerSize(); i++) {
            ItemStack stack = inventory.getItem(i);
            if (stack.is(Items.FREEDOM.get())) {
                return true;
            }
        }

        return false;
    }
}