package com.tyami.forlaism.item;

import com.tyami.forlaism.Forlaism;

import net.minecraft.server.level.ServerLevel;

import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * マスターピースクロック専用の遅延実行スケジューラ。
 *
 * ServerLevel の tick に合わせて、指定 tick 後に Runnable を実行する。
 *
 * 【重要】
 *   runnable の中で schedule() が呼ばれるケース（演出中に次の演出を予約）
 *   があるため、リストを直接イテレートせず、
 *   「tick終了予定のタスクを一時リストに集めてから実行」する。
 */
@Mod.EventBusSubscriber(modid = Forlaism.MOD_ID)
public final class MasterpieceClockScheduler {

    private static final List<Task> TASKS = new ArrayList<>();

    private MasterpieceClockScheduler() {
    }

    public static void schedule(ServerLevel level, int delayTicks, Runnable runnable) {
        synchronized (TASKS) {
            TASKS.add(new Task(level, delayTicks, runnable));
        }
    }

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;

        // =========================================================
        // 1. 残り tick を減らしつつ、実行対象をリストに集める
        // =========================================================
        List<Task> toRun = new ArrayList<>();

        synchronized (TASKS) {
            Iterator<Task> it = TASKS.iterator();
            while (it.hasNext()) {
                Task t = it.next();
                t.remaining--;

                if (t.remaining <= 0) {
                    toRun.add(t);
                    it.remove();
                }
            }
        }

        // =========================================================
        // 2. 実行対象を実行（この中で schedule() が呼ばれても安全）
        // =========================================================
        for (Task t : toRun) {
            try {
                t.runnable.run();
            } catch (Throwable ignored) {
            }
        }
    }

    private static final class Task {
        final ServerLevel level;
        int remaining;
        final Runnable runnable;

        Task(ServerLevel level, int delay, Runnable runnable) {
            this.level = level;
            this.remaining = delay;
            this.runnable = runnable;
        }
    }
}