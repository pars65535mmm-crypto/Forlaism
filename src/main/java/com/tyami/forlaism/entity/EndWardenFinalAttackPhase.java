package com.tyami.forlaism.entity;

/**
 * エンディストウォーデンの行動フェーズ。
 *
 * 通常攻撃 + 第二形態（TP攻撃・反射レーザー） + 第三形態（属性乱射）
 */
public enum EndWardenFinalAttackPhase {

    IDLE(0),
    WALK(0),
    MELEE(20),
    SONIC_BOOM(60),
    DIVE(40),
    ULTIMATE(200),

    /** 第二形態: TP移動攻撃（溜め→ビーム→背後TP→輪廻突進） */
    TP_STRIKE(60),

    /** 第三形態: 属性乱射（火/氷/雷/隕石/TNT） */
    ELEMENTAL_BARRAGE(200),

    DEATH(0);

    private final int baseDurationTicks;

    EndWardenFinalAttackPhase(int baseDurationTicks) {
        this.baseDurationTicks = baseDurationTicks;
    }

    public int getBaseDurationTicks() {
        return baseDurationTicks;
    }

    public String getAnimationName() {
        return switch (this) {
            case IDLE, DEATH -> "iddel";
            case WALK, DIVE -> "warkaa";
            case MELEE -> "atttack";
            case SONIC_BOOM, ULTIMATE, TP_STRIKE, ELEMENTAL_BARRAGE -> "beeeeem";
        };
    }

    public boolean isAttacking() {
        return this == MELEE || this == SONIC_BOOM || this == DIVE
                || this == ULTIMATE || this == TP_STRIKE || this == ELEMENTAL_BARRAGE;
    }
}