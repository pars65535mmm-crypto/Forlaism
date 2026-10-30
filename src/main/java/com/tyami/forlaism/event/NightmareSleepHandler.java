package com.tyami.forlaism.event;

import com.tyami.forlaism.world.NightmareHelper;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;

import net.minecraftforge.event.entity.player.PlayerSleepInBedEvent;
import net.minecraftforge.event.entity.player.PlayerWakeUpEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * ナイトメアモード中はベッドで寝られない。
 *
 * 挙動:
 *   1. ベッドを右クリック
 *   2. 就寝判定をキャンセル
 *   3. 悪夢演出（暗転・不穏な音・パーティクル）
 *   4. 強制的に「目が覚めた」扱い
 *   5. ペナルティ: 鈍足II + 吐き気I + 暗闇（短時間）
 */
@Mod.EventBusSubscriber(modid = "forlaism")
public final class NightmareSleepHandler {

    private NightmareSleepHandler() {
    }

    @SubscribeEvent
    public static void onSleep(PlayerSleepInBedEvent event) {

        Player player = event.getEntity();

        if (player.level().isClientSide) return;

        // ナイトメアじゃなければ通常通り寝られる
        if (!NightmareHelper.isActive(player)) return;

        // 就寝をキャンセル
        event.setResult(Player.BedSleepingProblem.OTHER_PROBLEM);

        // 演出
        if (player instanceof ServerPlayer sp) {
            playNightmare(sp);
        }
    }

    /**
     * 悪夢演出 + ペナルティ。
     */
    private static void playNightmare(ServerPlayer player) {

        ServerLevel level = player.serverLevel();

        // =========================================================
        // 1. 不穏なサウンド
        // =========================================================
        level.playSound(
                null,
                player.getX(), player.getY(), player.getZ(),
                SoundEvents.WARDEN_HEARTBEAT,
                SoundSource.AMBIENT,
                1.5F, 0.5F
        );

        level.playSound(
                null,
                player.getX(), player.getY(), player.getZ(),
                SoundEvents.ELDER_GUARDIAN_CURSE,
                SoundSource.AMBIENT,
                1.2F, 0.7F
        );

        level.playSound(
                null,
                player.getX(), player.getY(), player.getZ(),
                SoundEvents.SCULK_SHRIEKER_SHRIEK,
                SoundSource.AMBIENT,
                1.0F, 0.5F
        );

        // =========================================================
        // 2. パーティクル（不穏なオーラ）
        // =========================================================
        level.sendParticles(
                ParticleTypes.SCULK_SOUL,
                player.getX(), player.getY() + 1.0, player.getZ(),
                40,
                1.0, 1.0, 1.0,
                0.05
        );

        level.sendParticles(
                ParticleTypes.SQUID_INK,
                player.getX(), player.getY() + 1.0, player.getZ(),
                20,
                0.8, 0.8, 0.8,
                0.02
        );

        // =========================================================
        // 3. メッセージ
        // =========================================================
        player.displayClientMessage(
                Component.literal("§5§lひどい悪夢だ")
                        .withStyle(net.minecraft.ChatFormatting.DARK_PURPLE),
                true
        );

        // =========================================================
        // 4. ペナルティ付与
        // =========================================================
        // 鈍足II（15秒）
        player.addEffect(new MobEffectInstance(
                MobEffects.MOVEMENT_SLOWDOWN,
                300, 1,
                false, false, true
        ));

        // 吐き気I（10秒）
        player.addEffect(new MobEffectInstance(
                MobEffects.CONFUSION,
                200, 0,
                false, false, true
        ));

        // 暗闇（5秒）
        player.addEffect(new MobEffectInstance(
                MobEffects.DARKNESS,
                100, 0,
                false, false, true
        ));

        // 即座に「起きた」状態にする（起床イベントも走らせる）
        // → ベッド未使用扱いでリスポーン地点はセットされない
        player.stopSleeping();
    }
}