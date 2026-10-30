package com.tyami.forlaism.event;

import com.tyami.forlaism.world.BloodMoonManager;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.levelgen.Heightmap;

import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.List;

/**
 * ブラッドムーン中、プレイヤー周囲に追加モブを湧かせる。
 *
 * バニラのスポーン上限を超えて追加湧きさせる仕組み。
 * 1秒に1回、各プレイヤー周囲16〜32ブロックに1〜2体スポーン。
 */
@Mod.EventBusSubscriber(modid = "forlaism")
public final class BloodMoonSpawnBooster {

    private BloodMoonSpawnBooster() {
    }

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;

        var server = event.getServer();
        if (server == null) return;
        if (server.getTickCount() % 20 != 0) return; // 1秒ごと

        for (ServerLevel level : server.getAllLevels()) {
            if (!BloodMoonManager.isActive(level)) continue;
            if (!BloodMoonManager.isNight(level)) continue;

            for (ServerPlayer player : level.players()) {
                trySpawnAround(level, player);
            }
        }
    }

    private static void trySpawnAround(ServerLevel level, ServerPlayer player) {
        RandomSource random = level.getRandom();

        // 1〜2体
        int count = 1 + random.nextInt(2);

        for (int i = 0; i < count; i++) {
            double angle = random.nextDouble() * Math.PI * 2;
            double radius = 16 + random.nextDouble() * 16;

            double x = player.getX() + Math.cos(angle) * radius;
            double z = player.getZ() + Math.sin(angle) * radius;

            BlockPos surface = level.getHeightmapPos(
                    Heightmap.Types.WORLD_SURFACE, BlockPos.containing(x, player.getY(), z)
            );

            // 明るすぎる場所では湧かない
            if (level.getBrightness(LightLayer.BLOCK, surface) > 7) continue;

            // モブ選択（簡易）
            EntityType<? extends Mob> type = pickMobType(random);
            Mob mob = type.create(level);
            if (mob == null) continue;

            mob.moveTo(surface.getX() + 0.5, surface.getY(), surface.getZ() + 0.5,
                    random.nextFloat() * 360F, 0F);
            mob.finalizeSpawn(level,
                    level.getCurrentDifficultyAt(surface),
                    MobSpawnType.NATURAL, null, null);

            // ブラッドムーン強化
            BloodMoonHandler.applyBloodMoonBuffs(mob);

            level.addFreshEntity(mob);
        }
    }

    private static EntityType<? extends Mob> pickMobType(RandomSource random) {
        return switch (random.nextInt(6)) {
            case 0 -> EntityType.ZOMBIE;
            case 1 -> EntityType.SKELETON;
            case 2 -> EntityType.SPIDER;
            case 3 -> EntityType.CREEPER;
            case 4 -> EntityType.WITCH;
            default -> EntityType.HUSK;
        };
    }
}