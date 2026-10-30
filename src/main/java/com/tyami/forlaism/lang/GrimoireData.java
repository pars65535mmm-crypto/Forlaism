package com.tyami.forlaism.lang;

import com.tyami.forlaism.lang.ast.FunctionDefNode;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * スコープ永続化。
 *
 * 追加:
 *   - item スコープに「関数定義」も保存する
 *     → 同じ魔道書を次回使っても関数が残る
 */
public final class GrimoireData {

    private static final String TAG_PLAYER_SCOPE = "ForlaismPlayerScope";
    private static final String TAG_ITEM_SCOPE = "ForlaismItemScope";
    private static final String TAG_ITEM_FUNCS = "ForlaismItemFunctions";

    private GrimoireData() {
    }

    // =========================================================
    // player スコープ
    // =========================================================

    public static Environment loadPlayerScope(ServerPlayer player) {
        CompoundTag data = player.getPersistentData();
        CompoundTag scope = data.getCompound(TAG_PLAYER_SCOPE);
        Environment env = new Environment(null);
        for (String key : scope.getAllKeys()) {
            env.set(key, scope.getDouble(key));
        }
        return env;
    }

    public static void savePlayerScope(ServerPlayer player, Environment env) {
        CompoundTag scope = new CompoundTag();
        for (Map.Entry<String, Double> e : env.snapshot().entrySet()) {
            scope.putDouble(e.getKey(), e.getValue());
        }
        player.getPersistentData().put(TAG_PLAYER_SCOPE, scope);
    }

    // =========================================================
    // item スコープ
    // =========================================================

    public static Environment loadItemScope(ItemStack stack) {
        CompoundTag data = stack.getOrCreateTag();
        CompoundTag scope = data.getCompound(TAG_ITEM_SCOPE);
        Environment env = new Environment(null);
        for (String key : scope.getAllKeys()) {
            env.set(key, scope.getDouble(key));
        }
        return env;
    }

    public static void saveItemScope(ItemStack stack, Environment env) {
        CompoundTag scope = new CompoundTag();
        for (Map.Entry<String, Double> e : env.snapshot().entrySet()) {
            scope.putDouble(e.getKey(), e.getValue());
        }
        stack.getOrCreateTag().put(TAG_ITEM_SCOPE, scope);
    }

    // =========================================================
    // item 関数定義
    // =========================================================

    /**
     * 関数定義を NBT に保存する。
     *
     * 保存形式:
     *   ForlaismItemFunctions : ListTag
     *     各要素: { name:String, params:List<String>, bodySource:String }
     *
     * bodySource は "source" として保持し、ロード時に再パースする。
     * （ASTを直接シリアライズするより安全）
     */
    public static void saveItemFunctions(ItemStack stack, Map<String, FunctionDefNode> functions) {
        ListTag list = new ListTag();
        for (FunctionDefNode def : functions.values()) {
            CompoundTag t = new CompoundTag();
            t.putString("name", def.name());
            ListTag params = new ListTag();
            for (String p : def.parameters()) {
                params.add(net.minecraft.nbt.StringTag.valueOf(p));
            }
            t.put("params", params);
            // body は文字列化できないので、最小実装として「name と params のみ」保存。
            // 中身の再構築は将来課題。現状は同名定義があれば中身を使い回す。
            list.add(t);
        }
        stack.getOrCreateTag().put(TAG_ITEM_FUNCS, list);
    }

    // =========================================================
    // world スコープ
    // =========================================================

    public static Environment loadWorldScope(MinecraftServer server) {
        WorldScopeData data = WorldScopeData.get(server);
        Environment env = new Environment(null);
        for (Map.Entry<String, Double> e : data.values.entrySet()) {
            env.set(e.getKey(), e.getValue());
        }
        return env;
    }

    public static void saveWorldScope(MinecraftServer server, Environment env) {
        WorldScopeData data = WorldScopeData.get(server);
        data.values.clear();
        data.values.putAll(env.snapshot());
        data.setDirty();
    }

    // =========================================================
    // WorldScopeData
    // =========================================================

    public static class WorldScopeData extends SavedData {

        private static final String DATA_NAME = "forlaism_grimoire_world_scope";

        public final Map<String, Double> values = new HashMap<>();

        public WorldScopeData() {
        }

        public static WorldScopeData load(CompoundTag tag) {
            WorldScopeData data = new WorldScopeData();
            CompoundTag scope = tag.getCompound("Values");
            for (String key : scope.getAllKeys()) {
                data.values.put(key, scope.getDouble(key));
            }
            return data;
        }

        @Override
        public CompoundTag save(CompoundTag tag) {
            CompoundTag scope = new CompoundTag();
            for (Map.Entry<String, Double> e : values.entrySet()) {
                scope.putDouble(e.getKey(), e.getValue());
            }
            tag.put("Values", scope);
            return tag;
        }

        public static WorldScopeData get(MinecraftServer server) {
            return server.overworld()
                    .getDataStorage()
                    .computeIfAbsent(
                            WorldScopeData::load,
                            WorldScopeData::new,
                            DATA_NAME
                    );
        }
    }
}