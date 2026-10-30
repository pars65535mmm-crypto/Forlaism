package com.tyami.forlaism.world;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;

/**
 * 全世界時間停止の状態を管理・永続化するSavedData。
 * ワールドリログやサーバー再起動を行っても凍結フラグが維持される。
 */
public class TimeFreezeSavedData extends SavedData {

    private static final String DATA_NAME = "forlaism_time_freeze";

    private boolean frozen = false;

    public TimeFreezeSavedData() {
    }

    public boolean isFrozen() {
        return frozen;
    }

    public void setFrozen(boolean frozen) {
        if (this.frozen != frozen) {
            this.frozen = frozen;
            setDirty();
        }
    }

    public static TimeFreezeSavedData load(CompoundTag tag) {
        TimeFreezeSavedData data = new TimeFreezeSavedData();
        data.frozen = tag.getBoolean("Frozen");
        return data;
    }

    @Override
    public CompoundTag save(CompoundTag tag) {
        tag.putBoolean("Frozen", frozen);
        return tag;
    }

    public static TimeFreezeSavedData get(MinecraftServer server) {
        return server.overworld()
                .getDataStorage()
                .computeIfAbsent(
                        TimeFreezeSavedData::load,
                        TimeFreezeSavedData::new,
                        DATA_NAME
                );
    }
}
