package com.tyami.forlaism.world;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;

/**
 * ナイトメアモードのワールド単位フラグ。
 *
 * ワールド作成時にONにすると、以降そのワールドは
 * ナイトメアモードとして扱われる。
 *
 * ワールドに保存されるので再起動しても維持。
 */
public class NightmareSavedData extends SavedData {

    private static final String DATA_NAME = "forlaism_nightmare";

    /** ナイトメアモード中か。 */
    private boolean nightmare = false;

    public NightmareSavedData() {
    }

    // =========================================================
    // API
    // =========================================================

    public boolean isNightmare() {
        return nightmare;
    }

    public void setNightmare(boolean value) {
        if (this.nightmare != value) {
            this.nightmare = value;
            setDirty();
        }
    }

    /**
     * サーバーからナイトメア判定するショートハンド。
     */
    public static boolean isNightmare(MinecraftServer server) {
        if (server == null) return false;
        return get(server).isNightmare();
    }

    // =========================================================
    // SavedData
    // =========================================================

    public static NightmareSavedData load(CompoundTag tag) {
        NightmareSavedData data = new NightmareSavedData();
        data.nightmare = tag.getBoolean("Nightmare");
        return data;
    }

    @Override
    public CompoundTag save(CompoundTag tag) {
        tag.putBoolean("Nightmare", nightmare);
        return tag;
    }

    public static NightmareSavedData get(MinecraftServer server) {
        return server.overworld()
                .getDataStorage()
                .computeIfAbsent(
                        NightmareSavedData::load,
                        NightmareSavedData::new,
                        DATA_NAME
                );
    }
}