package com.tyami.forlaism.entity;

/**
 * ノクスラティアの技ID。
 *
 * 月光系（剣/ノクスルクス）と氷系（鎌/フラスラティア）に分類。
 */
public enum NoxslratiaSkill {

    // ===== 月光系 =====
    CRESCENT_SLASH("三日月斬り", Element.MOONLIGHT, 30, 0.05F),
    MOONLIGHT_WAVE("月光波", Element.MOONLIGHT, 40, 0.10F),
    FULL_MOON_FLASH("満月の一閃", Element.MOONLIGHT, 50, 0.08F),
    MOON_ECLIPSE("月蝕", Element.MOONLIGHT, 60, 0.06F),
    MOON_SHADOW_STEP("月影歩法", Element.MOONLIGHT, 20, 0.15F),

    // ===== 氷系 =====
    ICE_FLOWER_BURIAL("氷華葬", Element.ICE, 40, 0.08F),
    ABSOLUTE_ZERO("絶対零度", Element.ICE, 60, 0.05F),
    ICICLE_HELL("氷柱地獄", Element.ICE, 50, 0.07F),
    FREEZING_SCYTHE("凍結の鎌", Element.ICE, 30, 0.05F),
    WHITE_NIGHT("白夜", Element.HYBRID, 80, 0.04F);

    public enum Element {
        MOONLIGHT,
        ICE,
        HYBRID
    }

    private final String displayName;
    private final Element element;
    private final int cooldownTicks;
    private final float weight;

    NoxslratiaSkill(String displayName, Element element, int cooldownTicks, float weight) {
        this.displayName = displayName;
        this.element = element;
        this.cooldownTicks = cooldownTicks;
        this.weight = weight;
    }

    public String getDisplayName() { return displayName; }
    public Element getElement() { return element; }
    public int getCooldownTicks() { return cooldownTicks; }
    public float getWeight() { return weight; }
}