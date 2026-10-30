package com.tyami.forlaism.lang;

import com.tyami.forlaism.lang.ast.LoopNode;
import com.tyami.forlaism.lang.ast.Node;

import net.minecraft.server.level.ServerLevel;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;

/**
 * ループ呪文のスケジューラ。
 *
 * LoopNode を「tick ごとに1回実行」する。
 *
 * 各エントリは:
 *   - 対象のプレイヤーUUID
 *   - 実行する文（body）
 *   - 残り tick 数（durationTicks が null なら永続）
 *   - 実行間隔（intervalTicks）
 *   - 経過 tick カウンタ
 *
 * ループはプレイヤーに紐づく。
 * プレイヤーがログアウトすると止まる。
 */
@Mod.EventBusSubscriber(modid = "forlaism")
public final class SpellScheduler {

    private static final List<Entry> ACTIVE = new ArrayList<>();

    private SpellScheduler() {
    }

    /** ループを開始する。 */
    public static void schedule(
            UUID playerId,
            Node body,
            Integer durationTicks,
            Integer intervalTicks
    ) {
        ACTIVE.add(new Entry(
                playerId,
                body,
                durationTicks == null ? -1 : durationTicks,
                intervalTicks == null ? 1 : Math.max(1, intervalTicks),
                0
        ));
    }

    /** プレイヤーのループをすべて停止する。 */
    public static void stopAll(UUID playerId) {
        ACTIVE.removeIf(e -> e.playerId.equals(playerId));
    }

    /** プレイヤーのループ数を返す。 */
    public static int count(UUID playerId) {
        int c = 0;
        for (Entry e : ACTIVE) {
            if (e.playerId.equals(playerId)) c++;
        }
        return c;
    }

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        if (ACTIVE.isEmpty()) return;

        var server = event.getServer();
        if (server == null) return;

        Iterator<Entry> it = ACTIVE.iterator();

        while (it.hasNext()) {
            Entry e = it.next();

            var player = server.getPlayerList().getPlayer(e.playerId);
            if (player == null || !player.isAlive()) {
                it.remove();
                continue;
            }

            // 残りtick
            if (e.remainingTicks > 0) {
                e.remainingTicks--;
            }

            // 実行間隔チェック
            if (e.elapsed % e.intervalTicks == 0) {
                try {
                    SpellInterpreterHolder.interpret(
                            e.body,
                            new SpellContext(
                                    player,
                                    player.serverLevel(),
                                    player.getMainHandItem()
                            )
                    );
                } catch (Throwable t) {
                    // エラーで止めない
                }
            }
            e.elapsed++;

            // 期間終了
            if (e.remainingTicks == 0) {
                it.remove();
            }
        }
    }

    /** 内部エントリ。 */
    private static final class Entry {
        final UUID playerId;
        final Node body;
        int remainingTicks;      // -1 なら永続
        final int intervalTicks;
        int elapsed;

        Entry(UUID playerId, Node body, int remainingTicks, int intervalTicks, int elapsed) {
            this.playerId = playerId;
            this.body = body;
            this.remainingTicks = remainingTicks;
            this.intervalTicks = intervalTicks;
            this.elapsed = elapsed;
        }
    }
}