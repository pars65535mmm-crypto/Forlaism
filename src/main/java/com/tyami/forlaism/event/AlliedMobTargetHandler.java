package com.tyami.forlaism.event;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.MobType;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;

import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.List;

/**
 * 味方化されたMobに「敵対Mobを狙う」動きを与える。
 *
 * 既存のAIに Goal を追加するのは危険なので、
 * 毎tick「ターゲットがnull または 味方/プレイヤーなら、
 * 近くの敵対Mobを setTarget する」方式で制御する。
 */
@Mod.EventBusSubscriber(modid = "forlaism")
public final class AlliedMobTargetHandler {

    /** 索敵範囲。 */
    private static final double SEARCH_RANGE = 16.0D;

    private AlliedMobTargetHandler() {
    }

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {

        if (event.phase != TickEvent.Phase.END) return;

        var server = event.getServer();
        if (server == null) return;

        // 5tick毎にチェック（軽量化）
        if (server.getTickCount() % 5 != 0) return;

        // 味方化Mobを全レベルから探す
        for (var level : server.getAllLevels()) {

            List<Mob> alliedMobs = level.getEntitiesOfClass(
                    Mob.class,
                    // 広範囲だが、味方化Mobは通常少数なので実用上問題なし
                    // （もっと厳密にやりたいなら味方化Mobの座標リストを保持する）
                    new AABB(-30000000, -64, -30000000, 30000000, 320, 30000000),
                    m -> m.isAlive() && AlliedMobHandler.isAllied(m)
            );

            for (Mob ally : alliedMobs) {
                tickAlliedMob(ally);
            }
        }
    }

    private static void tickAlliedMob(Mob ally) {

        LivingEntity currentTarget = ally.getTarget();

        // 既に有効な敵（プレイヤー以外の敵対Mob）を狙ってるなら維持
        if (currentTarget != null
                && currentTarget.isAlive()
                && !(currentTarget instanceof Player)
                && !(currentTarget instanceof Mob m2 && AlliedMobHandler.isAllied(m2))) {
            return;
        }

        // 新しい敵対Mobを探す
        AABB area = ally.getBoundingBox().inflate(SEARCH_RANGE);

        List<Mob> candidates = ally.level().getEntitiesOfClass(
                Mob.class,
                area,
                m -> m != ally
                        && m.isAlive()
                        && !AlliedMobHandler.isAllied(m)
                        && isHostile(m)
        );

        // 一番近い敵をターゲット
        Mob nearest = null;
        double nearestDist = Double.MAX_VALUE;

        for (Mob candidate : candidates) {
            double dist = ally.distanceToSqr(candidate);
            if (dist < nearestDist) {
                nearestDist = dist;
                nearest = candidate;
            }
        }

        if (nearest != null) {
            ally.setTarget(nearest);
        }
    }

    /**
     * 敵対Mob判定。
     */
    private static boolean isHostile(Mob mob) {
        if (mob.getType().getCategory() == MobCategory.MONSTER) {
            return true;
        }
        if (mob.getMobType() == MobType.UNDEAD) {
            return true;
        }
        if (mob instanceof Monster) {
            return true;
        }
        return false;
    }
}