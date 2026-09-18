package com.tyami.forlaism.event;

import com.tyami.forlaism.registry.Fluids;
import com.tyami.forlaism.registry.Items;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.material.FluidState;

import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Mod.EventBusSubscriber
public class DreamFragmentSkyHandler {

    /*
     * ========================================
     * 設定
     * ========================================
     */

    // 必要時間：60秒
    // Minecraftは20tick = 1秒
    private static final int REQUIRED_TIME = 20 * 60;

    // 発動する時間帯
    // Minecraft時間で 0:00 ～ 4:00
    private static final long NIGHT_START = 18000L;
    private static final long NIGHT_END = 22000L;

    // 視線判定
    // 45度以上、上を向いていればOK
    private static final float LOOK_ANGLE = -45.0F;

    // 空が見えるか確認する最大距離
    private static final int SKY_CHECK_DISTANCE = 256;


    /*
     * ========================================
     * プレイヤーごとの待機時間
     * ========================================
     */

    private static final Map<UUID, Integer> timers =
            new HashMap<>();


    /*
     * ========================================
     * 毎tick処理
     * ========================================
     */

    @SubscribeEvent
    public static void onLivingTick(
            LivingEvent.LivingTickEvent event
    ) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }

        if (player.level().isClientSide()) {
            return;
        }

        UUID uuid = player.getUUID();


        /*
         * ----------------------------------------
         * 温泉にいるか
         * ----------------------------------------
         */

        if (!isInOnsen(player)) {
            timers.remove(uuid);
            return;
        }


        /*
         * ----------------------------------------
         * 深夜か
         * ----------------------------------------
         */

        long time = player.level().getDayTime() % 24000L;

        if (time < NIGHT_START || time >= NIGHT_END) {
            timers.remove(uuid);
            return;
        }


        /*
         * ----------------------------------------
         * 真上に空があるか
         * ----------------------------------------
         */

        if (!hasOpenSkyAbove(player)) {
            timers.remove(uuid);
            return;
        }


        /*
         * ----------------------------------------
         * 上を向いているか
         * ----------------------------------------
         */

        if (!isLookingUp(player)) {
            timers.remove(uuid);
            return;
        }


        /*
         * ----------------------------------------
         * タイマー
         * ----------------------------------------
         */

        int timer =
                timers.getOrDefault(uuid, 0) + 1;

        if (timer >= REQUIRED_TIME) {

            timers.remove(uuid);

            summonDreamFragment(player);

        } else {

            timers.put(uuid, timer);

        }
    }


    /*
     * ========================================
     * 温泉判定
     * ========================================
     */

    private static boolean isInOnsen(
            ServerPlayer player
    ) {
        Level level = player.level();

        BlockPos feetPos =
                BlockPos.containing(
                        player.getX(),
                        player.getY(),
                        player.getZ()
                );

        BlockPos bodyPos =
                BlockPos.containing(
                        player.getX(),
                        player.getY()
                                + player.getBbHeight()
                                * 0.6D,
                        player.getZ()
                );

        return isOnsen(
                level.getFluidState(feetPos)
        ) || isOnsen(
                level.getFluidState(bodyPos)
        );
    }


    private static boolean isOnsen(
            FluidState fluid
    ) {
        return fluid.getType() == Fluids.ONSEN.get()
                || fluid.getType()
                        == Fluids.ONSEN_FLOWING.get();
    }


    /*
     * ========================================
     * 空が見えるか
     * ========================================
     */

    private static boolean hasOpenSkyAbove(
            ServerPlayer player
    ) {
        Level level = player.level();

        BlockPos pos =
                BlockPos.containing(
                        player.getX(),
                        player.getY(),
                        player.getZ()
                );

        /*
         * プレイヤーの真上を確認。
         *
         * 1ブロックでも遮っていたらアウト。
         */

        for (int i = 1; i <= SKY_CHECK_DISTANCE; i++) {

            BlockPos above = pos.above(i);

            if (!level.getBlockState(above).isAir()) {
                return false;
            }
        }

        return true;
    }


    /*
     * ========================================
     * 上を向いているか
     * ========================================
     */

    private static boolean isLookingUp(
            ServerPlayer player
    ) {

        /*
         * MinecraftのXRotは
         *
         * 0   = 水平
         * -90 = 真上
         * 90  = 真下
         *
         * なので -45 以下なら
         * 45度以上、上を向いている。
         */

        return player.getXRot() <= LOOK_ANGLE;
    }


    /*
     * ========================================
     * Dream Fragmentを空から落とす
     * ========================================
     */

    private static void summonDreamFragment(
            ServerPlayer player
    ) {

        if (!(player.level()
                instanceof ServerLevel level)) {
            return;
        }

        /*
         * プレイヤーの真上。
         *
         * ここでは高さ30ブロックから
         * 落下させる。
         */

        double x = player.getX();
        double y = player.getY() + 30.0D;
        double z = player.getZ();

        ItemStack stack =
                new ItemStack(
                        Items.DREAM_FRAGMENT.get()
                );

        ItemEntity item =
                new ItemEntity(
                        level,
                        x,
                        y,
                        z,
                        stack
                );

        /*
         * 少しだけ落下する方向へ
         * 初速を与える。
         */

        item.setDeltaMovement(
                0.0D,
                -0.05D,
                0.0D
        );

        level.addFreshEntity(item);
    }
}