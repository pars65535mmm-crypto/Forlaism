package com.tyami.forlaism.lang.effect;

/**
 * 対象選択の種類。
 *
 * 「the nearest zombie」→ NEAREST + ZOMBIE
 * 「all zombies」→ ALL + ZOMBIE
 * 「a random zombie」→ RANDOM + ZOMBIE
 * 「the self」→ SELF
 */
public enum TargetSelector {
    /** the nearest X : 最も近い1体。 */
    NEAREST,

    /** all X : 全員。 */
    ALL,

    /** a random X : ランダムに1体。 */
    RANDOM,

    /** the self : 自分自身。 */
    SELF
}