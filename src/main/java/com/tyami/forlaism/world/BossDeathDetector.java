package com.tyami.forlaism.world;

import net.minecraftforge.fml.ModList;
import net.minecraftforge.forgespi.language.IModInfo;

import java.util.UUID;

/**
 * 即死の種類を判定して BossInstance に記録する。
 *
 * 呼び元スタックトレースからMOD名を特定する。
 */
public final class BossDeathDetector {

    private BossDeathDetector() {
    }

    /**
     * 即死を検出して BossInstance に記録する。
     */
    public static void detect(
            UUID bossId,
            BossDeathRecord.Type type,
            String detail
    ) {
        BossInstance inst = BossManager.getById(bossId);
        if (inst == null) return;

        String source = findCallerMod();

        if (inst.deathRecord == null) {
            inst.deathRecord = new BossDeathRecord();
        }
        inst.deathRecord.record(type, source, detail);
    }

    /**
     * 呼び元スタックトレースからMOD名を特定する。
     *
     * forlaism 自身と minecraft / forge 本体はスキップ。
     */
    public static String findCallerMod() {
        StackTraceElement[] stack = Thread.currentThread().getStackTrace();

        for (StackTraceElement e : stack) {
            String cn = e.getClassName();

            // 自分自身・Minecraft本体・Forge本体は除外
            if (cn.startsWith("com.tyami.forlaism")) continue;
            if (cn.startsWith("net.minecraft")) continue;
            if (cn.startsWith("net.minecraftforge")) continue;
            if (cn.startsWith("java.")) continue;
            if (cn.startsWith("sun.")) continue;
            if (cn.startsWith("jdk.")) continue;

            // MODのパッケージ名を推定
            // 例: com.example.cheatmod.SomeClass → com.example.cheatmod
            String modPackage = extractModPackage(cn);

            // ModList から一致するMODを探す
            String modName = lookupModByPackage(modPackage);
            if (modName != null) {
                return modName;
            }

            // 見つからなければパッケージ名を返す
            return modPackage;
        }

        return "不明";
    }

    /**
     * クラス名からMODっぽいパッケージ名を抽出する。
     *
     * com.example.cheatmod.SomeClass → com.example.cheatmod
     * jp.tyami.somemod.Weapon     → jp.tyami.somemod
     */
    private static String extractModPackage(String className) {
        String[] parts = className.split("\\.");

        // 最低3階層あればその3階層目までをMODパッケージと推定
        // 例: com.example.cheatmod.SomeClass → com.example.cheatmod
        if (parts.length >= 4) {
            return parts[0] + "." + parts[1] + "." + parts[2];
        } else if (parts.length >= 3) {
            return parts[0] + "." + parts[1];
        } else if (parts.length >= 2) {
            return parts[0];
        }
        return className;
    }

    /**
     * ModList からパッケージに一致するMOD名を探す。
     */
    private static String lookupModByPackage(String packageName) {
        try {
            for (IModInfo info : ModList.get().getMods()) {
                String modId = info.getModId();
                String modName = info.getDisplayName();

                // MOD ID がパッケージ名に含まれているか
                if (packageName.toLowerCase().contains(modId.toLowerCase())) {
                    return modName != null ? modName : modId;
                }

                // パッケージ名がMOD IDに含まれているか
                if (modId.toLowerCase().contains(packageName.toLowerCase())) {
                    return modName != null ? modName : modId;
                }

                // パッケージ名の最後の要素とMOD IDを比較
                String[] parts = packageName.split("\\.");
                String last = parts[parts.length - 1];
                if (last.equalsIgnoreCase(modId)) {
                    return modName != null ? modName : modId;
                }
            }
        } catch (Throwable ignored) {
        }

        return null;
    }
}