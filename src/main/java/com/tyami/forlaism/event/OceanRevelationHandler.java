package com.tyami.forlaism.event;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;

import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * 海で死んだ時に「啓示」が降りる処理。
 *
 * 死因が溺死 or 水中で死亡 のとき、
 * リスポーン後にチャットで儀式のヒントを流す。
 */
@Mod.EventBusSubscriber(modid = "forlaism")
public final class OceanRevelationHandler {

    private OceanRevelationHandler() {
    }

    // =========================================================
    // 死亡時に海判定
    // =========================================================

    @SubscribeEvent
    public static void onPlayerDeath(LivingDeathEvent event) {

        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }

        // 海（水）の中で死んだか
        if (!isInWater(player)) {
            return;
        }

        // 啓示を予約
        RevelationData.schedule(player);
    }

    private static boolean isInWater(ServerPlayer player) {

        BlockPos feet = BlockPos.containing(player.getX(), player.getY(), player.getZ());
        BlockPos head = BlockPos.containing(
                player.getX(),
                player.getY() + player.getBbHeight() * 0.8,
                player.getZ()
        );

        return isWater(player.level().getFluidState(feet))
                || isWater(player.level().getFluidState(head));
    }

    private static boolean isWater(FluidState fluid) {
        return fluid.getType() == Fluids.WATER
                || fluid.getType() == Fluids.FLOWING_WATER;
    }

    // =========================================================
    // リスポーン時に啓示を表示
    // =========================================================

    @SubscribeEvent
    public static void onRespawn(net.minecraftforge.event.entity.player.PlayerEvent.PlayerRespawnEvent event) {

        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }

        if (!RevelationData.consume(player)) {
            return;
        }

        showRevelation(player);
    }

    private static void showRevelation(ServerPlayer player) {

        // 演出: 暗転 + 吐き気 + 音
        player.addEffect(new MobEffectInstance(
                MobEffects.BLINDNESS,
                100,
                0,
                false,
                false,
                false
        ));

        player.addEffect(new MobEffectInstance(
                MobEffects.CONFUSION,
                120,
                0,
                false,
                false,
                false
        ));

        player.level().playSound(
                null,
                player.getX(), player.getY(), player.getZ(),
                SoundEvents.ELDER_GUARDIAN_CURSE,
                SoundSource.PLAYERS,
                1.0F, 0.5F
        );

        // チャットメッセージ（複数行で荘厳に）
        player.sendSystemMessage(Component.literal(""));
        player.sendSystemMessage(Component.literal("§3§l…海の底から、声が聞こえる…"));
        player.sendSystemMessage(Component.literal(""));
        player.sendSystemMessage(Component.literal("§b「生贄を捧げよ。」"));
        player.sendSystemMessage(Component.literal("§b「儀式の書を作り、深淵へ身を投げよ。」"));
        player.sendSystemMessage(Component.literal("§b「然れば、汝は新たな生贄を得るだろう。」"));
        player.sendSystemMessage(Component.literal(""));
        player.sendSystemMessage(Component.literal("§7※ 本と羽ペンで『儀式の書』を作り、"));
        player.sendSystemMessage(Component.literal("§7　 『生贄を捧げる』と書き記し、"));
        player.sendSystemMessage(Component.literal("§7　 奈落へ落ちよ。"));
        player.sendSystemMessage(Component.literal(""));

        player.displayClientMessage(
                Component.literal("§3啓示を受けた…"),
                true
        );
    }

    // =========================================================
    // 死亡→リスポーンの橋渡し
    // =========================================================

    private static final class RevelationData {

        private static final java.util.Set<java.util.UUID> PENDING = new java.util.HashSet<>();

        static void schedule(ServerPlayer player) {
            PENDING.add(player.getUUID());
        }

        static boolean consume(ServerPlayer player) {
            return PENDING.remove(player.getUUID());
        }
    }
}