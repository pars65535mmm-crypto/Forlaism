package com.tyami.forlaism.world;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * ボスの永続データ。
 *
 * ワールド再起動後も残る。
 */
public class BossData extends SavedData {

    private static final String DATA_NAME = "forlaism_bosses";

    /** ボスUUID → BossInstance。 */
    public final Map<UUID, BossInstance> bosses = new HashMap<>();

    public BossData() {
    }

    public static BossData load(CompoundTag tag) {
        BossData data = new BossData();
        ListTag list = tag.getList("Bosses", Tag.TAG_COMPOUND);
        for (int i = 0; i < list.size(); i++) {
            try {
                BossInstance inst = BossInstance.load(list.getCompound(i));
                data.bosses.put(inst.id, inst);
            } catch (Exception ignored) {
            }
        }
        return data;
    }

    @Override
    public CompoundTag save(CompoundTag tag) {
        ListTag list = new ListTag();
        for (BossInstance inst : bosses.values()) {
            list.add(inst.save());
        }
        tag.put("Bosses", list);
        return tag;
    }

    public static BossData get(MinecraftServer server) {
        return server.overworld()
                .getDataStorage()
                .computeIfAbsent(
                        BossData::load,
                        BossData::new,
                        DATA_NAME
                );
    }
}