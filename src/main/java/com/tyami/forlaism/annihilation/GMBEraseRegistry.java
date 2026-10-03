package com.tyami.forlaism.annihilation;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.player.Player;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.ArrayList;
import java.util.List;

/**
 * サーバー側 + クライアント側の消去UUID管理。
 */
public final class GMBEraseRegistry {

    /** クライアント側キャッシュ（パケットで同期される想定だが今はローカルでも更新）。 */
    private static final Set<UUID> CLIENT_CACHE = new HashSet<>();
    private static final Map<MinecraftServer, List<Tombstone>> TOMBSTONES = new WeakHashMap<>();

    private record Tombstone(String dimension, String type, double x, double y, double z,
                             long expiresAt) {}

    private GMBEraseRegistry() {
    }

    // =========================================================
    // サーバー側
    // =========================================================

    public static void markErased(MinecraftServer server, UUID uuid) {
        if (server != null && uuid != null) {
            GMBEraseData.get(server).markErased(uuid);
        }
    }

    public static boolean isErased(MinecraftServer server, UUID uuid) {
        if (server == null || uuid == null) return false;
        return GMBEraseData.get(server).isErased(uuid);
    }

    public static synchronized void markTombstone(MinecraftServer server, Entity entity, long now) {
        if (server == null || entity == null || entity instanceof ItemEntity) return;
        TOMBSTONES.computeIfAbsent(server, ignored -> new ArrayList<>()).add(new Tombstone(
                entity.level().dimension().location().toString(),
                entity.getType().builtInRegistryHolder().key().location().toString(),
                entity.getX(), entity.getY(), entity.getZ(), Long.MAX_VALUE));
    }

    /** UUIDが変わる再生成個体を、敵MOD名に依存せず同種・同地点で検知する。 */
    public static synchronized boolean isSuppressedReplacement(ServerLevel level, Entity entity, long now) {
        if (level == null || entity == null || entity instanceof ItemEntity) return false;
        if (isErased(level.getServer(), entity.getUUID())) return true;
        List<Tombstone> list = TOMBSTONES.get(level.getServer());
        if (list == null) return false;
        String dimension = level.dimension().location().toString();
        String type = entity.getType().builtInRegistryHolder().key().location().toString();
        boolean matched = false;
        list.removeIf(t -> t.expiresAt < now);
        for (Tombstone t : list) {
            if (t.dimension.equals(dimension) && t.type.equals(type)
                    && entity.distanceToSqr(t.x, t.y, t.z) <= 16.0D) {
                matched = true;
                break;
            }
        }
        return matched;
    }

    /** 攻撃元・ProjectileのOwnerを含めて、封印対象由来か判定する。 */
    public static boolean isSuppressedAttack(ServerLevel level, Entity source, long now) {
        if (source == null) return false;
        if (isSuppressedReplacement(level, source, now)) return true;
        return source instanceof Projectile projectile
                && projectile.getOwner() != null
                && isSuppressedReplacement(level, projectile.getOwner(), now);
    }

    /**
     * 攻撃元のEntity/Projectile/Forgeイベントを使わない独自攻撃用の最終防衛線。
     * GMB封印地点の近くにいるプレイヤーへのダメージを拒否する。
     */
    public static synchronized boolean isInSuppressionZone(ServerLevel level, Entity entity, long now) {
        if (!(entity instanceof Player) || level == null) return false;
        List<Tombstone> list = TOMBSTONES.get(level.getServer());
        if (list == null) return false;
        list.removeIf(t -> t.expiresAt < now);
        String dimension = level.dimension().location().toString();
        for (Tombstone t : list) {
            if (t.dimension.equals(dimension)
                    && entity.distanceToSqr(t.x, t.y, t.z) <= 1024.0D) {
                return true;
            }
        }
        return false;
    }

    // =========================================================
    // クライアント側
    // =========================================================

    public static void markErasedClient(UUID uuid) {
        if (uuid != null) CLIENT_CACHE.add(uuid);
    }

    public static boolean isErasedClient(UUID uuid) {
        return uuid != null && CLIENT_CACHE.contains(uuid);
    }

    public static void clearClient() {
        CLIENT_CACHE.clear();
    }
}
