package com.tyami.forlaism.entity;

/**
 * ノクスラティアの形態。
 *
 * 後から第2形態・第3形態などを追加できるようにする。
 *
 * 各形態は「HP閾値」と「使用可能フェーズ」を持ち、
 * NoxslratiaEntity が HP に応じて自動で切り替える。
 */
public enum NoxslratiaForm {

    /** 第一形態：通常。 */
    FORM_1(1.0F),

    /** 第二形態：HP 50% 以下で移行（予定地）。 */
    FORM_2(0.5F),

    /** 第三形態：HP 20% 以下で移行（予定地）。 */
    FORM_3(0.2F);

    /** この形態に入る HP 割合の閾値。 */
    private final float hpThreshold;

    NoxslratiaForm(float hpThreshold) {
        this.hpThreshold = hpThreshold;
    }

    public float getHpThreshold() {
        return hpThreshold;
    }

    /**
     * 現在の HP 割合から、その時点で最も低い閾値を満たす形態を返す。
     */
    public static NoxslratiaForm fromHpRatio(float hpRatio) {
        NoxslratiaForm result = FORM_1;
        for (NoxslratiaForm form : values()) {
            if (hpRatio <= form.hpThreshold) {
                result = form;
            }
        }
        return result;
    }
}