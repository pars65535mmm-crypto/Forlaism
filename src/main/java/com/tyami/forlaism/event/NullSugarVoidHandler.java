package com.tyami.forlaism.event;

import com.tyami.forlaism.Forlaism;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;

import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.Iterator;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 砂糖が奈落に落ちたとき、1/8192 の確率で null_sugar に変化し、
 * 奈落から浮上してプレイヤーの元へ戻ってくる。
 *
 * Y < -63 に到達した砂糖のItemEntityを対象とする。
 */
@Mod.EventBusSubscriber(modid = Forlaism.MOD_ID)
public class NullSugarVoidHandler {

    /** null_sugar が生成される確率の分母。 */
    //public static final int NULL_SUGAR_DENOMINATOR = 8192;
    public static final int NULL_SUGAR_DENOMINATOR = 8192;

    /** 奈落判定のY座標。 */
    private static final double VOID_Y = -63.0D;

    /** 浮上開始Y座標。 */
    private static final double ASCEND_START_Y = -62.0D;

    /** 浮上にかける時間（秒）。 */
    private static final double ASCEND_DURATION_SECONDS = 5.0D;

    /** 浮上完了後にさらに上へ飛ばすY座標（プレイヤー到達を保証）。 */
    private static final double ASCEND_TARGET_Y = 320.0D;

    private static final Random RANDOM = new Random();

    /** 浮上中のItemEntityを追跡。 */
    private static final Map<Integer, AscendData> ASCENDING = new ConcurrentHashMap<>();

    private static class AscendData {
        final ServerLevel level;
        final long startTick;
        boolean initialized = false;

        AscendData(ServerLevel level, long startTick) {
            this.level = level;
            this.startTick = startTick;
        }
    }

    @SubscribeEvent
    public static void onLevelTick(TickEvent.LevelTickEvent event) {

        if (event.phase != TickEvent.Phase.END) return;
        if (event.level.isClientSide) return;
        if (!(event.level instanceof ServerLevel serverLevel)) return;

        // =========================================================
        // 1. 浮上中のItemEntityを処理
        // =========================================================
        tickAscending(serverLevel);

        // =========================================================
        // 2. 奈落チェック（5tickごと）
        // =========================================================
        if (serverLevel.getGameTime() % 5 != 0) return;

        AABB voidArea = new AABB(
                -30000000, -128, -30000000,
                30000000, VOID_Y, 30000000
        );

        for (ItemEntity itemEntity : serverLevel.getEntitiesOfClass(
                ItemEntity.class,
                voidArea,
                e -> e.isAlive() && !e.getItem().isEmpty()
        )) {

            ItemStack stack = itemEntity.getItem();

            // 砂糖以外は無視
            if (!stack.is(net.minecraft.world.item.Items.SUGAR)) continue;

            // 1/8192 判定
            if (RANDOM.nextInt(NULL_SUGAR_DENOMINATOR) == 0) {

                // null_sugar に変化
                ItemStack nullSugar = new ItemStack(
                        com.tyami.forlaism.registry.Items.NULL_SUGAR.get(),
                        stack.getCount()
                );
                itemEntity.setItem(nullSugar);

                // 奈落から救出（-62 に固定）
                itemEntity.setPos(
                        itemEntity.getX(),
                        ASCEND_START_Y,
                        itemEntity.getZ()
                );
                itemEntity.setNoGravity(true);
                itemEntity.setDeltaMovement(0.0D, 0.0D, 0.0D);

                // 浮上リストに追加
                ASCENDING.put(
                        itemEntity.getId(),
                        new AscendData(serverLevel, serverLevel.getGameTime())
                );

                // 演出
                serverLevel.sendParticles(
                        ParticleTypes.PORTAL,
                        itemEntity.getX(),
                        itemEntity.getY(),
                        itemEntity.getZ(),
                        50,
                        0.5, 0.5, 0.5,
                        0.3
                );

                serverLevel.sendParticles(
                        ParticleTypes.END_ROD,
                        itemEntity.getX(),
                        itemEntity.getY(),
                        itemEntity.getZ(),
                        20,
                        0.3, 0.3, 0.3,
                        0.05
                );

                serverLevel.playSound(
                        null,
                        itemEntity.getX(),
                        itemEntity.getY(),
                        itemEntity.getZ(),
                        SoundEvents.END_PORTAL_SPAWN,
                        SoundSource.PLAYERS,
                        1.0F,
                        0.5F
                );

            } else {
                // はずれたら消滅
                itemEntity.discard();
            }
        }
    }

    // =========================================================
    // 浮上処理
    // =========================================================

    private static void tickAscending(ServerLevel serverLevel) {

        Iterator<Map.Entry<Integer, AscendData>> iterator =
                ASCENDING.entrySet().iterator();

        while (iterator.hasNext()) {

            Map.Entry<Integer, AscendData> entry = iterator.next();
            AscendData data = entry.getValue();

            // 別ディメンションならスキップ
            if (data.level != serverLevel) continue;

            ItemEntity itemEntity =
                    (ItemEntity) serverLevel.getEntity(entry.getKey());

            // エンティティが消えていたら追跡解除
            if (itemEntity == null || itemEntity.isRemoved()) {
                iterator.remove();
                continue;
            }

            // 既に奈落から十分浮上していたら解除
            if (itemEntity.getY() >= ASCEND_TARGET_Y) {
                itemEntity.setNoGravity(false);
                itemEntity.setDeltaMovement(0.0D, 0.0D, 0.0D);

                serverLevel.sendParticles(
                        ParticleTypes.END_ROD,
                        itemEntity.getX(),
                        itemEntity.getY(),
                        itemEntity.getZ(),
                        30,
                        0.5, 0.5, 0.5,
                        0.1
                );

                iterator.remove();
                continue;
            }

            // 補間移動
            long elapsedTicks =
                    serverLevel.getGameTime() - data.startTick;
            double totalTicks =
                    ASCEND_DURATION_SECONDS * 20.0D;
            double progress =
                    Math.min(1.0D, elapsedTicks / totalTicks);
            double eased = easeInOutCubic(progress);

            double newY =
                    ASCEND_START_Y
                            + (ASCEND_TARGET_Y - ASCEND_START_Y)
                            * eased;

            itemEntity.setPos(
                    itemEntity.getX(),
                    newY,
                    itemEntity.getZ()
            );

            // 上昇速度をdeltaMovementにも反映（パーティクル用）
            double deltaProgress = 0.01D;
            double nextProgress =
                    Math.min(1.0D, progress + deltaProgress);
            double nextEased = easeInOutCubic(nextProgress);
            double speed =
                    (nextEased - eased)
                            / deltaProgress
                            * 0.5D;

            itemEntity.setDeltaMovement(0.0D, speed, 0.0D);

            // 浮上中のパーティクル
            if (serverLevel.getGameTime() % 3 == 0) {
                serverLevel.sendParticles(
                        ParticleTypes.END_ROD,
                        itemEntity.getX(),
                        itemEntity.getY(),
                        itemEntity.getZ(),
                        1,
                        0.05, 0.05, 0.05,
                        0.01
                );
            }
        }
    }

    // =========================================================
    // イージング関数
    // =========================================================

    private static double easeInOutCubic(double t) {
        if (t < 0.5D) {
            return 4.0D * t * t * t;
        } else {
            return 1.0D - Math.pow(-2.0D * t + 2.0D, 3.0D) / 2.0D;
        }
    }
}