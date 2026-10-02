package com.tyami.forlaism.annihilation;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * GMBで消去したUUIDの永続化。
 * ワールドに刻む。再起動しても復活しない。
 */
public class GMBEraseData extends SavedData {

    private static final String DATA_NAME = "forlaism_gmb_erase";

    private final Set<UUID> erased = new HashSet<>();

    public GMBEraseData() {
    }

    public void markErased(UUID uuid) {
        if (uuid != null && erased.add(uuid)) {
            setDirty();
        }
    }

    public boolean isErased(UUID uuid) {
        return uuid != null && erased.contains(uuid);
    }

    public Set<UUID> snapshot() {
        return new HashSet<>(erased);
    }

    public static GMBEraseData load(CompoundTag tag) {
        GMBEraseData data = new GMBEraseData();
        ListTag list = tag.getList("Erased", Tag.TAG_COMPOUND);
        for (int i = 0; i < list.size(); i++) {
            try {
                data.erased.add(list.getCompound(i).getUUID("UUID"));
            } catch (Exception ignored) {}
        }
        return data;
    }

    @Override
    public CompoundTag save(CompoundTag tag) {
        ListTag list = new ListTag();
        for (UUID u : erased) {
            CompoundTag t = new CompoundTag();
            t.putUUID("UUID", u);
            list.add(t);
        }
        tag.put("Erased", list);
        return tag;
    }

    public static GMBEraseData get(MinecraftServer server) {
        return server.overworld()
                .getDataStorage()
                .computeIfAbsent(GMBEraseData::load, GMBEraseData::new, DATA_NAME);
    }
}