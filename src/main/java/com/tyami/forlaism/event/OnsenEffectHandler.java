package com.tyami.forlaism.event;
import com.tyami.forlaism.registry.Fluids;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.FlyingMob;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.material.FluidState;
import net.minecraftforge.event.entity.living.LivingChangeTargetEvent;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.event.entity.living.LivingKnockBackEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
@Mod.EventBusSubscriber
public class OnsenEffectHandler {
/*
 * =========================================================
 * 温泉判定
 * =========================================================
 */
public static boolean isInOnsen(LivingEntity entity) {
    Level level = entity.level();
    BlockPos feetPos = BlockPos.containing(
            entity.getX(),
            entity.getY(),
            entity.getZ()
    );
    BlockPos bodyPos = BlockPos.containing(
            entity.getX(),
            entity.getY() + entity.getBbHeight() * 0.6D,
            entity.getZ()
    );
    return isOnsen(level.getFluidState(feetPos))
            || isOnsen(level.getFluidState(bodyPos));
}
private static boolean isOnsen(FluidState fluid) {
    return fluid.getType() == Fluids.ONSEN.get()
            || fluid.getType() == Fluids.ONSEN_FLOWING.get();
}
/*
 * =========================================================
 * UOM
 * =========================================================
 */
/**
 * fantasy_ending のUOM EntityTypeを取得する。
 *
 * fantasy_ending が存在しない場合は null。
 *
 * その場合、夢幻の欠片チャレンジだけ無効になる。
 */
private static EntityType<?> getUltimateOrderManagerType() {
    ResourceLocation id =
            new ResourceLocation(
                    "fantasy_ending",
                    "ultimate_order_manager"
            );
    return ForgeRegistries.ENTITY_TYPES.getValue(id);
}
/*
 * =========================================================
 * 毎tick処理
 * =========================================================
 */
@SubscribeEvent
public static void onLivingTick(
        LivingEvent.LivingTickEvent event
) {
    LivingEntity entity =
            event.getEntity();
    if (entity.level().isClientSide()) {
        return;
    }
    /*
     * =====================================================
     * 夢幻の欠片
     * =====================================================
     */
    if (entity instanceof ServerPlayer player) {
        tickDreamFragmentChallenge(player);
    }
    /*
     * =====================================================
     * 温泉外ならここから先は終了
     * =====================================================
     */
    if (!isInOnsen(entity)) {
        return;
    }
    /*
     * =====================================================
     * 沈み込み防止
     * =====================================================
     */
    preventDeepSubmersion(entity);
    /*
     * =====================================================
     * 飛行Mob強制入浴
     * =====================================================
     */
    forceFlyingMobIntoOnsen(entity);
    /*
     * =====================================================
     * 再生
     * =====================================================
     */
    entity.getActiveEffects().removeIf(effect ->
            !effect.getEffect().isBeneficial()
    );
    entity.addEffect(
            new MobEffectInstance(
                    MobEffects.REGENERATION,
                    40,
                    3,
                    true,
                    true
            )
    );
    /*
     * =====================================================
     * 温泉中のMob
     * =====================================================
     */
    if (entity instanceof Mob mob) {
        mob.setTarget(null);
        mob.stopUsingItem();
        mob.setSwimming(false);
    }
    /*
     * =====================================================
     * 周囲のMobからターゲット解除
     * =====================================================
     */
    List<Mob> mobs =
            entity.level().getEntitiesOfClass(
                    Mob.class,
                    entity.getBoundingBox()
                            .inflate(16.0D)
            );
    for (Mob mob : mobs) {
        if (mob.getTarget() == entity) {
            mob.setTarget(null);
        }
    }
    /*
     * =====================================================
     * 湯気
     * =====================================================
     */
    if (entity.level()
            instanceof ServerLevel serverLevel) {
        if (entity.getRandom().nextFloat()
                < 0.10F) {
            spawnSteamFromOnsen(
                    serverLevel,
                    entity
            );
        }
    }
}
/*
 * =========================================================
 * 飛行Mob強制入浴
 * =========================================================
 */
private static void forceFlyingMobIntoOnsen(
        LivingEntity entity
) {
    if (!(entity instanceof FlyingMob flyingMob)) {
        return;
    }
    Level level =
            flyingMob.level();
    BlockPos center =
            flyingMob.blockPosition();
    for (int x = -3; x <= 3; x++) {
        for (int y = -3; y <= 3; y++) {
            for (int z = -3; z <= 3; z++) {
                double distanceSqr =
                        x * x
                        + y * y
                        + z * z;
                if (distanceSqr > 9.0D) {
                    continue;
                }
                BlockPos pos =
                        center.offset(x, y, z);
                FluidState fluid =
                        level.getFluidState(pos);
                if (!isOnsen(fluid)) {
                    continue;
                }
                double surfaceY =
                        pos.getY()
                        + fluid.getHeight(
                                level,
                                pos
                        );
                double targetY =
                        surfaceY - 0.8D;
                flyingMob.setPos(
                        flyingMob.getX(),
                        targetY,
                        flyingMob.getZ()
                );
                flyingMob.setDeltaMovement(
                        0.0D,
                        0.0D,
                        0.0D
                );
                flyingMob.setNoGravity(false);
                flyingMob.setSwimming(false);
                return;
            }
        }
    }
}
/*
 * =========================================================
 * ノックバック禁止
 * =========================================================
 */
@SubscribeEvent
public static void onKnockBack(
        LivingKnockBackEvent event
) {
    LivingEntity entity =
            event.getEntity();
    if (entity.level().isClientSide()) {
        return;
    }
    if (isInOnsen(entity)) {
        event.setCanceled(true);
    }
}
/*
 * =========================================================
 * 湯気
 * =========================================================
 */
private static void spawnSteamFromOnsen(
        ServerLevel level,
        LivingEntity entity
) {
    BlockPos center =
            entity.blockPosition();
    for (int i = 0; i < 8; i++) {
        int x =
                center.getX()
                + entity.getRandom().nextInt(7)
                - 3;
        int z =
                center.getZ()
                + entity.getRandom().nextInt(7)
                - 3;
        int y =
                center.getY();
        BlockPos pos =
                new BlockPos(x, y, z);
        FluidState fluid =
                level.getFluidState(pos);
        if (!isOnsen(fluid)) {
            continue;
        }
        BlockPos above =
                pos.above();
        if (!level.getBlockState(above).isAir()) {
            continue;
        }
        double height =
                fluid.getHeight(
                        level,
                        pos
                );
        double px =
                x
                + 0.2D
                + entity.getRandom().nextDouble()
                * 0.6D;
        double py =
                y
                + height
                + 0.05D;
        double pz =
                z
                + 0.2D
                + entity.getRandom().nextDouble()
                * 0.6D;
        level.sendParticles(
                ParticleTypes.CAMPFIRE_COSY_SMOKE,
                px,
                py,
                pz,
                1,
                0.03D,
                0.03D,
                0.03D,
                0.005D
        );
        return;
    }
}
/*
 * =========================================================
 * ターゲット変更禁止
 * =========================================================
 */
@SubscribeEvent
public static void onTargetChange(
        LivingChangeTargetEvent event
) {
    LivingEntity newTarget =
            event.getNewTarget();
    if (newTarget == null) {
        return;
    }
    if (isInOnsen(newTarget)) {
        event.setNewTarget(null);
        return;
    }
    LivingEntity entity =
            event.getEntity();
    if (entity instanceof Mob mob) {
        if (isInOnsen(mob)) {
            event.setNewTarget(null);
        }
    }
}
/*
 * =========================================================
 * 沈み込み防止
 * =========================================================
 */
private static void preventDeepSubmersion(
        LivingEntity entity
) {
    Level level =
            entity.level();
    BlockPos basePos =
            BlockPos.containing(
                    entity.getX(),
                    entity.getY(),
                    entity.getZ()
            );
    for (int y = 2; y >= -2; y--) {
        BlockPos pos =
                basePos.above(y);
        FluidState fluid =
                level.getFluidState(pos);
        if (!isOnsen(fluid)) {
            continue;
        }
        double surfaceY =
                pos.getY()
                + fluid.getHeight(
                        level,
                        pos
                );
        double minimumY =
                surfaceY - 1.5D;
        if (entity.getY() < minimumY) {
            entity.setPos(
                    entity.getX(),
                    minimumY,
                    entity.getZ()
            );
            entity.setDeltaMovement(
                    entity.getDeltaMovement().x,
                    Math.max(
                            entity.getDeltaMovement().y,
                            0.0D
                    ),
                    entity.getDeltaMovement().z
            );
        }
        return;
    }
}
/*
 * =========================================================
 * 夢幻の欠片
 * =========================================================
 */
/**
 * テスト中は1tick。
 *
 * 本番では200tick = 10秒。
 */
private static final int DREAM_FRAGMENT_TIME = 120;
/**
 * UOM検索範囲。
 *
 * 直径16ブロック
 * = 半径8ブロック
 *
 * 3次元距離で判定する。
 */
private static final double DREAM_FRAGMENT_RADIUS = 8.0D;
private static final Map<UUID, Integer>
        dreamFragmentTimers =
        new HashMap<>();
/**
 * 夢幻の欠片チャレンジ。
 *
 * 条件：
 *
 * 1. プレイヤーが温泉に入っている
 * 2. プレイヤーを中心とした
 *    半径8ブロックの球体内に
 *    fantasy_ending:ultimate_order_manager
 *    が存在する
 *
 * UOM自身が温泉にいる必要はない。
 *
 * UOMが球体の外へ出ても、
 * タイマーはリセットしない。
 *
 * プレイヤーが温泉から出た場合のみ
 * タイマーをリセットする。
 *
 * fantasy_endingが存在しない場合は、
 * UOMを取得できないため
 * このチャレンジは何もせず終了する。
 */
private static void tickDreamFragmentChallenge(
        ServerPlayer player
) {
    UUID uuid =
            player.getUUID();
    /*
     * =====================================================
     * プレイヤーが温泉にいるか
     * =====================================================
     */
    if (!isInOnsen(player)) {
        dreamFragmentTimers.remove(uuid);
        return;
    }
    /*
     * =====================================================
     * UOM EntityType取得
     * =====================================================
     *
     * fantasy_endingが存在しなければnull。
     */
    EntityType<?> uomType =
            getUltimateOrderManagerType();
    if (uomType == null) {
        return;
    }
    /*
     * =====================================================
     * 半径8ブロックの立体範囲
     * =====================================================
     */
    double radius =
            DREAM_FRAGMENT_RADIUS;
    double radiusSqr =
            radius * radius;
    /*
     * =====================================================
     * プレイヤー周辺のLivingEntity検索
     * =====================================================
     *
     * getBoundingBox().inflate(8) は
     * まず8×8×8の立方体で候補を取得。
     *
     * その後 distanceToSqr() で
     * 本当に球体内部かを判定する。
     */
    List<LivingEntity> entities =
            player.level().getEntitiesOfClass(
                    LivingEntity.class,
                    player.getBoundingBox()
                            .inflate(radius)
            );
    boolean uomNearby =
            false;
    for (LivingEntity entity : entities) {
        /*
         * UOMか確認
         */
        if (entity.getType() != uomType) {
            continue;
        }
        /*
         * 3次元距離で球体判定
         */
        if (entity.distanceToSqr(player)
                <= radiusSqr) {
            uomNearby = true;
            break;
        }
    }
    /*
     * =====================================================
     * UOMが球体内にいない
     * =====================================================
     *
     * タイマーはリセットしない。
     */
    if (!uomNearby) {
        return;
    }
    /*
     * =====================================================
     * タイマー進行
     * =====================================================
     */
    int timer =
            dreamFragmentTimers.getOrDefault(
                    uuid,
                    0
            ) + 1;
    /*
     * =====================================================
     * テスト用：1tickで達成
     * =====================================================
     */
    if (timer >= DREAM_FRAGMENT_TIME) {
        giveDreamFragment(player);
        dreamFragmentTimers.remove(uuid);
    } else {
        dreamFragmentTimers.put(
                uuid,
                timer
        );
    }
}
/*
 * =========================================================
 * 夢幻の欠片付与
 * =========================================================
 */
private static void giveDreamFragment(
        ServerPlayer player
) {
    ItemStack reward =
            new ItemStack(
                    com.tyami.forlaism.registry.Items
                            .DREAM_FRAGMENT
                            .get()
            );
    if (!player.getInventory().add(reward)) {
        player.drop(
                reward,
                false
        );
    }
}
}
