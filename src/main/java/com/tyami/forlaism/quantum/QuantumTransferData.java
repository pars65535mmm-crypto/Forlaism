package com.tyami.forlaism.quantum;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.Map;

/**
 * 量子転送ネットワークのワールド保存用 SavedData。
 *
 * サーバー全体で1つ。全ディメンション共有。
 */
public class QuantumTransferData extends SavedData {

    private static final String DATA_NAME = "forlaism_quantum_transfer";

    private final Map<Integer, QuantumPool> pools = new java.util.concurrent.ConcurrentHashMap<>();

    public QuantumTransferData() {
    }

    public static QuantumTransferData load(CompoundTag tag) {
        QuantumTransferData data = new QuantumTransferData();
        CompoundTag poolsTag = tag.getCompound("Pools");
        for (String key : poolsTag.getAllKeys()) {
            try {
                int network = Integer.parseInt(key);
                QuantumPool pool = new QuantumPool();
                pool.load(poolsTag.getCompound(key));
                data.pools.put(network, pool);
            } catch (NumberFormatException ignored) {
            }
        }
        return data;
    }

    @Override
    public CompoundTag save(CompoundTag tag) {
        CompoundTag poolsTag = new CompoundTag();
        for (Map.Entry<Integer, QuantumPool> e : pools.entrySet()) {
            poolsTag.put(String.valueOf(e.getKey()), e.getValue().save());
        }
        tag.put("Pools", poolsTag);
        return tag;
    }

    public QuantumPool getPool(int network) {
        int n = QuantumTransferNetwork.clamp(network);
        QuantumPool pool = pools.computeIfAbsent(n, k -> new QuantumPool());
        // 新しいプールを作ったら保存フラグを立てる
        setDirty();
        return pool;
    }

    /** サーバーから取得するヘルパー。 */
    public static QuantumTransferData get(MinecraftServer server) {
        return server.overworld()
                .getDataStorage()
                .computeIfAbsent(
                        QuantumTransferData::load,
                        QuantumTransferData::new,
                        DATA_NAME
                );
    }
}