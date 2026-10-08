package com.tyami.forlaism.world;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtIo;

import java.io.File;
import java.io.IOException;

/**
 * 「可能性」の入手フラグをグローバルに保存する。
 *
 * config/forlaism_possibility.dat に保存。
 * 全ワールド共通で参照される。
 */
public final class PossibilityGlobalData {

    private static final String FILE_NAME = "forlaism_possibility.dat";
    private static final String TAG_PENDING = "PossibilityPending";

    /** フラグが立っているか（= 次ワールドでドロップすべきか）。 */
    private static boolean pending = false;

    /** 一度ロードしたか。 */
    private static boolean loaded = false;

    private PossibilityGlobalData() {
    }

    // =========================================================
    // ファイルパス
    // =========================================================

    private static File getFile() {
        // Forge の config ディレクトリ
        File configDir = net.minecraftforge.fml.loading.FMLPaths.CONFIGDIR.get().toFile();
        if (!configDir.exists()) {
            configDir.mkdirs();
        }
        return new File(configDir, FILE_NAME);
    }

    // =========================================================
    // ロード / セーブ
    // =========================================================

    private static synchronized void ensureLoaded() {
        if (loaded) return;
        loaded = true;

        File file = getFile();
        if (!file.exists()) {
            pending = false;
            return;
        }

        try {
            CompoundTag tag = NbtIo.readCompressed(file);
            pending = tag.getBoolean(TAG_PENDING);
        } catch (IOException e) {
            System.err.println("[Forlaism] Failed to load possibility data: " + e);
            pending = false;
        }
    }

    private static synchronized void save() {
        File file = getFile();
        CompoundTag tag = new CompoundTag();
        tag.putBoolean(TAG_PENDING, pending);

        try {
            NbtIo.writeCompressed(tag, file);
        } catch (IOException e) {
            System.err.println("[Forlaism] Failed to save possibility data: " + e);
        }
    }

    // =========================================================
    // API
    // =========================================================

    /** フラグが立っているか。 */
    public static boolean isPending() {
        ensureLoaded();
        return pending;
    }

    /** フラグを立てる（死亡検知時）。 */
    public static void markPending() {
        ensureLoaded();
        pending = true;
        save();
    }

    /** フラグを消費する（ドロップ時）。 */
    public static void consume() {
        ensureLoaded();
        pending = false;
        save();
    }
}