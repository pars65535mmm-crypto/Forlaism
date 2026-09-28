package com.tyami.forlaism.erase;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * 抹消済みUUIDの永続化。
 *
 * ワールドに刻む。再起動しても消えない。
 */
public class EraseData extends SavedData {

    private static final String DATA_NAME = "forlaism_erase_data";

    /** 抹消済みUUID。 */
    private final Set<UUID> erased = new HashSet<>();

    public EraseData() {
    }

    // =========================================================
    // API
    // =========================================================

    public void markErased(UUID uuid) {
        if (uuid != null && erased.add(uuid)) {
            setDirty();
        }
    }

    public boolean isErased(UUID uuid) {
        return uuid != null && erased.contains(uuid);
    }

    public void clear(UUID uuid) {
        if (erased.remove(uuid)) {
            setDirty();
        }
    }

    public Set<UUID> snapshot() {
        return new HashSet<>(erased);
    }

    // =========================================================
    // SavedData
    // =========================================================

    public static EraseData load(CompoundTag tag) {
        EraseData data = new EraseData();
        ListTag list = tag.getList("Erased", Tag.TAG_INT_ARRAY);
        // UUIDを int[4] として保存
        ListTag uuidList = tag.getList("ErasedUUIDs", Tag.TAG_COMPOUND);
        for (int i = 0; i < uuidList.size(); i++) {
            CompoundTag t = uuidList.getCompound(i);
            try {
                UUID uuid = t.getUUID("UUID");
                data.erased.add(uuid);
            } catch (Exception ignored) {
            }
        }
        return data;
    }

    @Override
    public CompoundTag save(CompoundTag tag) {
        ListTag uuidList = new ListTag();
        for (UUID uuid : erased) {
            CompoundTag t = new CompoundTag();
            t.putUUID("UUID", uuid);
            uuidList.add(t);
        }
        tag.put("ErasedUUIDs", uuidList);
        return tag;
    }

    public static EraseData get(MinecraftServer server) {
        return server.overworld()
                .getDataStorage()
                .computeIfAbsent(
                        EraseData::load,
                        EraseData::new,
                        DATA_NAME
                );
    }
}