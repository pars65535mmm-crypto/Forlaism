package com.tyami.forlaism.event;

import com.tyami.forlaism.registry.Items;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemStack;


import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * ノクスルクスの入手儀式。
 *
 * 条件:
 *   1. 満月の夜（月相 0）
 *   2. 時間帯 18000 ～ 22000（真夜中前後、月が真上付近）
 *   3. プレイヤーが真上（pitch -80 ～ -90）を向いている
 *   4. メインハンドにネザライトの剣
 *   5. 空気 or 何もない場所で右クリック
 *
 * 成功時:
 *   - 視界暗転（クライアントへパケット）
 *   - 演出（音・パーティクル）
 *   - ネザライトの剣がノクスルクスに変化
 */
@Mod.EventBusSubscriber(modid = "forlaism")
public final class NoxLuxCraftHandler {

    /** 月が真上と判定する時間帯。 */
    private static final long MIDNIGHT_START = 18000L;
    private static final long MIDNIGHT_END = 22000L;

    /** 「真上を向いている」判定。 */
    private static final float LOOK_UP_MIN = -90.0F;
    private static final float LOOK_UP_MAX = -80.0F;

    private NoxLuxCraftHandler() {
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onRightClickItem(PlayerInteractEvent.RightClickItem event) {

        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }

        if (!(player.level() instanceof ServerLevel level)) {
            return;
        }

        ItemStack held = event.getItemStack();

        // =========================================================
        // メインハンドにネザライトの剣を持っているか
        // =========================================================
        if (!held.is(net.minecraft.world.item.Items.NETHERITE_SWORD)) {
            return;
        }

        // =========================================================
        // 満月チェック（月相 0 = 満月）
        // =========================================================
        if (level.getMoonPhase() != 0) {
            return;
        }

        // =========================================================
        // 時間帯チェック（真夜中）
        // =========================================================
        long dayTime = level.getDayTime() % 24000L;
        if (dayTime < MIDNIGHT_START || dayTime >= MIDNIGHT_END) {
            return;
        }

        // =========================================================
        // 真上を向いているか
        // =========================================================
        float pitch = player.getXRot();
        if (pitch < LOOK_UP_MIN || pitch > LOOK_UP_MAX) {
            return;
        }

        // =========================================================
        // 空が見えるか（屋外チェック）
        // =========================================================
        if (!level.canSeeSky(player.blockPosition())) {
            player.displayClientMessage(
                    Component.literal("§7月の光が届かない…"),
                    true
            );
            return;
        }

        // =========================================================
        // 儀式発動！
        // =========================================================
        performRitual(level, player, held);

        event.setCancellationResult(InteractionResult.SUCCESS);
        event.setCanceled(true);
    }

    // =========================================================
    // 儀式本体
    // =========================================================

    private static void performRitual(ServerLevel level, ServerPlayer player, ItemStack held) {

        // =========================================================
        // 演出: 音
        // =========================================================
        level.playSound(
                null,
                player.getX(), player.getY(), player.getZ(),
                SoundEvents.ENDER_DRAGON_GROWL,
                SoundSource.PLAYERS,
                2.0F, 0.5F
        );

        level.playSound(
                null,
                player.getX(), player.getY(), player.getZ(),
                SoundEvents.RESPAWN_ANCHOR_CHARGE,
                SoundSource.PLAYERS,
                2.0F, 0.7F
        );

        // =========================================================
        // 演出: パーティクル（月光が降り注ぐ）
        // =========================================================
        double cx = player.getX();
        double cy = player.getY();
        double cz = player.getZ();

        // 頭上から降り注ぐ光の柱
        for (double y = cy + 1.0; y < cy + 8.0; y += 0.3) {
            level.sendParticles(
                    ParticleTypes.END_ROD,
                    cx + (level.random.nextDouble() - 0.5) * 0.8,
                    y,
                    cz + (level.random.nextDouble() - 0.5) * 0.8,
                    1,
                    0, 0, 0,
                    0
            );
        }

        // 月の粒
        level.sendParticles(
                ParticleTypes.SOUL_FIRE_FLAME,
                cx, cy + 1.5, cz,
                60,
                1.0, 1.5, 1.0,
                0.1
        );

        // 光の輪
        for (int i = 0; i < 32; i++) {
            double angle = (i / 32.0) * Math.PI * 2;
            double r = 2.0;
            level.sendParticles(
                    ParticleTypes.FLASH,
                    cx + Math.cos(angle) * r,
                    cy + 0.5,
                    cz + Math.sin(angle) * r,
                    1,
                    0, 0, 0,
                    0
            );
        }

        // =========================================================
        // アイテム変換: ネザライトの剣 → ノクスルクス
        // =========================================================
        if (!player.getAbilities().instabuild) {
            held.shrink(1);
        }

        ItemStack noxlux = new ItemStack(Items.NOXLUX.get());

        if (!player.getInventory().add(noxlux)) {
            player.drop(noxlux, false);
        }

        // =========================================================
        // 視界暗転（クライアントへパケット送信）
        // =========================================================
        com.tyami.forlaism.network.FactotumPacketHandler.CHANNEL.sendTo(
                new com.tyami.forlaism.network.DarkenScreenPacket(40), // 2秒間
                player.connection.connection,
                net.minecraftforge.network.NetworkDirection.PLAY_TO_CLIENT
        );

        // =========================================================
        // メッセージ
        // =========================================================
        player.displayClientMessage(
                Component.literal("§5§l夜に沈む剣 §fが §d顕現 §fした…")
                        .withStyle(net.minecraft.ChatFormatting.DARK_PURPLE),
                true
        );
    }
}