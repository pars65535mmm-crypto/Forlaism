package com.tyami.forlaism.entity;

/**
 * ノクスラティアの行動フェーズ。
 *
 * 技を撃つフェーズは CAST_SKILL に集約し、
 * 実際にどの技を撃つかは NoxslratiaEntity#currentSkill で持つ。
 */
public enum NoxslratiaAttackPhase {

    IDLE(0, "idle"),
    WALK(0, "walk"),
    SCYTHE_ATTACK(30, "attack1"),
    SWORD_ATTACK(30, "attack2"),
    SWORD_ATTACK_3(20, "attack3"),
    TELEPORT(15, "tp"),
    SPAWN(33, "spawn(botu)"),
    DEATH(40, "idle"),

    /** 技を撃つフェーズ（攻撃アニメを流用）。 */
    CAST_SKILL(50, "attack2");

    private final int baseDurationTicks;
    private final String animationName;

    NoxslratiaAttackPhase(int baseDurationTicks, String animationName) {
        this.baseDurationTicks = baseDurationTicks;
        this.animationName = animationName;
    }

    public int getBaseDurationTicks() { return baseDurationTicks; }
    public String getAnimationName() { return animationName; }

    public boolean isAttacking() {
        return this == SCYTHE_ATTACK
                || this == SWORD_ATTACK
                || this == SWORD_ATTACK_3
                || this == CAST_SKILL;
    }
}