package com.tyami.forlaism.client;

import com.tyami.forlaism.network.BossSyncPacket;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import java.util.ArrayList;
import java.util.List;

/**
 * クライアント側で受信したボスリストを保持。
 */
@OnlyIn(Dist.CLIENT)
public final class ClientBossCache {

    /** 受信したボスリスト。 */
    public static volatile List<BossSyncPacket.Entry> entries = new ArrayList<>();

    private ClientBossCache() {
    }

    public static void update(List<BossSyncPacket.Entry> newEntries) {
        entries = new ArrayList<>(newEntries);
    }

    public static List<BossSyncPacket.Entry> snapshot() {
        return entries;
    }
}