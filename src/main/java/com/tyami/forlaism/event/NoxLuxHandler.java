package com.tyami.forlaism.event;

import com.tyami.forlaism.damage.MoonlightDamageSource;
import com.tyami.forlaism.item.NoxLuxItem;
import com.tyami.forlaism.registry.Items;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;

import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.AttackEntityEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * ノクスルクスの能力〈月夜〉を処理するハンドラ。
 *
 * 【状態遷移】
 *   1. Shift押した瞬間 → 見てるEntityを記録
 *   2. 0.5秒間Shift押し続け → 記録Entityの背後にTP
 *   3. TP後1秒以内に攻撃 → 追撃〈月夜〉発動
 */
@Mod.EventBusSubscriber(modid = "forlaism")
public final class NoxLuxHandler {

    /** プレイヤーごとの状態。 */
    private static final Map<UUID, State> STATES = new HashMap<>();

    private NoxLuxHandler() {
    }

    /**
     * ノクスルクス使用中のプレイヤーの状態。
     */
    private static final class State {

        /** 記録したEntityのUUID。 */
        UUID recordedEntityId = null;

        /** Shiftを押し続けているtick数。 */
        int shiftHoldTicks = 0;

        /** 追撃有効期限（ゲームtick）。 */
        long pursuitExpireTick = 0L;

        /** 前tickでShiftが押されていたか。 */
        boolean wasShiftDown = false;
    }

    // =========================================================
    // 毎tick処理
    // =========================================================

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {

        if (event.phase != TickEvent.Phase.END) {
            return;
        }

        if (!(event.player instanceof ServerPlayer player)) {
            return;
        }

        // メインハンドにノクスルクスを持っているか
        ItemStack main = player.getMainHandItem();
        boolean hasNoxLux = main.is(Items.NOXLUX.get());

        if (!hasNoxLux) {
            STATES.remove(player.getUUID());
            return;
        }

        State state = STATES.computeIfAbsent(player.getUUID(), k -> new State());

        boolean shiftDown = player.isShiftKeyDown();

        // =========================================================
        // 1. Shift押した瞬間: 見てるEntityを記録
        // =========================================================
        if (shiftDown && !state.wasShiftDown) {

            LivingEntity lookTarget = findLookTarget(player, 64.0D);

            if (lookTarget != null && lookTarget != player) {
                state.recordedEntityId = lookTarget.getUUID();
                state.shiftHoldTicks = 0;

            } else {
                state.recordedEntityId = null;
            }
        }

        // =========================================================
        // 2. Shift押し続け: 0.5秒でTP
        // =========================================================
        if (shiftDown && state.recordedEntityId != null) {

            state.shiftHoldTicks++;

            // 発動
            if (state.shiftHoldTicks >= NoxLuxItem.TP_CHARGE_TICKS) {

                performTeleport(player, state);
                state.shiftHoldTicks = 0;
            }
        }

        // Shift離したらカウントリセット（記録は残す）
        if (!shiftDown) {
            state.shiftHoldTicks = 0;
        }

        state.wasShiftDown = shiftDown;
    }

    // =========================================================
    // TP処理
    // =========================================================

    private static void performTeleport(ServerPlayer player, State state) {

        if (!(player.level() instanceof ServerLevel level)) {
            return;
        }

        Entity target = level.getEntity(state.recordedEntityId);

        if (!(target instanceof LivingEntity living) || !living.isAlive()) {
            state.recordedEntityId = null;
            return;
        }

        // =========================================================
        // ターゲットの背後を計算
        // =========================================================
        Vec3 look = living.getLookAngle().normalize();
        Vec3 behind = living.position().subtract(look.scale(2.0));

        // 安全チェック（背後に空間があるか）
        Vec3 destination = behind;

        // =========================================================
        // TP前の演出
        // =========================================================
        level.sendParticles(
                ParticleTypes.PORTAL,
                player.getX(), player.getY() + 1.0, player.getZ(),
                40,
                0.5, 1.0, 0.5,
                0.3
        );

        level.playSound(
                null,
                player.getX(), player.getY(), player.getZ(),
                SoundEvents.ENDERMAN_TELEPORT,
                SoundSource.PLAYERS,
                1.0F, 1.2F
        );

        // =========================================================
        // TP
        // =========================================================
        player.teleportTo(
                destination.x,
                destination.y,
                destination.z
        );

        // =========================================================
        // 視線をターゲットに向ける
        // =========================================================
        lookAt(player, living);

        // =========================================================
        // TP後の演出
        // =========================================================
        level.sendParticles(
                ParticleTypes.PORTAL,
                destination.x, destination.y + 1.0, destination.z,
                40,
                0.5, 1.0, 0.5,
                0.3
        );

        level.sendParticles(
                ParticleTypes.SOUL_FIRE_FLAME,
                destination.x, destination.y + 1.0, destination.z,
                15,
                0.4, 0.6, 0.4,
                0.05
        );

        level.playSound(
                null,
                destination.x, destination.y, destination.z,
                SoundEvents.ENDERMAN_TELEPORT,
                SoundSource.PLAYERS,
                1.0F, 1.5F
        );

        // =========================================================
        // 追撃ウィンドウ開始
        // =========================================================
        state.pursuitExpireTick = level.getGameTime() + NoxLuxItem.PURSUIT_WINDOW_TICKS;


    }

    /**
     * プレイヤーの視線をターゲットに向ける。
     */
    private static void lookAt(ServerPlayer player, LivingEntity target) {

        double dx = target.getX() - player.getX();
        double dy = target.getEyeY() - player.getEyeY();
        double dz = target.getZ() - player.getZ();

        double horizontal = Math.sqrt(dx * dx + dz * dz);

        float yaw = (float) (Math.atan2(dz, dx) * (180.0 / Math.PI)) - 90.0F;
        float pitch = (float) -(Math.atan2(dy, horizontal) * (180.0 / Math.PI));

        player.setYRot(yaw);
        player.setYHeadRot(yaw);
        player.setXRot(pitch);

        // クライアントに強制同期
        player.connection.teleport(
                player.getX(), player.getY(), player.getZ(),
                yaw, pitch
        );
    }

    /**
     * 視線先のEntityを取得。
     */
    private static LivingEntity findLookTarget(ServerPlayer player, double maxDist) {

        Vec3 eye = player.getEyePosition();
        Vec3 look = player.getLookAngle();
        Vec3 end = eye.add(look.scale(maxDist));

        AABB box = player.getBoundingBox()
                .expandTowards(look.scale(maxDist))
                .inflate(1.0);

        EntityHitResult hit = ProjectileUtil.getEntityHitResult(
                player.level(),
                player,
                eye,
                end,
                box,
                e -> e instanceof LivingEntity && e.isAlive() && e != player
        );

        if (hit != null && hit.getEntity() instanceof LivingEntity living) {
            return living;
        }

        return null;
    }

    // =========================================================
    // 攻撃: 追撃〈月夜〉
    // =========================================================

    @SubscribeEvent
    public static void onAttack(AttackEntityEvent event) {

        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }

        if (!(event.getTarget() instanceof LivingEntity target)) {
            return;
        }

        // メインハンドにノクスルクス
        if (!player.getMainHandItem().is(Items.NOXLUX.get())) {
            return;
        }

        State state = STATES.get(player.getUUID());
        if (state == null) {
            return;
        }

        // 追撃ウィンドウ内か
        if (!(player.level() instanceof ServerLevel level)) {
            return;
        }

        if (level.getGameTime() > state.pursuitExpireTick) {
            return;
        }

        // 記録したEntityと一致するか
        if (state.recordedEntityId == null
                || !state.recordedEntityId.equals(target.getUUID())) {
            return;
        }

        // =========================================================
        // 追撃〈月夜〉発動！
        // =========================================================
        triggerMoonlight(level, player, target);

        // 追撃ウィンドウ終了
        state.pursuitExpireTick = 0L;
        state.recordedEntityId = null;
    }

    /**
     * 追撃〈月夜〉。
     * 月光ダメージ 3 × 3回 + 硬直。
     */
    private static void triggerMoonlight(
            ServerLevel level,
            ServerPlayer player,
            LivingEntity target
    ) {

        // =========================================================
        // 月光ダメージ 3 × 3回
        // =========================================================
        for (int i = 0; i < NoxLuxItem.MOONLIGHT_HITS; i++) {

            target.invulnerableTime = 0;
            target.hurtTime = 0;

            target.hurt(
                    MoonlightDamageSource.of(level, player),
                    NoxLuxItem.MOONLIGHT_DAMAGE
            );

            // 月の粒
            level.sendParticles(
                    ParticleTypes.END_ROD,
                    target.getX(),
                    target.getY() + target.getBbHeight() * 0.5,
                    target.getZ(),
                    8,
                    0.4, 0.6, 0.4,
                    0.05
            );
        }

        // =========================================================
        // 硬直: 0.3秒間動けなくする
        // =========================================================
        applyStun(target, NoxLuxItem.MOONLIGHT_STUN_TICKS);

        // =========================================================
        // 演出
        // =========================================================
        level.sendParticles(
                ParticleTypes.FLASH,
                target.getX(),
                target.getY() + target.getBbHeight() * 0.5,
                target.getZ(),
                1,
                0, 0, 0, 0
        );

        level.sendParticles(
                ParticleTypes.SOUL_FIRE_FLAME,
                target.getX(),
                target.getY() + target.getBbHeight() * 0.5,
                target.getZ(),
                25,
                0.6, 0.8, 0.6,
                0.1
        );

        level.playSound(
                null,
                target.getX(), target.getY(), target.getZ(),
                SoundEvents.WARDEN_SONIC_BOOM,
                SoundSource.PLAYERS,
                1.2F, 1.8F
        );

        level.playSound(
                null,
                target.getX(), target.getY(), target.getZ(),
                SoundEvents.AMETHYST_BLOCK_CHIME,
                SoundSource.PLAYERS,
                1.5F, 0.8F
        );


    }

    /**
     * 硬直を付与する。
     */
    private static void applyStun(LivingEntity target, int ticks) {

        // 移動停止
        target.setDeltaMovement(0, 0, 0);
        target.hurtMarked = true;

        // MobならAI停止
        if (target instanceof Mob mob) {
            mob.setNoAi(true);
        }

        // 硬直解除用の予約
        // → 毎tickチェックするのではなく、専用のTick処理で解除
        STUN_MAP.put(target.getUUID(), target.level().getGameTime() + ticks);
    }

    /** 硬直中のEntityと解除tick。 */
    private static final Map<UUID, Long> STUN_MAP = new HashMap<>();

    /**
     * 硬直の解除チェック。
     */
    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {

        if (event.phase != TickEvent.Phase.END) {
            return;
        }

        var server = event.getServer();
        if (server == null) {
            return;
        }

        long now = server.overworld().getGameTime();

        STUN_MAP.entrySet().removeIf(entry -> {

            if (entry.getValue() > now) {
                return false;
            }

            // 解除
            UUID uuid = entry.getKey();

            for (ServerLevel level : server.getAllLevels()) {
                Entity e = level.getEntity(uuid);
                if (e instanceof LivingEntity living) {
                    if (living instanceof Mob mob) {
                        mob.setNoAi(false);
                    }
                    living.setDeltaMovement(0, 0, 0);
                    break;
                }
            }

            return true;
        });
    }
}