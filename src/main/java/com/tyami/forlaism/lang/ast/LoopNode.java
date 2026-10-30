package com.tyami.forlaism.lang.ast;

/**
 * ループ。
 *
 * 例:
 *   Loop for 100 ticks Every 10 ticks, heal the self with light.
 *     → durationTicks=100, intervalTicks=10, body=CommandNode(...)
 *
 *   Loop, heal the self with light.
 *     → durationTicks=null（永久）, intervalTicks=1（毎tick）
 *
 *   Loop Every 20 ticks, heal the self.
 *     → durationTicks=null, intervalTicks=20
 *
 * durationTicks が null なら永久ループ。
 * intervalTicks は最低1（0以下なら1に補正）。
 */
public record LoopNode(
        Integer durationTicks,
        Integer intervalTicks,
        Node body
) implements Node {

    public int interval() {
        return intervalTicks == null ? 1 : Math.max(1, intervalTicks);
    }

    public boolean isPermanent() {
        return durationTicks == null;
    }

    public int duration() {
        return durationTicks == null ? -1 : Math.max(1, durationTicks);
    }
}