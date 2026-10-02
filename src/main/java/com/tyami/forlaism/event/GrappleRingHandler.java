package com.tyami.forlaism.event;

import com.tyami.forlaism.registry.Items;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import top.theillusivec4.curios.api.CuriosApi;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * グラップルリングの動作処理。
 *
 * 【動きの演出】
 *   発射直後: 加速 (0 → MAX)
 *   中盤   : 最高速
 *   終盤   : 減速 (MAX → 0)
 *
 *   → イージング関数で「ビュン！」を表現
 *
 * 【ロープ描画】
 *   プレイヤー ↔ 着弾点 を毎tick パーティクルで線を引く
 *   → 「引っ張られてる感」を出す
 *
 * 【解除】
 *   Shift or 到達 or 再度発射
 *
 * 【落下無効】
 *   発射中 + 解除後3秒 (60tick)
 */
@Mod.EventBusSubscriber(modid = "forlaism")
public final class GrappleRingHandler {

    /** 射程。 */
    public static final double RANGE = 100.0D;

    /** 1tickあたりの最高速度（ブロック/tick）。 */
    public static final double MAX_SPEED = 2.8D;

    /** 最低速度（終盤の減速下限）。 */
    public static final double MIN_SPEED = 0.4D;

    /** 到達判定距離。 */
    public static final double REACH_DISTANCE = 0.5D;

    /** 解除後の落下無効tick。 */
    public static final int FALL_PROTECT_TICKS = 60;

    /** 最大発射時間（保険）。 */
    public static final int MAX_GRAPPLE_TICKS = 400;

    /** 加速にかけるtick数。 */
    public static final int ACCEL_TICKS = 8;

    /** 減速を始める距離（ブロック）。 */
    public static final double DECEL_DISTANCE = 3.0D;

    /**
     * 着弾点の上方向オフセット。
     */
    public static final double TARGET_Y_OFFSET = 2.3D;

    /** プレイヤーごとのグラップル状態。 */
    private static final Map<UUID, GrappleState> STATES = new HashMap<>();

    private GrappleRingHandler() {
    }

    // =========================================================
    // 状態
    // =========================================================

    private static class GrappleState {
        Vec3 target;
        double startDistance;  // 発射時の距離
        int ticks;
        int fallProtect;

        GrappleState(Vec3 target, double startDistance) {
            this.target = target;
            this.startDistance = startDistance;
            this.ticks = 0;
            this.fallProtect = 0;
        }
    }

    // =========================================================
    // 発射
    // =========================================================

    public static void fire(ServerPlayer player) {

        if (!hasRing(player)) return;

        Vec3 eye = player.getEyePosition();
        Vec3 look = player.getLookAngle().normalize();
        Vec3 end = eye.add(look.scale(RANGE));

        BlockHitResult hit = player.level().clip(new ClipContext(
                eye,
                end,
                ClipContext.Block.COLLIDER,
                ClipContext.Fluid.NONE,
                player
        ));

        if (hit.getType() != HitResult.Type.BLOCK) {
            if (player.level() instanceof ServerLevel sl) {
                sl.playSound(null, player.getX(), player.getY(), player.getZ(),
                        SoundEvents.DISPENSER_FAIL, SoundSource.PLAYERS, 0.8F, 1.5F);
            }
            return;
        }

        Vec3 hitPoint = hit.getLocation();
        Vec3 target = hitPoint.add(0.0D, TARGET_Y_OFFSET, 0.0D);

        // 発射時の距離を記録（緩急計算に使う）
        double startDist = player.position().distanceTo(target);

        // 既存フックを消して新規発射
        STATES.put(player.getUUID(), new GrappleState(target, startDist));

        if (player.level() instanceof ServerLevel sl) {
            // 発射演出
            spawnRopeParticles(sl, eye, hitPoint);
            spawnImpactParticles(sl, hitPoint);

            sl.playSound(null, player.getX(), player.getY(), player.getZ(),
                    SoundEvents.FISHING_BOBBER_THROW, SoundSource.PLAYERS, 1.0F, 1.2F);

            sl.playSound(null, hitPoint.x, hitPoint.y, hitPoint.z,
                    SoundEvents.CHAIN_PLACE, SoundSource.PLAYERS, 1.2F, 1.0F);
        }
    }

    // =========================================================
    // 毎tick処理
    // =========================================================

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {

        if (event.phase != TickEvent.Phase.END) return;

        Player player = event.player;
        if (player.level().isClientSide) return;
        if (!(player instanceof ServerPlayer sp)) return;

        GrappleState state = STATES.get(player.getUUID());

        // ---- 落下無効カウントダウン ----
        if (state != null && state.fallProtect > 0) {
            state.fallProtect--;
            if (state.fallProtect <= 0) {
                STATES.remove(player.getUUID());
                state = null;
            }
        }

        if (state == null) return;
        if (state.target == null) return;

        // ---- リング装備チェック ----
        if (!hasRing(player)) {
            release(sp);
            return;
        }

        // ---- Shiftで解除 ----
        if (player.isShiftKeyDown()) {
            release(sp);
            return;
        }

        // ---- 保険: 最大時間超過 ----
        state.ticks++;
        if (state.ticks > MAX_GRAPPLE_TICKS) {
            release(sp);
            return;
        }

        // ---- 引き寄せ ----
        pullPlayer(sp, state);

        // ---- ロープ描画 ----
        drawRope(sp, state);
    }

    /**
     * プレイヤーをフック地点へ引き寄せる。
     *
     * 発射直後 : 加速
     * 中盤     : 最高速
     * 終盤     : 減速
     */
    private static void pullPlayer(ServerPlayer player, GrappleState state) {

        if (state.target == null) return;

        Vec3 pos = player.position();
        Vec3 target = state.target;

        Vec3 diff = target.subtract(pos);
        double dist = diff.length();

        // ---- 到達判定 ----
        if (dist <= REACH_DISTANCE) {
            GrappleState newState = new GrappleState(null, 0);
            newState.fallProtect = FALL_PROTECT_TICKS;
            STATES.put(player.getUUID(), newState);

            player.setDeltaMovement(Vec3.ZERO);
            player.hurtMarked = true;
            player.fallDistance = 0.0F;

            if (player.level() instanceof ServerLevel sl) {
                sl.playSound(null, player.getX(), player.getY(), player.getZ(),
                        SoundEvents.CHAIN_BREAK, SoundSource.PLAYERS, 1.0F, 1.2F);

                // 着地パーティクル
                sl.sendParticles(
                        ParticleTypes.CLOUD,
                        player.getX(), player.getY() + 0.1, player.getZ(),
                        15,
                        0.3, 0.05, 0.3,
                        0.05
                );
            }
            return;
        }

        Vec3 dir = diff.normalize();

        // =========================================================
        // 速度計算（緩急）
        // =========================================================
        double speed = MAX_SPEED;

        // ---- 1. 発射直後は加速 ----
        if (state.ticks < ACCEL_TICKS) {
            double t = (double) state.ticks / ACCEL_TICKS;
            // easeOutQuad: 最初は速く、徐々に緩やかに
            double eased = 1.0D - (1.0D - t) * (1.0D - t);
            speed = MIN_SPEED + (MAX_SPEED - MIN_SPEED) * eased;
        }

        // ---- 2. 終盤は減速 ----
        if (dist < DECEL_DISTANCE) {
            double t = dist / DECEL_DISTANCE;  // 0..1
            // easeInQuad: 終盤になるほど強く減速
            double eased = t * t;
            double decelSpeed = MIN_SPEED + (MAX_SPEED - MIN_SPEED) * eased;
            speed = Math.min(speed, decelSpeed);
        }

        // ---- 速度適用 ----
        Vec3 velocity = dir.scale(speed);
        player.setDeltaMovement(velocity);
        player.hurtMarked = true;

        // 落下無効
        player.fallDistance = 0.0F;

        // =========================================================
        // 移動中の演出
        // =========================================================
        if (player.level() instanceof ServerLevel sl) {

            // 通過パーティクル（速度に応じて増やす）
            if (state.ticks % 1 == 0) {
                int count = (int) Math.max(1, speed * 1.5);
                sl.sendParticles(
                        ParticleTypes.END_ROD,
                        player.getX(), player.getY() + 1.0, player.getZ(),
                        count,
                        0.15, 0.15, 0.15,
                        0.0
                );
            }

            // 発射直後は派手に
            if (state.ticks < ACCEL_TICKS && state.ticks % 2 == 0) {
                sl.sendParticles(
                        ParticleTypes.CRIT,
                        player.getX(), player.getY() + 1.0, player.getZ(),
                        5,
                        0.3, 0.3, 0.3,
                        0.1
                );
            }

            // 風切り音（一定間隔）
            if (state.ticks % 8 == 0) {
                sl.playSound(null, player.getX(), player.getY(), player.getZ(),
                        SoundEvents.ELYTRA_FLYING,
                        SoundSource.PLAYERS,
                        0.4F,
                        1.5F + (float) (speed * 0.1));
            }
        }
    }

    /**
     * プレイヤー ↔ 着弾点 を結ぶロープを描画。
     *
     * パーティクルで線を引くことで「引っ張られてる感」を出す。
     */
    private static void drawRope(ServerPlayer player, GrappleState state) {

        if (state.target == null) return;
        if (!(player.level() instanceof ServerLevel sl)) return;

        Vec3 from = player.getEyePosition();
        Vec3 to = state.target;

        Vec3 diff = to.subtract(from);
        double dist = diff.length();
        if (dist < 0.01D) return;

        // ロープのパーティクル密度（距離に応じて増やすが上限あり）
        int steps = (int) Math.min(40, Math.max(8, dist * 1.2D));

        for (int i = 1; i < steps; i++) {
            double t = (double) i / steps;

            // ロープの「たわみ」を再現：少し下に垂れる
            // 中央付近が一番垂れるように sin カーブ
            double sag = Math.sin(t * Math.PI) * Math.min(0.4D, dist * 0.02D);

            double x = from.x + diff.x * t;
            double y = from.y + diff.y * t - sag;
            double z = from.z + diff.z * t;

            // メインのロープ（END_ROD）
            sl.sendParticles(
                    ParticleTypes.END_ROD,
                    x, y, z,
                    1,
                    0.02, 0.02, 0.02,
                    0.0
            );

            // たまに電気を走らせる
            if (i % 4 == 0 && state.ticks % 3 == 0) {
                sl.sendParticles(
                        ParticleTypes.ELECTRIC_SPARK,
                        x, y, z,
                        1,
                        0.02, 0.02, 0.02,
                        0.0
                );
            }
        }

        // 着弾点にアンカー演出
        if (state.ticks % 2 == 0) {
            sl.sendParticles(
                    ParticleTypes.CRIT,
                    to.x, to.y, to.z,
                    2,
                    0.1, 0.1, 0.1,
                    0.0
            );
        }
    }

    /**
     * フックを解除する。落下無効を3秒付与。
     */
    public static void release(Player player) {
        GrappleState state = STATES.get(player.getUUID());
        if (state == null) return;

        if (state.target == null) return;

        GrappleState newState = new GrappleState(null, 0);
        newState.fallProtect = FALL_PROTECT_TICKS;
        STATES.put(player.getUUID(), newState);

        player.fallDistance = 0.0F;

        if (player.level() instanceof ServerLevel sl) {
            sl.playSound(null, player.getX(), player.getY(), player.getZ(),
                    SoundEvents.CHAIN_BREAK, SoundSource.PLAYERS, 0.8F, 1.4F);
        }
    }

    // =========================================================
    // ユーティリティ
    // =========================================================

    private static boolean hasRing(Player player) {
        return CuriosApi.getCuriosInventory(player)
                .map(handler -> handler.findFirstCurio(
                        stack -> stack.is(Items.GRAPPLE_RING.get())
                ).isPresent())
                .orElse(false);
    }

    private static void spawnRopeParticles(ServerLevel sl, Vec3 from, Vec3 to) {
        int steps = 20;
        for (int i = 1; i < steps; i++) {
            double t = (double) i / steps;
            double x = from.x + (to.x - from.x) * t;
            double y = from.y + (to.y - from.y) * t;
            double z = from.z + (to.z - from.z) * t;

            sl.sendParticles(
                    ParticleTypes.CRIT,
                    x, y, z,
                    1,
                    0.0, 0.0, 0.0,
                    0.0
            );
        }
    }

    private static void spawnImpactParticles(ServerLevel sl, Vec3 point) {
        sl.sendParticles(
                ParticleTypes.CRIT,
                point.x, point.y, point.z,
                10,
                0.2, 0.2, 0.2,
                0.1
        );
        sl.sendParticles(
                ParticleTypes.END_ROD,
                point.x, point.y, point.z,
                5,
                0.1, 0.1, 0.1,
                0.02
        );
    }
}