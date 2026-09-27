package com.tyami.forlaism.world;

import com.tyami.forlaism.damage.RinneDamageSource;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;

import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;

/**
 * レールガン着弾地点に生成される「虚空」。
 *
 * サーバー側:
 *   3秒間、半径45m以内の全Mobに毎tick 200の輪廻ダメージ。
 *
 * クライアント側:
 *   spawnClient() で見た目だけ登録し、VoidFieldRenderer が描画。
 */
@Mod.EventBusSubscriber(modid = "forlaism")
public final class VoidFieldManager {

    
    /** 最大持続tick（フェード計算用）。 */
    public static final int DURATION_MAX = 60;

    /** 虚空の毎tickダメージ（強化版）。 */
    public static final float VOID_DAMAGE_PER_TICK = 9_222_222.0F;

    private static final List<VoidField> ACTIVE = new ArrayList<>();

    private VoidFieldManager() {
    }

    // =========================================================
    // サーバー: スポーン
    // =========================================================

    public static void spawn(
            ServerLevel level,
            BlockPos center,
            int durationTicks,
            double radius,
            float damagePerTick,  // ← 引数は互換性のため残すが使わない
            UUID ownerUUID
    ) {
        ACTIVE.add(new VoidField(
                level,
                center.immutable(),
                durationTicks,
                radius,
                VOID_DAMAGE_PER_TICK,  // ← ここを固定値に
                ownerUUID
        ));

        // 生成演出
        level.playSound(null, center,
                net.minecraft.sounds.SoundEvents.ENDER_DRAGON_GROWL,
                net.minecraft.sounds.SoundSource.PLAYERS, 2.0F, 0.5F);

        level.sendParticles(
                ParticleTypes.SCULK_SOUL,
                center.getX() + 0.5, center.getY() + 1.0, center.getZ() + 0.5,
                100, 2.0, 2.0, 2.0, 0.2
        );
    }

    // =========================================================
    // サーバー: 毎tick処理
    // =========================================================

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        if (ACTIVE.isEmpty()) return;

        Iterator<VoidField> it = ACTIVE.iterator();

        while (it.hasNext()) {
            VoidField f = it.next();

            // クライアント用エントリは server tick では触らない
            if (f.level == null) continue;

            f.remainingTicks--;

            if (f.remainingTicks <= 0) {
                it.remove();
                continue;
            }

            tickField(f);
        }
    }

    private static void tickField(VoidField f) {

        // クライアント側（level == null）はダメージ処理しない
        if (f.level == null) return;

        double radiusSqr = f.radius * f.radius;
        AABB area = new AABB(f.center).inflate(f.radius);

        // =========================================================
        // 範囲内の全Mobへ輪廻ダメージ
        // =========================================================
        var targets = f.level.getEntitiesOfClass(
                LivingEntity.class, area,
                e -> e.isAlive() && !e.isSpectator()
        );

        for (LivingEntity target : targets) {
            if (target.blockPosition().distSqr(f.center) > radiusSqr) continue;

            // 自分自身（発射者）は除外
            if (f.ownerUUID != null && target.getUUID().equals(f.ownerUUID)) continue;

            target.invulnerableTime = 0;
            target.hurtTime = 0;
            target.hurt(RinneDamageSource.of(f.level, null), f.damagePerTick);
        }

        // =========================================================
        // 演出
        // =========================================================
        if (f.level.getGameTime() % 2 == 0) {
            f.level.sendParticles(
                    ParticleTypes.PORTAL,
                    f.center.getX() + 0.5,
                    f.center.getY() + 1.0,
                    f.center.getZ() + 0.5,
                    20,
                    f.radius * 0.5, 2.0, f.radius * 0.5,
                    0.3
            );

            f.level.sendParticles(
                    ParticleTypes.SCULK_SOUL,
                    f.center.getX() + 0.5,
                    f.center.getY() + 1.0,
                    f.center.getZ() + 0.5,
                    10,
                    f.radius * 0.3, 1.0, f.radius * 0.3,
                    0.1
            );
        }
    }

    // =========================================================
    // クライアント: スポーン（見た目のみ）
    // =========================================================

    /**
     * クライアント側で虚空を表示するための登録。
     * ダメージ処理はしない（サーバーのみ）。
     */
    public static void spawnClient(BlockPos center, int durationTicks, double radius) {
        ACTIVE.add(new VoidField(
                null,  // クライアントはServerLevel持たない
                center.immutable(),
                durationTicks,
                radius,
                0.0F,
                null
        ));
    }

    /**
     * クライアント側の寿命更新。
     * VoidFieldRenderer から毎フレーム呼ぶ。
     */
    public static void clientTick() {
        Iterator<VoidField> it = ACTIVE.iterator();
        while (it.hasNext()) {
            VoidField f = it.next();
            if (f.level != null) continue;  // サーバー側は別管理
            f.remainingTicks--;
            if (f.remainingTicks <= 0) it.remove();
        }
    }

    // =========================================================
    // 描画用スナップショット
    // =========================================================

    /**
     * アクティブな虚空のスナップショット（描画用）。
     */
    public static List<VoidFieldView> snapshot() {
        List<VoidFieldView> out = new ArrayList<>(ACTIVE.size());
        for (VoidField f : ACTIVE) {
            out.add(new VoidFieldView(
                    f.center.getX() + 0.5,
                    f.center.getY() + 1.0,
                    f.center.getZ() + 0.5,
                    f.radius,
                    f.remainingTicks,
                    f.durationTicks
            ));
        }
        return out;
    }

    /** 描画用のビュー。 */
    public record VoidFieldView(
            double x, double y, double z,
            double radius,
            int remainingTicks,
            int durationTicks
    ) {
    }

    // =========================================================
    // 内部クラス
    // =========================================================

    private static final class VoidField {
        /** サーバー側のみ non-null。クライアント側は null。 */
        @Nullable
        final ServerLevel level;
        final BlockPos center;
        final double radius;
        final float damagePerTick;
        @Nullable
        final UUID ownerUUID;
        final int durationTicks;
        int remainingTicks;

        VoidField(@Nullable ServerLevel level, BlockPos center,
                  int durationTicks, double radius,
                  float damagePerTick, @Nullable UUID ownerUUID) {
            this.level = level;
            this.center = center;
            this.durationTicks = durationTicks;
            this.radius = radius;
            this.damagePerTick = damagePerTick;
            this.ownerUUID = ownerUUID;
            this.remainingTicks = durationTicks;
        }
    }
}