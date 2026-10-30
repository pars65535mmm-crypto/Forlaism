package com.tyami.forlaism.item;

import com.tyami.forlaism.entity.NoxslratiaEntity;
import com.tyami.forlaism.registry.ModEntityTypes;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * 機械仕掛けのウツワ。
 *
 * 右クリックで発動:
 *   1. 周囲の水を凍らせる
 *   2. 雪を降らせる
 *   3. 時間を昼へ
 *   4. 天候を晴れへ
 *   5. パーティクル演出
 *   6. ノクスラティア召喚
 *
 * 使い捨て。
 */
public class MechanicalVesselItem extends Item {

    /** 周囲の水を凍らせる半径。 */
    public static final int FREEZE_RADIUS = 16;

    /** 召喚距離。 */
    public static final double SPAWN_DISTANCE = 4.0D;

    /** 演出総時間（tick）。3秒 = 60tick。 */
    public static final int EFFECT_DURATION = 60;

    public MechanicalVesselItem(Properties properties) {
        super(properties.stacksTo(1).fireResistant().rarity(Rarity.EPIC));
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        if (level.isClientSide) {
            return InteractionResultHolder.sidedSuccess(stack, true);
        }

        if (!(level instanceof ServerLevel sl)) {
            return InteractionResultHolder.pass(stack);
        }

        // =========================================================
        // 1. 周囲の水を凍らせる
        // =========================================================
        freezeNearbyWater(sl, player.blockPosition());

        // =========================================================
        // 2. 天候を「雪が降る」に（晴れに雪を重ねる）
        // =========================================================
        // 天候を一旦雨（雪）にして、あとで晴れに
        sl.setWeatherParameters(0, 6000, true, false);

        // =========================================================
        // 3. 時間を昼へ（徐々に）
        // =========================================================
        long currentTime = sl.getDayTime() % 24000L;
        long targetTime = 6000L; // 昼

        // 次の昼まで進める（今が昼より後なら翌日）
        long delta = targetTime - currentTime;
        if (delta < 0) delta += 24000L;

        // 時間を徐々に進める（20tickごとに 300ずつ）
        scheduleTimeTransition(sl, delta);

        // =========================================================
        // 4. パーティクル演出 + 召喚
        // =========================================================
        Vec3 origin = player.position();
        Vec3 look = player.getLookAngle().normalize();
        Vec3 spawnPos = origin.add(look.scale(SPAWN_DISTANCE));

        scheduleEffectAndSummon(sl, origin, spawnPos, player);

        // =========================================================
        // 消費
        // =========================================================
        if (!player.getAbilities().instabuild) {
            stack.shrink(1);
        }

        return InteractionResultHolder.sidedSuccess(stack, false);
    }

    // =========================================================
    // 水を凍らせる
    // =========================================================

    private static void freezeNearbyWater(ServerLevel level, BlockPos center) {
        for (int dx = -FREEZE_RADIUS; dx <= FREEZE_RADIUS; dx++) {
            for (int dy = -8; dy <= 8; dy++) {
                for (int dz = -FREEZE_RADIUS; dz <= FREEZE_RADIUS; dz++) {

                    BlockPos pos = center.offset(dx, dy, dz);

                    // 円形範囲内
                    if (dx * dx + dz * dz > FREEZE_RADIUS * FREEZE_RADIUS) continue;

                    FluidState fluid = level.getFluidState(pos);
                    if (fluid.getType() != Fluids.WATER && fluid.getType() != Fluids.FLOWING_WATER) continue;

                    if (!fluid.isSource()) continue;

                    BlockState state = level.getBlockState(pos);
                    if (!state.is(Blocks.WATER)) continue;

                    level.setBlockAndUpdate(pos, Blocks.FROSTED_ICE.defaultBlockState());
                }
            }
        }
    }

    // =========================================================
    // 時間を昼に
    // =========================================================

    private static void scheduleTimeTransition(ServerLevel level, long deltaTicks) {
        // 20 tick ごとに 300 tick 進める
        long totalSteps = deltaTicks / 300 + 1;
        for (int i = 0; i < totalSteps; i++) {
            final int step = i;
            com.tyami.forlaism.item.MasterpieceClockScheduler.schedule(
                    level, i * 20,
                    () -> {
                        // 時間を進める
                        level.setDayTime(level.getDayTime() + 300);
                    }
            );
        }
    }

    // =========================================================
    // 演出 + 召喚
    // =========================================================

    private static void scheduleEffectAndSummon(
            ServerLevel level,
            Vec3 origin,
            Vec3 spawnPos,
            Player player
    ) {
        // =========================================================
        // 0～60tick: パーティクル演出
        // =========================================================
        for (int t = 0; t < EFFECT_DURATION; t++) {
            final int tick = t;
            com.tyami.forlaism.item.MasterpieceClockScheduler.schedule(
                    level, tick,
                    () -> spawnEffectParticles(level, origin, spawnPos, tick)
            );
        }

        // =========================================================
        // 60tick後: 天候を晴れに + ノクスラティア召喚
        // =========================================================
        com.tyami.forlaism.item.MasterpieceClockScheduler.schedule(
                level, EFFECT_DURATION,
                () -> {
                    // 天候を晴れに
                    level.setWeatherParameters(12000, 0, false, false);

                    // 時間を確実に昼へ
                    level.setDayTime(6000);

                    // 召喚
                    summonNoxslratia(level, spawnPos, player);
                }
        );
    }

    // =========================================================
    // パーティクル演出
    // =========================================================

    private static void spawnEffectParticles(
            ServerLevel level,
            Vec3 origin,
            Vec3 spawnPos,
            int tick
    ) {
        // =========================================================
        // 周囲に雪を降らせる
        // =========================================================
        for (int i = 0; i < 8; i++) {
            double ox = (level.random.nextDouble() - 0.5) * 20.0;
            double oz = (level.random.nextDouble() - 0.5) * 20.0;
            double oy = 12.0 + level.random.nextDouble() * 5.0;

            level.sendParticles(
                    ParticleTypes.SNOWFLAKE,
                    origin.x + ox,
                    origin.y + oy,
                    origin.z + oz,
                    1,
                    0.1, -0.3, 0.1,
                    0.02
            );
        }

        // =========================================================
        // 召喚位置に渦を巻くオーラ
        // =========================================================
        double progress = tick / (double) EFFECT_DURATION;
        double radius = 4.0 * (1.0 - progress * 0.7);

        for (int i = 0; i < 6; i++) {
            double angle = (tick * 0.4) + (i * Math.PI * 2.0 / 6.0);
            double px = spawnPos.x + Math.cos(angle) * radius;
            double pz = spawnPos.z + Math.sin(angle) * radius;
            double py = spawnPos.y + progress * 3.0;

            level.sendParticles(
                    ParticleTypes.SCULK_SOUL,
                    px, py, pz,
                    1,
                    0, 0.02, 0,
                    0.01
            );
        }

        // =========================================================
        // 時々、閃光
        // =========================================================
        if (tick % 10 == 0) {
            level.sendParticles(
                    ParticleTypes.FLASH,
                    spawnPos.x, spawnPos.y + 1.0, spawnPos.z,
                    1, 0, 0, 0, 0
            );


        }

        // =========================================================
        // 雪原に変わるような、冷気の粒
        // =========================================================
        if (tick % 3 == 0) {
            for (int i = 0; i < 4; i++) {
                double ox = (level.random.nextDouble() - 0.5) * 15.0;
                double oz = (level.random.nextDouble() - 0.5) * 15.0;
                level.sendParticles(
                        ParticleTypes.END_ROD,
                        origin.x + ox,
                        origin.y + 0.1 + level.random.nextDouble() * 2.0,
                        origin.z + oz,
                        1,
                        0, 0.02, 0,
                        0.005
                );
            }
        }
    }

    // =========================================================
    // ノクスラティア召喚
    // =========================================================

    private static void summonNoxslratia(ServerLevel level, Vec3 pos, Player player) {

        NoxslratiaEntity boss = ModEntityTypes.NOXSLRATIA.get().create(level);
        if (boss == null) return;

        boss.moveTo(
                pos.x, pos.y, pos.z,
                player.getYRot() + 180.0F,
                0.0F
        );

        boss.finalizeSpawn(
                level,
                level.getCurrentDifficultyAt(player.blockPosition()),
                MobSpawnType.MOB_SUMMONED,
                null,
                null
        );

        level.addFreshEntity(boss);

        // =========================================================
        // 派手な召喚演出
        // =========================================================
        level.playSound(
                null,
                pos.x, pos.y, pos.z,
                SoundEvents.WARDEN_ROAR,
                SoundSource.HOSTILE,
                3.0F, 0.5F
        );

        level.playSound(
                null,
                pos.x, pos.y, pos.z,
                SoundEvents.END_PORTAL_SPAWN,
                SoundSource.HOSTILE,
                2.0F, 0.7F
        );

        level.playSound(
                null,
                pos.x, pos.y, pos.z,
                SoundEvents.LIGHTNING_BOLT_THUNDER,
                SoundSource.HOSTILE,
                2.0F, 0.6F
        );

        level.sendParticles(
                ParticleTypes.SCULK_SOUL,
                pos.x, pos.y + 1.0, pos.z,
                200, 2.5, 3.0, 2.5, 0.25
        );

        level.sendParticles(
                ParticleTypes.EXPLOSION_EMITTER,
                pos.x, pos.y + 1.0, pos.z,
                5, 0, 0, 0, 0
        );

        level.sendParticles(
                ParticleTypes.SNOWFLAKE,
                pos.x, pos.y + 1.0, pos.z,
                100, 3.0, 3.0, 3.0, 0.1
        );

        level.sendParticles(
                ParticleTypes.FLASH,
                pos.x, pos.y + 1.0, pos.z,
                8, 0, 0, 0, 0
        );


    }
}