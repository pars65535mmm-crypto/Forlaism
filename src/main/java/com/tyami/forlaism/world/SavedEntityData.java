package com.tyami.forlaism.world;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * マスターピースクロックで保存したEntityの永続データ。
 *
 * 各エントリは「保存時のNBTそのまま」を保持するので、
 * 復活させるとHP・装備・AI状態すべて元通りになる。
 */
public class SavedEntityData extends SavedData {

    private static final String DATA_NAME = "forlaism_masterpiece_clock_saved_entities";

    /** 保存エントリ1件分。 */
    public static final class Entry {
        public final UUID id;
        public final String displayName;
        public final String entityTypeId;
        public final CompoundTag entityNbt;
        public final long savedAt;

        public Entry(UUID id, String displayName, String entityTypeId,
                     CompoundTag entityNbt, long savedAt) {
            this.id = id;
            this.displayName = displayName;
            this.entityTypeId = entityTypeId;
            this.entityNbt = entityNbt;
            this.savedAt = savedAt;
        }
    }

    private final List<Entry> entries = new ArrayList<>();

    public SavedEntityData() {
    }

    // =========================================================
    // API
    // =========================================================

    public void add(Entry entry) {
        entries.add(entry);
        setDirty();
    }

    public List<Entry> getAll() {
        return List.copyOf(entries);
    }

    public Entry get(UUID id) {
        for (Entry e : entries) {
            if (e.id.equals(id)) return e;
        }
        return null;
    }

    public boolean remove(UUID id) {
        boolean removed = entries.removeIf(e -> e.id.equals(id));
        if (removed) setDirty();
        return removed;
    }

    public void clear() {
        entries.clear();
        setDirty();
    }

    public int size() {
        return entries.size();
    }

    // =========================================================
    // SavedData
    // =========================================================

    public static SavedEntityData load(CompoundTag tag) {
        SavedEntityData data = new SavedEntityData();
        ListTag list = tag.getList("Entries", Tag.TAG_COMPOUND);

        for (int i = 0; i < list.size(); i++) {
            CompoundTag t = list.getCompound(i);
            try {
                UUID id = t.getUUID("Id");
                String displayName = t.getString("DisplayName");
                String entityTypeId = t.getString("EntityType");
                CompoundTag entityNbt = t.getCompound("EntityNbt");
                long savedAt = t.getLong("SavedAt");

                data.entries.add(new Entry(id, displayName, entityTypeId, entityNbt, savedAt));
            } catch (Exception ignored) {
            }
        }

        return data;
    }

    @Override
    public CompoundTag save(CompoundTag tag) {
        ListTag list = new ListTag();

        for (Entry e : entries) {
            CompoundTag t = new CompoundTag();
            t.putUUID("Id", e.id);
            t.putString("DisplayName", e.displayName);
            t.putString("EntityType", e.entityTypeId);
            t.put("EntityNbt", e.entityNbt.copy());
            t.putLong("SavedAt", e.savedAt);
            list.add(t);
        }

        tag.put("Entries", list);
        return tag;
    }

    public static SavedEntityData get(MinecraftServer server) {
        return server.overworld()
                .getDataStorage()
                .computeIfAbsent(
                        SavedEntityData::load,
                        SavedEntityData::new,
                        DATA_NAME
                );
    }
}