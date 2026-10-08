package com.tyami.forlaism.world;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;

/**
 * Dreamディメンションの崩壊状態を管理する。
 *
 * 「崩壊済み」フラグが立つと、二度とDreamに入れなくなる。
 * ワールドデータに保存されるので、再起動しても維持。
 */
public class DreamCollapseData extends SavedData {

    private static final String DATA_NAME = "forlaism_dream_collapse";

    /** 崩壊済みフラグ。 */
    private boolean collapsed = false;

    public DreamCollapseData() {
    }

    // =========================================================
    // API
    // =========================================================

    public boolean isCollapsed() {
        return collapsed;
    }

    public void markCollapsed() {
        if (!collapsed) {
            collapsed = true;
            setDirty();
        }
    }

    public void reset() {
        collapsed = false;
        setDirty();
    }

    // =========================================================
    // SavedData
    // =========================================================

    public static DreamCollapseData load(CompoundTag tag) {
        DreamCollapseData data = new DreamCollapseData();
        data.collapsed = tag.getBoolean("Collapsed");
        return data;
    }

    @Override
    public CompoundTag save(CompoundTag tag) {
        tag.putBoolean("Collapsed", collapsed);
        return tag;
    }

    public static DreamCollapseData get(MinecraftServer server) {
        return server.overworld()
                .getDataStorage()
                .computeIfAbsent(
                        DreamCollapseData::load,
                        DreamCollapseData::new,
                        DATA_NAME
                );
    }
}