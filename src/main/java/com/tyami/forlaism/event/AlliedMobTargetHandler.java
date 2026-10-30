package com.tyami.forlaism.event;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.MobType;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;

import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.entity.EntityLeaveLevelEvent;
import net.minecraftforge.event.server.ServerStoppingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.WeakHashMap;

/**
 * 味方化されたMobに「敵対Mobを狙う」動きを与える。
 *
 * 既存のAIに Goal を追加するのは危険なので、
 * 定期的に「ターゲットがnull または 味方/プレイヤーなら、
 * 近くの敵対Mobを setTarget する」方式で制御する。
 *
 * 【軽量化】
 * 以前は全レベル・全チャンクを巨大なAABBでフルスキャンしていたため、
 * サーバーTickの60%以上を消費する深刻な負荷源になっていた。
 * 味方化されたMobのみを弱参照Setで動的に追跡し、
 * 味方Mobが存在しない時は即座にスキップ、
 * 存在する時も各Mobの周囲16ブロックのみを局所走査する方式に改善。
 */
@Mod.EventBusSubscriber(modid = "forlaism")
public final class AlliedMobTargetHandler {

    /** 索敵範囲。 */
    private static final double SEARCH_RANGE = 16.0D;

    /**
     * 現在ワールドに存在・ロードされている味方化Mobの追跡リスト。
     * 弱参照 Set を使用してメモリリークを完全に防止。
     */
    private static final Set<Mob> TRACKED_ALLIES =
            Collections.synchronizedSet(Collections.newSetFromMap(new WeakHashMap<>()));

    private AlliedMobTargetHandler() {
    }

    /**
     * 味方化Mobを追跡対象に登録する。
     */
    public static void trackAlliedMob(Mob mob) {
        if (mob != null && !mob.level().isClientSide) {
            TRACKED_ALLIES.add(mob);
        }
    }

    /**
     * 味方化Mobを追跡対象から解除する。
     */
    public static void untrackAlliedMob(Mob mob) {
        if (mob != null) {
            TRACKED_ALLIES.remove(mob);
        }
    }

    /**
     * エンティティがワールドに参加（スポーンまたはチャンクロード）した時、
     * 味方化タグを持っていれば追跡リストに加える。
     */
    @SubscribeEvent
    public static void onEntityJoinLevel(EntityJoinLevelEvent event) {
        if (event.getLevel().isClientSide) return;

        if (event.getEntity() instanceof Mob mob && AlliedMobHandler.isAllied(mob)) {
            trackAlliedMob(mob);
        }
    }

    /**
     * エンティティがワールドから離脱（アンロードまたは消滅）した時、追跡リストから解除。
     */
    @SubscribeEvent
    public static void onEntityLeaveLevel(EntityLeaveLevelEvent event) {
        if (event.getEntity() instanceof Mob mob) {
            untrackAlliedMob(mob);
        }
    }

    /**
     * サーバー停止時に追跡リストをクリア。
     */
    @SubscribeEvent
    public static void onServerStopping(ServerStoppingEvent event) {
        TRACKED_ALLIES.clear();
    }

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {

        if (event.phase != TickEvent.Phase.END) return;

        // 味方化Mobが1体もいなければ完全ノーコストで即リターン（負荷 0%）
        if (TRACKED_ALLIES.isEmpty()) return;

        var server = event.getServer();
        if (server == null) return;

        // 5tick毎にチェック
        if (server.getTickCount() % 5 != 0) return;

        // 追跡リストを同期走査し、無効化されたMobをクリーンアップしながら処理
        synchronized (TRACKED_ALLIES) {
            Iterator<Mob> iterator = TRACKED_ALLIES.iterator();
            while (iterator.hasNext()) {
                Mob ally = iterator.next();

                // 死亡、消滅、または味方化解除されていたらリストから削除
                if (ally == null || !ally.isAlive() || ally.isRemoved() || !AlliedMobHandler.isAllied(ally)) {
                    iterator.remove();
                    continue;
                }

                // 味方Mobの個別AIティック
                tickAlliedMob(ally);
            }
        }
    }

    private static void tickAlliedMob(Mob ally) {

        LivingEntity currentTarget = ally.getTarget();

        // 既に有効な敵（プレイヤー以外の敵対Mob）を狙ってるなら維持
        if (currentTarget != null
                && currentTarget.isAlive()
                && !currentTarget.isRemoved()
                && !(currentTarget instanceof Player)
                && !(currentTarget instanceof Mob m2 && AlliedMobHandler.isAllied(m2))) {
            return;
        }

        // 新しい敵対Mobを探す（味方Mobの周囲 16ブロック のみ局所探索）
        AABB area = ally.getBoundingBox().inflate(SEARCH_RANGE);

        List<Mob> candidates = ally.level().getEntitiesOfClass(
                Mob.class,
                area,
                m -> m != ally
                        && m.isAlive()
                        && !m.isRemoved()
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
