package com.tyami.forlaism.event;

import com.tyami.forlaism.registry.Items;
import com.tyami.forlaism.world.DreamDimension;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import net.minecraftforge.event.entity.player.PlayerSleepInBedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Dreamディメンションへの遷移処理。
 *
 * 条件:
 *   - 深夜 (18000 ～ 22000)
 *   - HP が 3 以下
 *   - 夢幻の欠片 (dream_fragment) を所持
 *   - ベッドで寝ようとする
 *
 * 遷移先:
 *   forlaism:dream の 0, -63, 0
 */
@Mod.EventBusSubscriber(modid = "forlaism")
public final class DreamSleepHandler {

    /** HP条件。 */
    private static final float HP_THRESHOLD = 3.0F;

    /** Dreamディメンションのスポーン座標。 */
    private static final double SPAWN_X = 0.5D;
    private static final double SPAWN_Y = -63.0D;
    private static final double SPAWN_Z = 0.5D;

    private DreamSleepHandler() {
    }

    @SubscribeEvent
    public static void onSleep(PlayerSleepInBedEvent event) {

        Player player = event.getEntity();

        if (player.level().isClientSide) {
            return;
        }

        if (!(player instanceof ServerPlayer serverPlayer)) {
            return;
        }

        // ---- 条件1: 深夜か ----
        long time = player.level().getDayTime() % 24000L;
        boolean isMidnight = time >= 18000L && time < 22000L;
        if (!isMidnight) {
            return;
        }

        // ---- 条件2: HPが3以下か ----
        if (player.getHealth() > HP_THRESHOLD) {
            return;
        }

        // ---- 条件3: 夢幻の欠片を持っているか ----
        if (!hasDreamFragment(player)) {
            return;
        }

        // ---- 条件クリア！Dreamへ ----
        teleportToDream(serverPlayer);

        // ベッドでの睡眠はキャンセル（寝た判定にはしない）
        event.setResult(Player.BedSleepingProblem.OTHER_PROBLEM);

        // 夢幻の欠片を1個消費（お好みで）
        consumeDreamFragment(player);
    }

    private static boolean hasDreamFragment(Player player) {
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            ItemStack stack = player.getInventory().getItem(i);
            if (stack.is(Items.DREAM_FRAGMENT.get())) {
                return true;
            }
        }
        return false;
    }

    private static void consumeDreamFragment(Player player) {
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            ItemStack stack = player.getInventory().getItem(i);
            if (stack.is(Items.DREAM_FRAGMENT.get())) {
                stack.shrink(1);
                return;
            }
        }
    }

    private static void teleportToDream(ServerPlayer player) {

        ServerLevel dream = player.server.getLevel(DreamDimension.DREAM_LEVEL);

        if (dream == null) {
            player.displayClientMessage(
                    Component.literal("§cDreamディメンションが見つかりません。"),
                    true
            );
            return;
        }

        // テレポート
        player.teleportTo(
                dream,
                SPAWN_X,
                SPAWN_Y,
                SPAWN_Z,
                player.getYRot(),
                player.getXRot()
        );

        // スポーン地点をリスポーン地点に設定（ベッド無しなので任意）
        BlockPos spawnPos = BlockPos.containing(SPAWN_X, SPAWN_Y, SPAWN_Z);
        player.setRespawnPosition(
                DreamDimension.DREAM_LEVEL,
                spawnPos,
                player.getYRot(),
                true,
                false
        );

        // 演出
        player.displayClientMessage(
                Component.literal("§d§l微睡む夢の淵へ…"),
                true
        );
    }
}