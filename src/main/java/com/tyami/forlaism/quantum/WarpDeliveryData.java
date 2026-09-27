package com.tyami.forlaism.quantum;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Warp送信機の遅延配送データ。
 *
 * 送信元 → 送信先へ「5秒後に届く」を実現する。
 *
 * ・チャンクが未ロードの場合、ロードされるまで永遠に待つ（案B）
 * ・到着時に ItemEntity を出現させる
 */
public class WarpDeliveryData extends SavedData {

    private static final String DATA_NAME = "forlaism_warp_delivery";

    /** 保留中の配送リスト。 */
    private final List<PendingDelivery> pending = new ArrayList<>();

    public WarpDeliveryData() {
    }

    // =========================================================
    // スケジュール登録
    // =========================================================

    public static void schedule(
            MinecraftServer server,
            BlockPos pos,
            ResourceKey<Level> dim,
            ItemStack stack,
            long arriveTick
    ) {
        WarpDeliveryData data = get(server);
        data.pending.add(new PendingDelivery(
                pos.immutable(),
                dim,
                stack.copy(),
                arriveTick
        ));
        data.setDirty();
    }

    // =========================================================
    // 毎tick処理
    // =========================================================

    public static void tick(MinecraftServer server) {
        WarpDeliveryData data = get(server);

        if (data.pending.isEmpty()) return;

        long now = server.overworld().getGameTime();

        Iterator<PendingDelivery> it = data.pending.iterator();

        while (it.hasNext()) {
            PendingDelivery d = it.next();

            if (now < d.arriveTick) continue;

            ServerLevel targetLevel = server.getLevel(d.dim);
            if (targetLevel == null) {
                it.remove();
                data.setDirty();
                continue;
            }

            // チャンクロードチェック（未ロードなら次tickで再挑戦）
            ChunkPos cp = new ChunkPos(d.pos);
            if (!targetLevel.getChunkSource().hasChunk(cp.x, cp.z)) {
                continue;
            }

            // 到着！
            ItemEntity item = new ItemEntity(
                    targetLevel,
                    d.pos.getX() + 0.5,
                    d.pos.getY() + 1.0,
                    d.pos.getZ() + 0.5,
                    d.stack
            );
            item.setPickUpDelay(0);
            targetLevel.addFreshEntity(item);

            // パーティクル演出
            targetLevel.sendParticles(
                    net.minecraft.core.particles.ParticleTypes.PORTAL,
                    d.pos.getX() + 0.5,
                    d.pos.getY() + 1.0,
                    d.pos.getZ() + 0.5,
                    20,
                    0.3, 0.3, 0.3,
                    0.1
            );

            it.remove();
            data.setDirty();
        }
    }

    // =========================================================
    // SavedData
    // =========================================================

    public static WarpDeliveryData load(CompoundTag tag) {
        WarpDeliveryData data = new WarpDeliveryData();
        ListTag list = tag.getList("Pending", Tag.TAG_COMPOUND);

        for (int i = 0; i < list.size(); i++) {
            CompoundTag t = list.getCompound(i);
            try {
                BlockPos pos = BlockPos.of(t.getLong("Pos"));
                ResourceKey<Level> dim = ResourceKey.create(
                        net.minecraft.core.registries.Registries.DIMENSION,
                        new ResourceLocation(t.getString("Dim"))
                );
                ItemStack stack = ItemStack.of(t.getCompound("Stack"));
                long arriveTick = t.getLong("ArriveTick");

                if (!stack.isEmpty()) {
                    data.pending.add(new PendingDelivery(pos, dim, stack, arriveTick));
                }
            } catch (Exception ignored) {
            }
        }
        return data;
    }

    @Override
    public CompoundTag save(CompoundTag tag) {
        ListTag list = new ListTag();

        for (PendingDelivery d : pending) {
            CompoundTag t = new CompoundTag();
            t.putLong("Pos", d.pos.asLong());
            t.putString("Dim", d.dim.location().toString());
            t.put("Stack", d.stack.save(new CompoundTag()));
            t.putLong("ArriveTick", d.arriveTick);
            list.add(t);
        }

        tag.put("Pending", list);
        return tag;
    }

    public static WarpDeliveryData get(MinecraftServer server) {
        return server.overworld()
                .getDataStorage()
                .computeIfAbsent(
                        WarpDeliveryData::load,
                        WarpDeliveryData::new,
                        DATA_NAME
                );
    }

    // =========================================================
    // 内部クラス
    // =========================================================

    private static class PendingDelivery {
        final BlockPos pos;
        final ResourceKey<Level> dim;
        final ItemStack stack;
        final long arriveTick;

        PendingDelivery(BlockPos pos, ResourceKey<Level> dim, ItemStack stack, long arriveTick) {
            this.pos = pos;
            this.dim = dim;
            this.stack = stack;
            this.arriveTick = arriveTick;
        }
    }
}