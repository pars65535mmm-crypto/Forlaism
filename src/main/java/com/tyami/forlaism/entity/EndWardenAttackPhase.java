package com.tyami.forlaism.entity;

/**
 * EndWarden の行動フェーズ。
 *
 * 通常個体とリバース個体で共有する。
 * リバースは ULTIMATE フェーズを使う。
 */
public enum EndWardenAttackPhase {

    /** 待機。 */
    IDLE(0),

    /** 歩行・徘徊。 */
    WALK(0),

    /** 近接攻撃。 */
    MELEE(20),

    /** ソニックブーム（羽バサ + ビーム）。 */
    SONIC_BOOM(60),

    /** 突進。 */
    DIVE(40),

    /** リバース専用：奥義（メテオ + レーザー乱射 + テレポ）。 */
    ULTIMATE(200),

    /** 死亡演出。 */
    DEATH(0);

    private final int baseDurationTicks;

    EndWardenAttackPhase(int baseDurationTicks) {
        this.baseDurationTicks = baseDurationTicks;
    }

    public int getBaseDurationTicks() {
        return baseDurationTicks;
    }

    /** アニメーション名（Blockbench定義名に合わせる）。 */
    public String getAnimationName() {
        return switch (this) {
            case IDLE -> "iddel";
            case WALK -> "warkaa";
            case MELEE -> "atttack";
            case SONIC_BOOM -> "beeeeem";
            case DIVE -> "warkaa";
            case ULTIMATE -> "beeeeem";
            case DEATH -> "iddel";
        };
    }

    public boolean isAttacking() {
        return this == MELEE || this == SONIC_BOOM || this == DIVE || this == ULTIMATE;
    }
}